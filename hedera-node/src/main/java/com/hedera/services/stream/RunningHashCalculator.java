package com.hedera.services.stream;

import com.hedera.services.context.properties.PropertySource;
import com.hedera.services.legacy.config.PropertiesLoader;
import com.hedera.services.legacy.services.stats.HederaNodeStats;
import com.hedera.services.utils.EntityIdUtils;
import com.hederahashgraph.api.proto.java.AccountID;
import com.swirlds.common.Platform;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.stream.ObjectStreamCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static com.swirlds.common.Constants.SEC_TO_MS;
import static com.swirlds.logging.LogMarker.EVENT_STREAM;
import static com.swirlds.logging.LogMarker.OBJECT_STREAM_DETAIL;

/**
 * RunningHashCalculator takes a RecordStreamObject, calculates running Hash,
 * sends this object and runningHash to RecordStreamFileWriter.
 * This is not be a separate thread, because we need to update the runningHash in ServicesState at the end of handleTransaction() for avoiding ISS
 */
public class RunningHashCalculator {
	/** use this for all logging, as controlled by the optional data/log4j2.xml file */
	private static final Logger log = LogManager.getLogger();
	/** calculates RunningHash for RecordStreamObjects */
	private ObjectStreamCreator<RecordStreamObject> objectStreamCreator;
	/** serializes RecordStreamObjects to record stream files */
	private RecordStreamFileWriter<RecordStreamObject> consumer;
	/** initialHash loaded from state */
	private Hash initialHash;
	/**
	 * when recordStreamObject streaming is started after reconnect, or at state recovering, startWriteAtCompleteWindow should be set
	 * to be true;
	 * when recordStreamObject streaming is started after restart, it should be set to be false
	 */
	private boolean startWriteAtCompleteWindow = false;
	/**
	 * When we freeze the platform, we should close and sign the last record stream file after finish writing all RecordStreamObject in current working queue
	 */
	private boolean inFreeze = false;

	/**
	 * whether calcRunningHash thread is stopped
	 */
	private volatile boolean stopped = false;

	/** the directory to which recordStreamObject stream files are written */
	private String streamDir;

	private Platform platform;

	private long recordLogPeriod;

	private Supplier<RunningHashLeaf> runningHashLeafSupplier;

	private HederaNodeStats stats;

	private String nodeIdStr;

	static final String RECORD_LOG_PERIOD_PROP_NAME = "hedera.recordStream.logPeriod";
	static final String RECORD_LOG_DIR_PROP_NAME = "hedera.recordStream.logDir";

	public RunningHashCalculator(final Platform platform, final PropertySource propertySource,
			final Hash initialHash, final Supplier<RunningHashLeaf> runningHashLeafSupplier,
			final HederaNodeStats stats,
			final AccountID nodeAccountID) {
		this.platform = platform;
		this.recordLogPeriod = propertySource.getLongProperty(RECORD_LOG_PERIOD_PROP_NAME);
		this.initialHash = initialHash;
		log.info("RunningHashCalculator initialHash: ", () -> (initialHash));
		this.runningHashLeafSupplier = runningHashLeafSupplier;
		this.stats = stats;
		this.nodeIdStr = EntityIdUtils.asLiteralString(nodeAccountID);
		setStreamDir(propertySource.getStringProperty(RECORD_LOG_DIR_PROP_NAME));
	}

	/**
	 * @param parentDir parent directory of record stream directory
	 */
	public void setStreamDir(final String parentDir) {
		streamDir = Path.of(parentDir, "record" + nodeIdStr).toString();
		directoryAssurance(streamDir);
	}

	public String getStreamDir() {
		return streamDir;
	}

	/**
	 * initialize ObjectStreamCreator and set initial RunningHash;
	 * initialize and start TimestampStreamFileWriter if recordStreamObject streaming is enabled
	 */
	public void initializeAndStartRecordStream() {
		if (PropertiesLoader.isEnableRecordStreaming()) {
			//initialize and start TimestampStreamFileWriter, set directory and set startWriteAtCompleteWindow;
			consumer = new RecordStreamFileWriter<>(initialHash, streamDir,
					recordLogPeriod * SEC_TO_MS,
					this.platform,
					startWriteAtCompleteWindow,
					PropertiesLoader.getRecordStreamQueueCapacity(),
					stats,
					nodeIdStr);
		}
		objectStreamCreator = new ObjectStreamCreator<>(initialHash, consumer);
	}

	/**
	 * Each time a Record is generated,
	 * calculates the runningHash of RecordStreamObjects,
	 * updates the runningHash in ServicesState,
	 * and sends this recordStreamObject to a consumer which serializes it to file if recordStreamObjectStreaming is enabled
	 */
	public void calcAndUpdateRunningHash(RecordStreamObject recordStreamObject) {
		// update runningHash, send this object and new runningHash to the consumer if recordStreamObjectStreaming is enabled
		Hash runningHash = objectStreamCreator.addObject(recordStreamObject);

		if (log.isDebugEnabled()) {
			log.debug(OBJECT_STREAM_DETAIL.getMarker(),
					"RunningHash after adding recordStreamObject {} : {}",
					() -> recordStreamObject.toShortString(), () -> runningHash);
		}

		// update runningHash in ServicesState
		runningHashLeafSupplier.get().setHash(runningHash);

		if (inFreeze) {
			// if freeze period is started, we close the objectStreamCreator,
			objectStreamCreator.close();
			if (PropertiesLoader.isEnableRecordStreaming()) {
				// if freeze period is started, we send a close notification to the consumer
				// to let the consumer know it should close and sign file after finish writing all workloads
				// in its working queue.
				consumer.close();
			}
		}
	}

	/**
	 * set startWriteAtCompleteWindow:
	 * it should be set to be true after reconnect, or at state recovering;
	 * it should be set to be false at restart
	 *
	 * @param startWriteAtCompleteWindow
	 */
	void setStartWriteAtCompleteWindow(boolean startWriteAtCompleteWindow) {
		this.startWriteAtCompleteWindow = startWriteAtCompleteWindow;
		log.info("RunningHashCalculator::setStartWriteAtCompleteWindow: {}", () -> startWriteAtCompleteWindow);
	}

	/**
	 * set initialHash and isReconnect after loading from signed state
	 *
	 * @param initialHash
	 */
	void setInitialHash(final Hash initialHash) {
		this.initialHash = initialHash;
		log.info(EVENT_STREAM.getMarker(),
				"RunningHashCalculator::setInitialHash: {}", () -> initialHash);
	}

	/** Creates parent if necessary */
	private static void directoryAssurance(String directory) {
		try {
			Files.createDirectories(Paths.get(directory));
		} catch (IOException e) {
			log.error("Record stream dir {} doesn't exist and cannot be created!", directory, e);
			throw new IllegalStateException(e);
		}
	}

	/**
	 * this method is called when the node falls behind,
	 * stops the streamFileWriter.
	 */
	void stopAndClear() {
		if (consumer != null) {
			consumer.stopAndClear();
		}
	}

	/**
	 * set `inFreeze` to be given value
	 * @param inFreeze
	 */
	public void setInFreeze(boolean inFreeze) {
		this.inFreeze = inFreeze;
		log.info("RecordStream inFreeze is set to be {} ", inFreeze);
	}
}

