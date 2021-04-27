package com.hedera.services.bdd.suites.issues;

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
import static com.hedera.services.bdd.spec.HapiApiSpec.*;

import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hedera.services.bdd.spec.transactions.crypto.HapiCryptoTransfer.tinyBarsFromTo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.*;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.*;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.NOT_SUPPORTED;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Issue2098Spec extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(Issue2098Spec.class);

	public static void main(String... args) {
		new Issue2098Spec().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(
				new HapiApiSpec[]{
						queryApiPermissionsChangeImmediately(),
						txnApiPermissionsChangeImmediately(),
						adminsCanQueryNoMatterPermissions(),
						adminsCanTransactNoMatterPermissions(),
				}
		);
	}

	private HapiApiSpec txnApiPermissionsChangeImmediately() {
		return defaultHapiSpec("TxnApiPermissionsChangeImmediately")
				.given(
						cryptoCreate("civilian")
				).when(
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.erasingProps(Set.of("cryptoTransfer"))
				).then(
						cryptoTransfer(tinyBarsFromTo("civilian", FUNDING, 1L))
								.payingWith("civilian")
								.hasPrecheck(NOT_SUPPORTED),
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.overridingProps(Map.of("cryptoTransfer", "0-*")),
						cryptoTransfer(tinyBarsFromTo("civilian", FUNDING, 1L))
								.payingWith("civilian")
				);
	}

	private HapiApiSpec queryApiPermissionsChangeImmediately() {
		return defaultHapiSpec("QueryApiPermissionsChangeImmediately")
				.given(
						cryptoCreate("civilian"),
						createTopic("misc")
				).when(
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.erasingProps(Set.of("getTopicInfo"))
				).then(
						getTopicInfo("misc").payingWith("civilian").hasCostAnswerPrecheck(NOT_SUPPORTED),
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.overridingProps(Map.of("getTopicInfo", "0-*")),
						getTopicInfo("misc").payingWith("civilian")
				);
	}

	private HapiApiSpec adminsCanQueryNoMatterPermissions() {
		return defaultHapiSpec("AdminsCanQueryNoMatterPermissions")
				.given(
						cryptoCreate("civilian"),
						createTopic("misc")
				).when(
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.erasingProps(Set.of("getTopicInfo"))
				).then(
						getTopicInfo("misc").payingWith("civilian").hasCostAnswerPrecheck(NOT_SUPPORTED),
						getTopicInfo("misc"),
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.overridingProps(Map.of("getTopicInfo", "0-*"))
				);
	}

	private HapiApiSpec adminsCanTransactNoMatterPermissions() {
		return defaultHapiSpec("AdminsCanTransactNoMatterPermissions")
				.given(
						cryptoCreate("civilian")
				).when(
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.erasingProps(Set.of("cryptoTransfer"))
				).then(
						cryptoTransfer(tinyBarsFromTo("civilian", FUNDING, 1L))
								.payingWith("civilian")
								.hasPrecheck(NOT_SUPPORTED),
						cryptoTransfer(tinyBarsFromTo(GENESIS, FUNDING, 1L)),
						fileUpdate(API_PERMISSIONS)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.overridingProps(Map.of("cryptoTransfer", "0-*"))
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}

