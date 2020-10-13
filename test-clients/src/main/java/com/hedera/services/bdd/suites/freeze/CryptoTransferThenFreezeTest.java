package com.hedera.services.bdd.suites.freeze;

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.utilops.UtilVerbs;
import com.hedera.services.bdd.suites.perf.CryptoTransferLoadTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.freeze;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.sleepFor;

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
		return List.of(
				new HapiApiSpec[] { runCryptoTransfers(), freezeAfterTransfers() });
	}

	private HapiApiSpec freezeAfterTransfers() {
		return defaultHapiSpec("FreezeAfterTransfers").given().when(
				freeze().startingIn(1).minutes().andLasting(1).minutes().payingWith(GENESIS)
		).then(
				// sleep for a while to wait for this freeze transaction be handled
				UtilVerbs.sleepFor(75_000)
		);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
