package com.hedera.services.bdd.suites.freeze;

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.suites.perf.CryptoTransferLoadTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.freeze;

public class CryptoTransferLoadThenFreezeTest extends CryptoTransferLoadTest {
	private static final Logger log = LogManager.getLogger(CryptoTransferLoadThenFreezeTest.class);

	public static void main(String... args) {
		parseArgs(args);

		CryptoTransferLoadThenFreezeTest suite = new CryptoTransferLoadThenFreezeTest();
		suite.setReportStats(true);
		suite.runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(runCryptoTransfers(), freezeAfterTransfers());
	}

	private HapiApiSpec freezeAfterTransfers() {
		log.info("Is about to send freeze transaction");
		return defaultHapiSpec("FreezeAfterTransfers").given().when(
		).then(
				freeze().startingIn(60).seconds().andLasting(1).minutes()
		);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
