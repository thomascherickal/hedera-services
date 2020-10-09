package com.hedera.services.bdd.suites.freeze;

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.suites.perf.CryptoTransferLoadTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.freeze;

public class CryptoTransferThenFreezeTest extends CryptoTransferLoadTest {
	private static final Logger log = LogManager.getLogger(CryptoTransferThenFreezeTest.class);

	public static void main(String... args) {
		parseArgs(args);

		CryptoTransferThenFreezeTest suite = new CryptoTransferThenFreezeTest();
		suite.setReportStats(true);
		suite.runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(freezeAfterTransfers(), runCryptoTransfers() );
	}

	private HapiApiSpec freezeAfterTransfers() {
		log.info("Is about to send freeze transaction");
		return defaultHapiSpec("FreezeAfterTransfers").given().when(
		).then(
				freeze().startingIn(testDurationMinutes.getAsInt() - 1).minutes().andLasting(1).minutes()
		);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
