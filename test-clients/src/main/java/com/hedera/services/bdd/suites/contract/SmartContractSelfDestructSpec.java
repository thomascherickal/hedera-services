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
import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hedera.services.bdd.spec.utilops.CustomSpecAssert;
import com.hedera.services.bdd.suites.HapiApiSuite;
import com.hederahashgraph.api.proto.java.AccountID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.List;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.assertions.ContractFnResultAsserts.isLiteralResult;
import static com.hedera.services.bdd.spec.assertions.ContractFnResultAsserts.resultWith;
import static com.hedera.services.bdd.spec.assertions.TransactionRecordAsserts.recordWith;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTxnRecord;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCall;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.asId;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_SOLIDITY_ADDRESS;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OBTAINER_SAME_CONTRACT_ID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.SUCCESS;

public class SmartContractSelfDestructSpec extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(SmartContractSelfDestructSpec.class);

	public static void main(String... args) {
		new org.ethereum.crypto.HashUtil();
		new SmartContractSelfDestructSpec().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[] {
				payTestSelfDestructCall(),
		});
	}

	HapiApiSpec payTestSelfDestructCall() {
		return defaultHapiSpec("payTestSelfDestructCall")
				.given(
						cryptoCreate("payer").balance(1_000_000_000_000L).logged(),
						cryptoCreate("receiver").balance( 1_000L),
						fileCreate("bytecode")
								.path(ContractResources.PAY_TEST_SELF_DESTRUCT_BYTECODE_PATH),
						contractCreate("payTestSelfDestruct")
								.bytecode("bytecode")
				).when(
						withOpContext((spec, opLog) -> {
							var subop1 = contractCall("payTestSelfDestruct", ContractResources.DEPOSIT_ABI, 1_000L)
									.payingWith("payer")
									.gas(300_000L)
									.via("deposit")
									.sending(1_000L);

                            var subop2 = contractCall("payTestSelfDestruct", ContractResources.GET_BALANCE_ABI)
									 .payingWith("payer")
									 .gas(300_000L)
									 .via("getBalance");

							AccountID contractAccountId = asId("payTestSelfDestruct", spec);
							var subop3 = contractCall("payTestSelfDestruct", ContractResources.KILL_ME_ABI, contractAccountId.getAccountNum() )
									.payingWith("payer")
									.gas(300_000L)
									.hasKnownStatus(OBTAINER_SAME_CONTRACT_ID);

							var subop4 = contractCall("payTestSelfDestruct", ContractResources.KILL_ME_ABI, 999_999L)
									.payingWith("payer")
									.gas(300_000L)
									.hasKnownStatus(INVALID_SOLIDITY_ADDRESS);

							AccountID receiverAccountId = asId("receiver", spec);
							var subop5 = contractCall("payTestSelfDestruct", ContractResources.KILL_ME_ABI, receiverAccountId.getAccountNum())
									.payingWith("payer")
									.gas(300_000L)
									.via("selfDestruct")
									.hasKnownStatus(SUCCESS);

							CustomSpecAssert.allRunFor(spec, subop1, subop2,subop3, subop4, subop5);
						})
					).then(
						getTxnRecord("deposit"),
						getTxnRecord("getBalance")
								.hasPriority(recordWith().contractCallResult(
										resultWith().resultThruAbi(
												ContractResources.GET_BALANCE_ABI,
												isLiteralResult(new Object[] { BigInteger.valueOf(1_000L) })))),
						getAccountBalance("receiver")
								.hasTinyBars(2_000L)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
