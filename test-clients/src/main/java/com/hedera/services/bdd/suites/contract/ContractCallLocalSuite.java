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

import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.sleepFor;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INSUFFICIENT_TX_FEE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.RESULT_SIZE_LIMIT_EXCEEDED;
import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.keys.KeyFactory.KeyType.THRESHOLD;
import static com.hedera.services.bdd.spec.assertions.ContractFnResultAsserts.isLiteralResult;
import static com.hedera.services.bdd.spec.assertions.ContractFnResultAsserts.resultWith;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.contractCallLocal;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCall;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;

public class ContractCallLocalSuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(ContractCallLocalSuite.class);

	public static void main(String... args) {
		new ContractCallLocalSuite().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return allOf(
				positiveSpecs(),
				negativeSpecs()
		);
	}

	private List<HapiApiSpec> negativeSpecs() {
		return Arrays.asList(
				impureCallFails(),
				insufficientFeeFails(),
				undersizedMaxResultFails(),
				lowBalanceFails()
		);
	}

	private List<HapiApiSpec> positiveSpecs() {
		return Arrays.asList(
				vanillaSuccess()
		);
	}

	private HapiApiSpec vanillaSuccess() {
		return defaultHapiSpec("VanillaSuccess")
				.given(
						fileCreate("parentDelegateBytecode").path(ContractResources.DELEGATING_CONTRACT_BYTECODE_PATH),
						contractCreate("parentDelegate").bytecode("parentDelegateBytecode").adminKey(THRESHOLD)
				).when(
						contractCall("parentDelegate", ContractResources.CREATE_CHILD_ABI)
				).then(
						sleepFor(3_000L),
						contractCallLocal("parentDelegate", ContractResources.GET_CHILD_RESULT_ABI)
								.has(resultWith().resultThruAbi(
										ContractResources.GET_CHILD_RESULT_ABI,
										isLiteralResult(new Object[] { BigInteger.valueOf(7L) })))
				);
	}

	private HapiApiSpec impureCallFails() {
		return defaultHapiSpec("ImpureCallFails")
				.given(
						fileCreate("parentDelegateBytecode").path(ContractResources.DELEGATING_CONTRACT_BYTECODE_PATH),
						contractCreate("parentDelegate").bytecode("parentDelegateBytecode").adminKey(THRESHOLD)
				).when().then(
						sleepFor(3_000L),
						contractCallLocal("parentDelegate", ContractResources.CREATE_CHILD_ABI)
								.nodePayment(1_234_567)
								.hasAnswerOnlyPrecheck(ResponseCodeEnum.LOCAL_CALL_MODIFICATION_EXCEPTION)
				);
	}

	private HapiApiSpec insufficientFeeFails() {
		final long ADEQUATE_QUERY_PAYMENT = 500_000L;

		return defaultHapiSpec("InsufficientFee")
				.given(
						fileCreate("parentDelegateBytecode").path(ContractResources.DELEGATING_CONTRACT_BYTECODE_PATH),
						contractCreate("parentDelegate").bytecode("parentDelegateBytecode")
				).when(
						contractCall("parentDelegate", ContractResources.CREATE_CHILD_ABI)
				).then(
						sleepFor(3_000L),
						contractCallLocal("parentDelegate", ContractResources.GET_CHILD_RESULT_ABI)
								.nodePayment(ADEQUATE_QUERY_PAYMENT)
								.fee(0L)
								.hasAnswerOnlyPrecheck(INSUFFICIENT_TX_FEE));
	}

	private HapiApiSpec lowBalanceFails() {
		final long ADEQUATE_QUERY_PAYMENT = 500_000_000L;

		return defaultHapiSpec("LowBalanceFails")
				.given(
						fileCreate("parentDelegateBytecode")
								.path(ContractResources.DELEGATING_CONTRACT_BYTECODE_PATH),
						contractCreate("parentDelegate").bytecode("parentDelegateBytecode"),
						cryptoCreate("payer").balance(ADEQUATE_QUERY_PAYMENT)
				).when(
						contractCall("parentDelegate", ContractResources.CREATE_CHILD_ABI)
				).then(
						sleepFor(3_000L),
						contractCallLocal("parentDelegate", ContractResources.GET_CHILD_RESULT_ABI)
								.logged()
								.payingWith("payer")
								.nodePayment(ADEQUATE_QUERY_PAYMENT)
								.hasAnswerOnlyPrecheck(INSUFFICIENT_PAYER_BALANCE),
						getAccountBalance("payer").logged(),
						sleepFor(1_000L),
						getAccountBalance("payer").logged()
				);
	}

	private HapiApiSpec undersizedMaxResultFails() {
		return defaultHapiSpec("UndersizedMaxResult")
				.given(
						fileCreate("parentDelegateBytecode").path(ContractResources.DELEGATING_CONTRACT_BYTECODE_PATH),
						contractCreate("parentDelegate").bytecode("parentDelegateBytecode")
				).when(
						contractCall("parentDelegate", ContractResources.CREATE_CHILD_ABI)
				).then(
						sleepFor(3_000L),
						contractCallLocal("parentDelegate", ContractResources.GET_CHILD_RESULT_ABI)
								.maxResultSize(1L)
								.nodePayment(1_234_567)
								.hasAnswerOnlyPrecheck(RESULT_SIZE_LIMIT_EXCEEDED));
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
