package com.hedera.services.bdd.suites.contract;

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

import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_ADMIN_KEY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.MODIFYING_IMMUTABLE_CONTRACT;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.*;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.*;

import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static com.hedera.services.bdd.spec.HapiApiSpec.*;

import java.util.Arrays;
import java.util.List;

public class DeprecatedContractKeySuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(DeprecatedContractKeySuite.class);

	public static void main(String... args) {
		new DeprecatedContractKeySuite().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return Arrays.asList(
				new HapiApiSpec[] {
						createWithDeprecatedKeyCreatesImmutableContract(),
						givenAdminKeyMustBeValid(),
				}
		);
	}

	private HapiApiSpec createWithDeprecatedKeyCreatesImmutableContract() {
		return defaultHapiSpec("CreateWithDeprecatedKeyCreatesImmutableContract")
				.given(
						fileCreate("bytecode").path(ContractResources.BALANCE_LOOKUP_BYTECODE_PATH)
				).when(
						contractCreate("target").bytecode("bytecode").useDeprecatedAdminKey()
				).then(
						contractDelete("target").hasKnownStatus(MODIFYING_IMMUTABLE_CONTRACT)
				);
	}

	private HapiApiSpec givenAdminKeyMustBeValid() {
		return defaultHapiSpec("GivenAdminKeyMustBeValid")
				.given(
						fileCreate("bytecode").path(ContractResources.BALANCE_LOOKUP_BYTECODE_PATH),
						contractCreate("target").bytecode("bytecode")
				).when(
						getContractInfo("target").logged()
				).then(
						contractUpdate("target")
								.useDeprecatedAdminKey()
								.signedBy(GENESIS, "target")
								.hasKnownStatus(INVALID_ADMIN_KEY)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
