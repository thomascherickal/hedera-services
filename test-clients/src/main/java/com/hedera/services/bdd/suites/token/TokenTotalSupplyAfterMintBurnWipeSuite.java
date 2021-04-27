package com.hedera.services.bdd.suites.token;

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
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountInfo;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTokenInfo;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTxnRecord;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.burnToken;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.mintToken;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenAssociate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.wipeTokenAccount;
import static com.hedera.services.bdd.spec.transactions.token.TokenMovement.moving;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;

public class TokenTotalSupplyAfterMintBurnWipeSuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(TokenTotalSupplyAfterMintBurnWipeSuite.class);

	private static String TOKEN_TREASURY = "treasury";

	public static void main(String... args) {
		new TokenTotalSupplyAfterMintBurnWipeSuite().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[] {
						checkTokenTotalSupplyAfterMintAndBurn(),
						totalSupplyAfterWipe()
				}
		);
	}


	public HapiApiSpec checkTokenTotalSupplyAfterMintAndBurn() {
		String tokenName = "tokenToTest";
		return defaultHapiSpec("checkTokenTotalSupplyAfterMintAndBurn")
				.given(
						cryptoCreate(TOKEN_TREASURY).balance(0L),
						cryptoCreate("tokenReceiver").balance(0L),
						newKeyNamed("adminKey"),
						newKeyNamed("supplyKey")
				).when(
						tokenCreate(tokenName)
								.treasury(TOKEN_TREASURY)
								.initialSupply(1000)
								.decimals(1)
								.supplyKey("supplyKey")
								.via("createTxn")
				).then(
						getTxnRecord("createTxn").logged(),
						mintToken(tokenName, 1000).via("mintToken"),
						getTxnRecord("mintToken").logged(),
						getTokenInfo(tokenName)
								.hasTreasury(TOKEN_TREASURY)
								.hasTotalSupply(2000),
						burnToken(tokenName, 200).via("burnToken"),
						getTxnRecord("burnToken").logged(),

						getTokenInfo(tokenName)
								.logged()
								.hasTreasury(TOKEN_TREASURY)
								.hasTotalSupply(1800)

				);
	}

	public HapiApiSpec totalSupplyAfterWipe() {
		var tokenToWipe = "tokenToWipe";

		return defaultHapiSpec("totalSupplyAfterWipe")
				.given(
						newKeyNamed("wipeKey"),
						cryptoCreate("assoc1").balance(0L),
						cryptoCreate("assoc2").balance(0L),
						cryptoCreate(TOKEN_TREASURY).balance(0L)
				).when(
						tokenCreate(tokenToWipe)
								.name(tokenToWipe)
								.treasury(TOKEN_TREASURY)
								.initialSupply(1_000)
								.wipeKey("wipeKey"),
						tokenAssociate("assoc1", tokenToWipe),
						tokenAssociate("assoc2", tokenToWipe),
						cryptoTransfer(
								moving(500, tokenToWipe).between(TOKEN_TREASURY, "assoc1")),
						cryptoTransfer(
								moving(200, tokenToWipe).between(TOKEN_TREASURY, "assoc2")),
						getAccountBalance("assoc1")
								.hasTokenBalance(tokenToWipe, 500),
						getAccountBalance(TOKEN_TREASURY)
								.hasTokenBalance(tokenToWipe, 300),
						getAccountInfo("assoc1").logged(),

						wipeTokenAccount(tokenToWipe, "assoc1", 200)
								.via("wipeTxn1").logged(),
						wipeTokenAccount(tokenToWipe, "assoc2", 200)
								.via("wipeTxn2").logged()
				).then(
						getAccountBalance("assoc2")
								.hasTokenBalance(tokenToWipe, 0),
						getTokenInfo(tokenToWipe)
								.hasTotalSupply(600)
								.hasName(tokenToWipe)
								.logged(),
						getAccountBalance(TOKEN_TREASURY)
								.hasTokenBalance(tokenToWipe, 300)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
