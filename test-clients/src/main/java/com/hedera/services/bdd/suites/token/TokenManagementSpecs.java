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
import static com.hedera.services.bdd.spec.queries.crypto.ExpectedTokenRel.relationshipWith;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.burnToken;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.grantTokenKyc;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.mintToken;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.revokeTokenKyc;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenAssociate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenFreeze;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenUnfreeze;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.wipeTokenAccount;
import static com.hedera.services.bdd.spec.transactions.token.TokenMovement.moving;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.ACCOUNT_FROZEN_FOR_TOKEN;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INSUFFICIENT_TOKEN_BALANCE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_SIGNATURE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_BURN_AMOUNT;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_MINT_AMOUNT;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_WIPING_AMOUNT;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_FREEZE_KEY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_KYC_KEY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_SUPPLY_KEY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_WIPE_KEY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_NOT_ASSOCIATED_TO_ACCOUNT;
import static com.hederahashgraph.api.proto.java.TokenFreezeStatus.Frozen;
import static com.hederahashgraph.api.proto.java.TokenKycStatus.Revoked;

public class TokenManagementSpecs extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(TokenManagementSpecs.class);

	public static void main(String... args) {
		new TokenManagementSpecs().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return allOf(
				List.of(new HapiApiSpec[] {
								freezeMgmtFailureCasesWork(),
								freezeMgmtSuccessCasesWork(),
								kycMgmtFailureCasesWork(),
								kycMgmtSuccessCasesWork(),
								supplyMgmtSuccessCasesWork(),
								wipeAccountFailureCasesWork(),
								wipeAccountSuccessCasesWork(),
								supplyMgmtFailureCasesWork(),
								burnTokenFailsDueToInsufficientTreasuryBalance(),
								frozenTreasuryCannotBeMintedOrBurned(),
								revokedKYCTreasuryCannotBeMintedOrBurned(),
						}
				)
		);
	}

	private HapiApiSpec frozenTreasuryCannotBeMintedOrBurned() {
		return defaultHapiSpec("FrozenTreasuryCannotBeMintedOrBurned")
				.given(
						newKeyNamed("supplyKey"),
						newKeyNamed("freezeKey"),
						cryptoCreate(TOKEN_TREASURY).balance(0L)
				).when(
						tokenCreate("supple")
								.freezeKey("freezeKey")
								.supplyKey("supplyKey")
								.initialSupply(1)
								.treasury(TOKEN_TREASURY)
				).then(
						tokenFreeze("supple", TOKEN_TREASURY),
						mintToken("supple", 1)
								.hasKnownStatus(ACCOUNT_FROZEN_FOR_TOKEN),
						burnToken("supple", 1)
								.hasKnownStatus(ACCOUNT_FROZEN_FOR_TOKEN),
						getTokenInfo("supple")
								.hasTotalSupply(1),
						getAccountInfo(TOKEN_TREASURY)
								.hasToken(
										relationshipWith("supple")
												.balance(1)
												.freeze(Frozen)
								)
				);
	}

	private HapiApiSpec revokedKYCTreasuryCannotBeMintedOrBurned() {
		return defaultHapiSpec("RevokedKYCTreasuryCannotBeMintedOrBurned")
				.given(
						newKeyNamed("supplyKey"),
						newKeyNamed("kycKey"),
						cryptoCreate(TOKEN_TREASURY).balance(0L)
				).when(
						tokenCreate("supple")
								.kycKey("kycKey")
								.supplyKey("supplyKey")
								.initialSupply(1)
								.treasury(TOKEN_TREASURY)
				).then(
						revokeTokenKyc("supple", TOKEN_TREASURY),
						mintToken("supple", 1)
								.hasKnownStatus(ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN),
						burnToken("supple", 1)
								.hasKnownStatus(ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN),
						getTokenInfo("supple")
								.hasTotalSupply(1),
						getAccountInfo(TOKEN_TREASURY)
								.hasToken(
										relationshipWith("supple")
												.balance(1)
												.kyc(Revoked)
								)
				);
	}

	private HapiApiSpec burnTokenFailsDueToInsufficientTreasuryBalance() {
		final String BURN_TOKEN = "burn";
		final int TOTAL_SUPPLY = 100;
		final int TRANSFER_AMOUNT = 50;
		final int BURN_AMOUNT = 60;

		return defaultHapiSpec("BurnTokenFailsDueToInsufficientTreasuryBalance")
				.given(
						newKeyNamed("burnKey"),
						cryptoCreate("misc").balance(0L),
						cryptoCreate(TOKEN_TREASURY).balance(0L)
				).when(
						tokenCreate(BURN_TOKEN)
								.treasury(TOKEN_TREASURY)
								.initialSupply(TOTAL_SUPPLY)
								.supplyKey("burnKey"),
						tokenAssociate("misc", BURN_TOKEN),
						cryptoTransfer(
								moving(TRANSFER_AMOUNT, BURN_TOKEN)
										.between(TOKEN_TREASURY, "misc")),
						getAccountBalance("misc")
								.hasTokenBalance(BURN_TOKEN, TRANSFER_AMOUNT),
						getAccountBalance(TOKEN_TREASURY)
								.hasTokenBalance(BURN_TOKEN, TRANSFER_AMOUNT),
						getAccountInfo("misc").logged(),
						burnToken(BURN_TOKEN, BURN_AMOUNT)
								.hasKnownStatus(INSUFFICIENT_TOKEN_BALANCE)
								.via("wipeTxn"),
						getTokenInfo(BURN_TOKEN).logged(),
						getAccountInfo("misc").logged()

				).then(
						getTokenInfo(BURN_TOKEN)
								.hasTotalSupply(TOTAL_SUPPLY),
						getAccountBalance(TOKEN_TREASURY)
								.hasTokenBalance(BURN_TOKEN, TRANSFER_AMOUNT),
						getTxnRecord("wipeTxn").logged()
				);
	}

	public HapiApiSpec wipeAccountSuccessCasesWork() {
		var wipeableToken = "with";

		return defaultHapiSpec("WipeAccountSuccessCasesWork")
				.given(
						newKeyNamed("wipeKey"),
						cryptoCreate("misc").balance(0L),
						cryptoCreate(TOKEN_TREASURY).balance(0L)
				).when(
						tokenCreate(wipeableToken)
								.treasury(TOKEN_TREASURY)
								.initialSupply(1_000)
								.wipeKey("wipeKey"),
						tokenAssociate("misc", wipeableToken),
						cryptoTransfer(
								moving(500, wipeableToken).between(TOKEN_TREASURY, "misc")),
						getAccountBalance("misc")
								.hasTokenBalance(wipeableToken, 500),
						getAccountBalance(TOKEN_TREASURY)
								.hasTokenBalance(wipeableToken, 500),
						getAccountInfo("misc").logged(),
						wipeTokenAccount(wipeableToken, "misc", 500)
								.via("wipeTxn"),
						getAccountInfo("misc").logged()
				).then(
						getAccountBalance("misc")
								.hasTokenBalance(wipeableToken, 0),
						getTokenInfo(wipeableToken)
								.hasTotalSupply(500),
						getAccountBalance(TOKEN_TREASURY)
								.hasTokenBalance(wipeableToken, 500),
						getTxnRecord("wipeTxn").logged()
				);
	}

	public HapiApiSpec wipeAccountFailureCasesWork() {
		var unwipeableToken = "without";
		var wipeableToken = "with";
		var anotherWipeableToken = "anotherWith";

		return defaultHapiSpec("WipeAccountFailureCasesWork")
				.given(
						newKeyNamed("wipeKey"),
						cryptoCreate("misc").balance(0L),
						cryptoCreate(TOKEN_TREASURY).balance(0L)
				).when(
						tokenCreate(unwipeableToken)
								.treasury(TOKEN_TREASURY),
						tokenCreate(wipeableToken)
								.treasury(TOKEN_TREASURY)
								.wipeKey("wipeKey"),
						tokenCreate(anotherWipeableToken)
								.treasury(TOKEN_TREASURY)
								.initialSupply(1_000)
								.wipeKey("wipeKey"),
						tokenAssociate("misc", anotherWipeableToken),
						cryptoTransfer(
								moving(500, anotherWipeableToken).between(TOKEN_TREASURY, "misc"))
				).then(
						wipeTokenAccount(unwipeableToken, TOKEN_TREASURY, 1)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_WIPE_KEY),
						wipeTokenAccount(wipeableToken, "misc", 1)
								.hasKnownStatus(TOKEN_NOT_ASSOCIATED_TO_ACCOUNT),
						wipeTokenAccount(wipeableToken, TOKEN_TREASURY, 1)
								.signedBy(GENESIS)
								.hasKnownStatus(INVALID_SIGNATURE),
						wipeTokenAccount(wipeableToken, TOKEN_TREASURY, 1)
								.hasKnownStatus(CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT),
						wipeTokenAccount(anotherWipeableToken, "misc", 501)
								.hasKnownStatus(INVALID_WIPING_AMOUNT),
						wipeTokenAccount(anotherWipeableToken, "misc", -1)
								.hasPrecheck(INVALID_WIPING_AMOUNT),
						wipeTokenAccount(anotherWipeableToken, "misc", 0)
								.hasPrecheck(INVALID_WIPING_AMOUNT)
				);
	}

	public HapiApiSpec kycMgmtFailureCasesWork() {
		var withoutKycKey = "withoutKycKey";
		var withKycKey = "withKycKey";

		return defaultHapiSpec("KycMgmtFailureCasesWork")
				.given(
						newKeyNamed("oneKyc"),
						cryptoCreate(TOKEN_TREASURY).balance(0L),
						tokenCreate(withoutKycKey)
								.treasury(TOKEN_TREASURY),
						tokenCreate(withKycKey)
								.kycKey("oneKyc")
								.treasury(TOKEN_TREASURY)
				).when(
						grantTokenKyc(withoutKycKey, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_KYC_KEY),
						grantTokenKyc(withKycKey, "1.2.3")
								.hasKnownStatus(INVALID_ACCOUNT_ID),
						grantTokenKyc(withKycKey, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(INVALID_SIGNATURE),
						grantTokenKyc(withoutKycKey, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_KYC_KEY),
						revokeTokenKyc(withKycKey, "1.2.3")
								.hasKnownStatus(INVALID_ACCOUNT_ID),
						revokeTokenKyc(withKycKey, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(INVALID_SIGNATURE)
				).then(
						getTokenInfo(withoutKycKey)
								.hasRegisteredId(withoutKycKey)
								.logged()
				);
	}

	public HapiApiSpec freezeMgmtFailureCasesWork() {
		var unfreezableToken = "without";
		var freezableToken = "withPlusDefaultTrue";

		return defaultHapiSpec("FreezeMgmtFailureCasesWork")
				.given(
						newKeyNamed("oneFreeze"),
						cryptoCreate(TOKEN_TREASURY).balance(0L),
						cryptoCreate("go").balance(0L),
						tokenCreate(unfreezableToken)
								.treasury(TOKEN_TREASURY),
						tokenCreate(freezableToken)
								.freezeDefault(true)
								.freezeKey("oneFreeze")
								.treasury(TOKEN_TREASURY)
				).when(
						tokenFreeze(unfreezableToken, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_FREEZE_KEY),
						tokenFreeze(freezableToken, "1.2.3")
								.hasKnownStatus(INVALID_ACCOUNT_ID),
						tokenFreeze(freezableToken, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(INVALID_SIGNATURE),
						tokenFreeze(freezableToken, "go")
								.hasKnownStatus(TOKEN_NOT_ASSOCIATED_TO_ACCOUNT),
						tokenUnfreeze(freezableToken, "go")
								.hasKnownStatus(TOKEN_NOT_ASSOCIATED_TO_ACCOUNT),
						tokenUnfreeze(unfreezableToken, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_FREEZE_KEY),
						tokenUnfreeze(freezableToken, "1.2.3")
								.hasKnownStatus(INVALID_ACCOUNT_ID),
						tokenUnfreeze(freezableToken, TOKEN_TREASURY)
								.signedBy(GENESIS)
								.hasKnownStatus(INVALID_SIGNATURE)
				).then(
						getTokenInfo(unfreezableToken)
								.hasRegisteredId(unfreezableToken)
								.logged()
				);
	}

	public HapiApiSpec freezeMgmtSuccessCasesWork() {
		var withPlusDefaultFalse = "withPlusDefaultFalse";

		return defaultHapiSpec("FreezeMgmtSuccessCasesWork")
				.given(
						cryptoCreate(TOKEN_TREASURY).balance(0L),
						cryptoCreate("misc").balance(0L),
						newKeyNamed("oneFreeze"),
						newKeyNamed("twoFreeze"),
						tokenCreate(withPlusDefaultFalse)
								.freezeDefault(false)
								.freezeKey("twoFreeze")
								.treasury(TOKEN_TREASURY),
						tokenAssociate("misc", withPlusDefaultFalse)
				).when(
						cryptoTransfer(
								moving(1, withPlusDefaultFalse)
										.between(TOKEN_TREASURY, "misc")),
						tokenFreeze(withPlusDefaultFalse, "misc"),
						cryptoTransfer(
								moving(1, withPlusDefaultFalse)
										.between(TOKEN_TREASURY, "misc"))
								.hasKnownStatus(ACCOUNT_FROZEN_FOR_TOKEN),
						getAccountInfo("misc").logged(),
						tokenUnfreeze(withPlusDefaultFalse, "misc"),
						cryptoTransfer(
								moving(1, withPlusDefaultFalse)
										.between(TOKEN_TREASURY, "misc"))
				).then(
						getAccountInfo("misc").logged()
				);
	}

	public HapiApiSpec kycMgmtSuccessCasesWork() {
		var withKycKey = "withKycKey";
		var withoutKycKey = "withoutKycKey";

		return defaultHapiSpec("KycMgmtSuccessCasesWork")
				.given(
						cryptoCreate(TOKEN_TREASURY).balance(0L),
						cryptoCreate("misc").balance(0L),
						newKeyNamed("oneKyc"),
						newKeyNamed("twoKyc"),
						tokenCreate(withKycKey)
								.kycKey("oneKyc")
								.treasury(TOKEN_TREASURY),
						tokenCreate(withoutKycKey)
								.treasury(TOKEN_TREASURY),
						tokenAssociate("misc", withKycKey, withoutKycKey)
				).when(
						cryptoTransfer(
								moving(1, withKycKey)
										.between(TOKEN_TREASURY, "misc"))
								.hasKnownStatus(ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN),
						getAccountInfo("misc").logged(),
						grantTokenKyc(withKycKey, "misc"),
						cryptoTransfer(
								moving(1, withKycKey)
										.between(TOKEN_TREASURY, "misc")),
						revokeTokenKyc(withKycKey, "misc"),
						cryptoTransfer(
								moving(1, withKycKey)
										.between(TOKEN_TREASURY, "misc"))
								.hasKnownStatus(ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN),
						cryptoTransfer(
								moving(1, withoutKycKey)
										.between(TOKEN_TREASURY, "misc"))
				).then(
						getAccountInfo("misc").logged()
				);
	}

	public HapiApiSpec supplyMgmtSuccessCasesWork() {
		return defaultHapiSpec("SupplyMgmtSuccessCasesWork")
				.given(
						cryptoCreate(TOKEN_TREASURY).balance(0L),
						newKeyNamed("supplyKey"),
						tokenCreate("supple")
								.supplyKey("supplyKey")
								.initialSupply(10)
								.decimals(1)
								.treasury(TOKEN_TREASURY)
				).when(
						getTokenInfo("supple").logged(),
						getAccountBalance(TOKEN_TREASURY).logged(),
						mintToken("supple", 100).via("mintTxn"),
						burnToken("supple", 50).via("burnTxn")
				).then(
						getAccountInfo(TOKEN_TREASURY).logged(),
						getTokenInfo("supple").logged(),
						getTxnRecord("mintTxn").logged(),
						getTxnRecord("burnTxn").logged()
				);
	}

	public HapiApiSpec supplyMgmtFailureCasesWork() {
		return defaultHapiSpec("SupplyMgmtFailureCasesWork")
				.given(
						newKeyNamed("supplyKey")
				).when(
						tokenCreate("rigid"),
						tokenCreate("supple")
								.supplyKey("supplyKey")
								.decimals(16)
								.initialSupply(1)
				).then(
						mintToken("rigid", 1)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_SUPPLY_KEY),
						burnToken("rigid", 1)
								.signedBy(GENESIS)
								.hasKnownStatus(TOKEN_HAS_NO_SUPPLY_KEY),
						mintToken("supple", Long.MAX_VALUE)
								.hasKnownStatus(INVALID_TOKEN_MINT_AMOUNT),
						mintToken("supple", 0)
								.hasPrecheck(INVALID_TOKEN_MINT_AMOUNT),
						burnToken("supple", 2)
								.hasKnownStatus(INVALID_TOKEN_BURN_AMOUNT),
						burnToken("supple", 0)
								.hasPrecheck(INVALID_TOKEN_BURN_AMOUNT)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
