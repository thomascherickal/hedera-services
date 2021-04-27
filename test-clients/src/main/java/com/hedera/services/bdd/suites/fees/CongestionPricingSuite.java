package com.hedera.services.bdd.suites.fees;

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
import com.hedera.services.bdd.spec.HapiSpecSetup;
import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hedera.services.bdd.spec.transactions.crypto.HapiCryptoTransfer;
import com.hedera.services.bdd.spec.utilops.UtilVerbs;
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTxnRecord;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCall;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileUpdate;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.blockingOrder;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hedera.services.bdd.suites.utils.sysfiles.serdes.ThrottleDefsLoader.protoDefsFromResource;

public class CongestionPricingSuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(CongestionPricingSuite.class);

	private static final String defaultCongestionMultipliers =
			HapiSpecSetup.getDefaultNodeProps().get("fees.percentCongestionMultipliers");
	private static final String defaultMinCongestionPeriod =
			HapiSpecSetup.getDefaultNodeProps().get("fees.minCongestionPeriod");

	public static void main(String... args) {
		new CongestionPricingSuite().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(
				new HapiApiSpec[] {
						canUpdateMultipliersDynamically(),
				}
		);
	}

	private HapiApiSpec canUpdateMultipliersDynamically() {
		var artificialLimits = protoDefsFromResource("testSystemFiles/artificial-limits.json");
		var defaultThrottles = protoDefsFromResource("testSystemFiles/throttles-dev.json");
		String tmpMinCongestionPeriod = "1";

		AtomicLong normalPrice = new AtomicLong();
		AtomicLong sevenXPrice = new AtomicLong();

		return defaultHapiSpec("CanUpdateMultipliersDynamically")
				.given(
						cryptoCreate("civilian")
								.payingWith(GENESIS)
								.balance(ONE_MILLION_HBARS),
						fileCreate("bytecode")
								.path(ContractResources.MULTIPURPOSE_BYTECODE_PATH)
								.payingWith(GENESIS),
						contractCreate("scMulti")
								.bytecode("bytecode")
								.payingWith(GENESIS),
						contractCall("scMulti")
								.payingWith("civilian")
								.fee(ONE_HUNDRED_HBARS)
								.sending(ONE_HBAR)
								.via("cheapCall"),
						getTxnRecord("cheapCall")
								.providingFeeTo(normalPrice::set)
				).when(
						fileUpdate(APP_PROPERTIES)
								.fee(ONE_HUNDRED_HBARS)
								.payingWith(EXCHANGE_RATE_CONTROL)
								.overridingProps(Map.of(
										"fees.percentCongestionMultipliers", "1,7x",
										"fees.minCongestionPeriod", tmpMinCongestionPeriod
								)),
						fileUpdate(THROTTLE_DEFS)
								.payingWith(EXCHANGE_RATE_CONTROL)
								.contents(artificialLimits.toByteArray()),
						blockingOrder(
								IntStream.range(0, 10).mapToObj(i ->
										contractCall("scMulti")
												.payingWith(GENESIS)
												.fee(ONE_HUNDRED_HBARS)
												.sending(ONE_HBAR))
										.toArray(HapiSpecOperation[]::new)
						),
						contractCall("scMulti")
								.payingWith("civilian")
								.fee(ONE_HUNDRED_HBARS)
								.sending(ONE_HBAR)
								.via("pricyCall")
				).then(
						getTxnRecord("pricyCall")
								.payingWith(GENESIS)
								.providingFeeTo(sevenXPrice::set),
						withOpContext((spec, opLog) -> {
							Assert.assertEquals(
									"~7x multiplier should be in affect!",
									7.0,
									(1.0 * sevenXPrice.get()) / normalPrice.get(),
									0.1);
						}),
						fileUpdate(THROTTLE_DEFS)
								.fee(ONE_HUNDRED_HBARS)
								.payingWith(EXCHANGE_RATE_CONTROL)
								.contents(defaultThrottles.toByteArray()),
						fileUpdate(APP_PROPERTIES)
								.fee(ONE_HUNDRED_HBARS)
								.payingWith(EXCHANGE_RATE_CONTROL)
								.overridingProps(Map.of(
										"fees.percentCongestionMultipliers", defaultCongestionMultipliers,
										"fees.minCongestionPeriod", defaultMinCongestionPeriod
								)),
						/* Make sure the multiplier is reset before the next spec runs */
						cryptoTransfer(HapiCryptoTransfer.tinyBarsFromTo(GENESIS, FUNDING, 1))
								.payingWith(GENESIS)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
