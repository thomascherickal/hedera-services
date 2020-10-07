package com.hedera.services.stream;

import com.hedera.services.context.properties.PropertySource;
import com.hedera.services.legacy.services.stats.HederaNodeStats;
import com.hedera.services.utils.EntityIdUtils;
import com.hederahashgraph.api.proto.java.AccountID;
import com.swirlds.common.Platform;
import com.swirlds.common.crypto.Hash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static com.hedera.services.legacy.logic.ApplicationConstants.RECORD_LOG_DIR;
import static com.hedera.services.stream.RunningHashCalculator.RECORD_LOG_DIR_PROP_NAME;
import static com.hedera.services.stream.RunningHashCalculator.RECORD_LOG_PERIOD_PROP_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RunningHashCalculatorTest {

	private Platform platform;
	private PropertySource propertySource;
	private Hash initialHash;
	private HederaNodeStats stats;
	private Supplier<RunningHashLeaf> runningHashLeafSupplier;
	private AccountID nodeAccountID;
	private static final String recordLogDir = "data/recordstreams";

	@BeforeEach
	void setup() {
		platform = mock(Platform.class);
		propertySource = mock(PropertySource.class);
		initialHash = mock(Hash.class);
		stats = mock(HederaNodeStats.class);
		runningHashLeafSupplier = () -> mock(RunningHashLeaf.class);
		nodeAccountID = AccountID.newBuilder().setAccountNum(3).build();

		when(propertySource.getLongProperty(RECORD_LOG_PERIOD_PROP_NAME)).thenReturn(2L);
		when(propertySource.getStringProperty(RECORD_LOG_DIR_PROP_NAME)).thenReturn(RECORD_LOG_DIR);
	}

	@Test
	public void setStreamDirTest() {
		RunningHashCalculator runningHashCalculator = new RunningHashCalculator(platform, propertySource, initialHash,
				runningHashLeafSupplier, stats, nodeAccountID);
		assertEquals(recordLogDir + "/record" + EntityIdUtils.asLiteralString(nodeAccountID),
				runningHashCalculator.getStreamDir());
	}
}
