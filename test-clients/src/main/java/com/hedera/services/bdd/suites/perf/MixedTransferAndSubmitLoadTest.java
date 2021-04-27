package com.hedera.services.bdd.suites.perf;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.suites.HapiApiSuite;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.createTopic;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.submitMessageTo;
import static com.hedera.services.bdd.spec.transactions.crypto.HapiCryptoTransfer.tinyBarsFromTo;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.inParallel;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.logIt;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.runLoadTest;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.BUSY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.DUPLICATE_TRANSACTION;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.PLATFORM_TRANSACTION_NOT_CREATED;
import static java.util.concurrent.TimeUnit.MINUTES;

public class MixedTransferAndSubmitLoadTest extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(MixedTransferAndSubmitLoadTest.class);

	public static void main(String... args) {
		MixedTransferAndSubmitLoadTest suite = new MixedTransferAndSubmitLoadTest();
		suite.setReportStats(true);
		suite.runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(runMixedTransferAndSubmits());
	}

	@Override
	public boolean hasInterestingStats() {
		return true;
	}

	private HapiApiSpec runMixedTransferAndSubmits() {
		PerfTestLoadSettings settings = new PerfTestLoadSettings();
		final AtomicInteger submittedSoFar = new AtomicInteger(0);

		Supplier<HapiSpecOperation[]> transferBurst = () -> new HapiSpecOperation[] {
				inParallel(flattened(
						IntStream.range(0, settings.getBurstSize() / 2)
								.mapToObj(ignore ->
										cryptoTransfer(tinyBarsFromTo("sender", "receiver", 1L))
												.noLogging()
												.hasPrecheckFrom(
														OK, BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED)
												.deferStatusResolution())
								.toArray(n -> new HapiSpecOperation[n]),
						IntStream.range(0, settings.getBurstSize() / 2)
								.mapToObj(ignore ->
										submitMessageTo("topic")
												.message("A fascinating item of general interest!")
												.noLogging()
												.hasPrecheckFrom(
														OK, BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED)
												.deferStatusResolution())
								.toArray(n -> new HapiSpecOperation[n])
				)),
				logIt(ignore ->
						String.format(
								"Now a 50/50 mix of %d transfers and messages submitted in total.",
								submittedSoFar.addAndGet(settings.getBurstSize()))),
		};

		return defaultHapiSpec("RunMixedTransferAndSubmits")
				.given(
						withOpContext((spec, ignore) -> settings.setFrom(spec.setup().ciPropertiesMap())),
						logIt(ignore -> settings.toString())
				).when(
						createTopic("topic"),
						cryptoCreate("sender").balance(999_999_999_999_999L),
						cryptoCreate("receiver")
				).then(
						runLoadTest(transferBurst)
								.tps(settings::getTps)
								.tolerance(settings::getTolerancePercentage)
								.allowedSecsBelow(settings::getAllowedSecsBelow)
								.lasting(settings::getMins, () -> MINUTES)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}


