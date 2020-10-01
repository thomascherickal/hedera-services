package com.hedera.services.stream;

import com.hedera.services.context.properties.PropertySource;
import com.hedera.services.legacy.config.PropertiesLoader;
import com.hedera.services.legacy.services.stats.HederaNodeStats;
import com.swirlds.common.Platform;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.stream.ObjectStreamCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

import static com.swirlds.common.Constants.SEC_TO_MS;
import static com.swirlds.logging.LogMarker.EVENT_STREAM;
import static com.swirlds.logging.LogMarker.OBJECT_STREAM_DETAIL;

public class RunningHashCalculator {
	/** use this for all logging, as controlled by the optional data/log4j2.xml file */
	private static final Logger log = LogManager.getLogger();

	/**
	 * a queue of RecordStreamObject for calculating RunningHash;
	 * A running hash of hashes of all RecordStreamObject have there been throughout all of history, is stored in the SwildsState;
	 * when recordStreaming is enabled, a running hash of hashes of all RecordStreamObject have been written
	 * is saved in the beginning of each new record stream file.
	 */
	private BlockingQueue<RecordStreamObject> forRunningHash;
	/** A thread that calculates RunningHash for RecordStreamObjects */
	private Thread threadCalcRunningHash;
	/** calculates RunningHash for RecordStreamObjects */
	private ObjectStreamCreator<RecordStreamObject> objectStreamCreator;
	/** serializes RecordStreamObjects to recordStreamObject stream files */
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

	private String addressMemo;

	public RunningHashCalculator(final Platform platform, final PropertySource propertySource,
			final Hash initialHash, final Supplier<RunningHashLeaf> runningHashLeafSupplier,
			final HederaNodeStats stats,
			final String addressMemo) {
		this.platform = platform;
		forRunningHash = new ArrayBlockingQueue<>(PropertiesLoader.getRecordStreamQueueCapacity());
		this.recordLogPeriod = propertySource.getLongProperty("hedera.recordStream.logPeriod");
		this.streamDir = propertySource.getStringProperty("hedera.recordStream.logDir");
		this.initialHash = initialHash;
		this.runningHashLeafSupplier = runningHashLeafSupplier;
		this.stats = stats;
		this.addressMemo = addressMemo;
	}

	/**
	 * @param streamDir
	 */
	public void setStreamDir(final String streamDir) {
		directoryAssurance(streamDir);
		this.streamDir = streamDir;
	}

	public String getStreamDir() {
		return streamDir;
	}

	/**
	 * initialize ObjectStreamCreator and set initial RunningHash;
	 * initialize and start TimestampStreamFileWriter if recordStreamObject streaming is enabled
	 */
	public void startCalcRunningHashThread() {
		if (PropertiesLoader.isEnableRecordStreaming()) {
			//initialize and start TimestampStreamFileWriter, set directory and set startWriteAtCompleteWindow;
			consumer = new RecordStreamFileWriter<>(initialHash, streamDir,
					recordLogPeriod * SEC_TO_MS,
					this.platform,
					startWriteAtCompleteWindow,
					PropertiesLoader.getRecordStreamQueueCapacity(),
					stats,
					addressMemo);
		}
		objectStreamCreator = new ObjectStreamCreator<>(initialHash, consumer);
		threadCalcRunningHash = new Thread(this::run);
		threadCalcRunningHash.start();
		threadCalcRunningHash.setName("calc_running_hash_" + addressMemo);
		log.info("{} started. initialHash: {}",
				() -> threadCalcRunningHash.getName(),
				() -> initialHash);
	}

	private void run() {
		while (!stopped) {
			try {
				calcRunningHash();
			} catch (InterruptedException ex) {
				log.info("calcRunningHash interrupted");
			}
		}
	}

	/**
	 * calcRunningHash is repeatedly called by the threadCalRunningHash thread.
	 * Each time, it takes one recordStreamObject from forRunningHash queue
	 * streamCreator updates RunningHash,
	 * and sends this recordStreamObject to consumer which serializes this recordStreamObject to file if recordStreamObjectStreaming is enabled
	 *
	 * @throws InterruptedException
	 */
	private void calcRunningHash() throws InterruptedException {
		// if the node is not frozen, we should take RecordStreamObject from forRunningHash queue;
		// update runningHash and write it to record stream file;
		// if the node is frozen, and forRunningHash queue is not empty, we should consume all elements in forRunningHash queue
		if (!inFreeze || !forRunningHash.isEmpty()) {
			RecordStreamObject recordStreamObject = forRunningHash.take();

			stats.updateRecordStreamQueueSize(getCalcRunningHashQueueSize());

			// update runningHash, send this object and new runningHash to the consumer if recordStreamObjectStreaming is enabled
			Hash runningHash = objectStreamCreator.addObject(recordStreamObject);

			//TODO: change to debug level, if (log.isDebugEnabled()) {
			log.info(OBJECT_STREAM_DETAIL.getMarker(),
					"RunningHash after adding recordStreamObject {} : {}",
					() -> recordStreamObject.toShortString(), () -> runningHash);

			// update runningHash in ServicesState
			runningHashLeafSupplier.get().setHash(runningHash);
		} else {
			// if freeze period is started, and forRunningHash queue is empty, we close the objectStreamCreator,
			objectStreamCreator.close();
			if (PropertiesLoader.isEnableRecordStreaming()) {
				// if freeze period is started, and forRunningHash queue is empty, we send a close notification to the consumer
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
		File dir = new File(directory);
		if (!dir.exists()) dir.mkdirs();
	}

	/**
	 * put RecordStreamObject into forRunningHash queue
	 *
	 * This method is called by {@link com.hedera.services.legacy.services.state.AwareProcessLogic} when a new TransactionRecord has been generated.
	 * If the queue is full, then this will block until it isn't full
	 *
	 * @param recordStreamObject
	 */
	public void forRunningHashPut(final RecordStreamObject recordStreamObject) {
		try {
			// put this consensus recordStreamObject into the queue for calculating running Hash
			// later this recordStreamObject will be put into forCons queue
			forRunningHash.put(recordStreamObject);
			stats.updateRecordStreamQueueSize(getCalcRunningHashQueueSize());
		} catch (InterruptedException e) {
			log.info("forRunningHashPut interrupted");
		}
	}

	/**
	 * this method is called when the node falls behind,
	 * clears the queue, stops threadCalcRunningHash and streamFileWriter.
	 */
	void stopAndClear() {
		forRunningHash.clear();
		stopped = true;
		if (consumer != null) {
			consumer.stopAndClear();
		}
		log.info("threadCalcRunningHash stopped");
	}

	/**
	 * set `inFreeze` to be given value
	 * @param inFreeze
	 */
	public void setInFreeze(boolean inFreeze) {
		this.inFreeze = inFreeze;
		log.info("RecordStream inFreeze is set to be {} ", inFreeze);
	}

	public int getCalcRunningHashQueueSize() {
		if (forRunningHash == null) {
			return 0;
		}
		return forRunningHash.size();
	}
}

