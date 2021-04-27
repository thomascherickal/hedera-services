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
import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hedera.services.bdd.spec.transactions.TxnVerbs;
import com.hedera.services.bdd.spec.utilops.CustomSpecAssert;
import com.hedera.services.bdd.suites.HapiApiSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCall;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileUpdate;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;

public class BigArraySpec extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(BigArraySpec.class);

	public static void main(String... args) {
		/* Has a static initializer whose behavior seems influenced by initialization of ForkJoinPool#commonPool. */
		new org.ethereum.crypto.HashUtil();

		new BigArraySpec().runSuiteAsync();
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[] {
				bigArray(),
		});
	}

	HapiApiSpec bigArray() {
		var MAX_CONTRACT_STORAGE_ALLOWED = 1025;

		return HapiApiSpec.defaultHapiSpec("BigArray")
				.given(
						fileUpdate(APP_PROPERTIES)
								.overridingProps(Map.of(
										"contracts.maxStorageKb", "" + MAX_CONTRACT_STORAGE_ALLOWED
								)).payingWith(ADDRESS_BOOK_CONTROL),
						TxnVerbs.fileCreate("bigArrayContractFile")
								.path(ContractResources.GROW_ARRAY_BYTECODE_PATH),
						TxnVerbs.contractCreate("bigArrayContract")
								.bytecode("bigArrayContractFile")
				)
				.when(
						withOpContext((spec, opLog) -> {
							int kbPerStep = 16;
							List<HapiSpecOperation> subOps = new ArrayList<>();

							for (int sizeNow = kbPerStep; sizeNow < MAX_CONTRACT_STORAGE_ALLOWED; sizeNow += kbPerStep) {
								var subOp1 = contractCall(
										"bigArrayContract", ContractResources.BIG_ARRAY_GROW_TO_ABI, sizeNow)
										.gas(300_000L)
										.logged();
								subOps.add(subOp1);
							}
							CustomSpecAssert.allRunFor(spec, subOps);
						})
				)
				.then(
						withOpContext((spec, opLog) -> {
							long numberOfIterations = 10;
							List<HapiSpecOperation> subOps = new ArrayList<>();

							for (int i = 0; i < numberOfIterations; i++) {
								var subOp1 = contractCall(
										"bigArrayContract", ContractResources.BIG_ARRAY_CHANGE_ARRAY_ABI,
										ThreadLocalRandom.current().nextInt(1000))
										.logged();
								subOps.add(subOp1);
							}
							CustomSpecAssert.allRunFor(spec, subOps);
						})
				);
	}
}
