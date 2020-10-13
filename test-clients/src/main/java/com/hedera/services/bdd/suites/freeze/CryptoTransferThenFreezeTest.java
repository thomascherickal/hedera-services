package com.hedera.services.bdd.suites.freeze;

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.spec.utilops.LoadTest;
import com.hedera.services.bdd.spec.utilops.UtilVerbs;
import com.hedera.services.bdd.suites.perf.CryptoTransferLoadTest;
import com.hedera.services.bdd.suites.perf.PerfTestLoadSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Supplier;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.crypto.HapiCryptoTransfer.tinyBarsFromTo;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.freeze;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.logIt;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.sleepFor;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.BUSY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.DUPLICATE_TRANSACTION;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.PLATFORM_TRANSACTION_NOT_CREATED;

public class CryptoTransferThenFreezeTest extends LoadTest {
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

	protected HapiApiSpec runCryptoTransfers() {
		PerfTestLoadSettings settings = new PerfTestLoadSettings();

		Supplier<HapiSpecOperation[]> transferBurst = () -> new HapiSpecOperation[] {
				cryptoTransfer(tinyBarsFromTo("sender", "receiver", 1L))
						.noLogging()
						.payingWith("sender")
						.suppressStats(true)
						.hasRetryPrecheckFrom(BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED)
						.deferStatusResolution()
		};

		return defaultHapiSpec("RunCryptoTransfers")
				.given(
						withOpContext((spec, ignore) -> settings.setFrom(spec.setup().ciPropertiesMap())),
						logIt(ignore -> settings.toString())
				).when(
						cryptoCreate("sender").balance(initialBalance.getAsLong())
								.withRecharging()
								.rechargeWindow(3)
								.hasRetryPrecheckFrom(BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED),
						cryptoCreate("receiver")
								.hasRetryPrecheckFrom(BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED)
				).then(
						defaultLoadTest(transferBurst, settings)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
