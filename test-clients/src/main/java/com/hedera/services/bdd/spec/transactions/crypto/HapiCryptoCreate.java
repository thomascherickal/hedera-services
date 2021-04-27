package com.hedera.services.bdd.spec.transactions.crypto;

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

import com.google.common.base.MoreObjects;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.CryptoCreateTransactionBody;
import com.hederahashgraph.api.proto.java.Duration;
import com.hederahashgraph.api.proto.java.HederaFunctionality;
import com.hederahashgraph.api.proto.java.Key;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionBody;
import com.hederahashgraph.api.proto.java.TransactionResponse;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.HapiPropertySource;
import com.hedera.services.bdd.spec.keys.SigControl;
import com.hedera.services.bdd.spec.transactions.HapiTxnOp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.hedera.services.bdd.spec.transactions.TxnFactory.bannerWith;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.SUCCESS;
import static com.hedera.services.bdd.spec.keys.KeyFactory.KeyType;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.netOf;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.UNKNOWN;

public class HapiCryptoCreate extends HapiTxnOp<HapiCryptoCreate> {
	static final Logger log = LogManager.getLogger(HapiCryptoCreate.class);

	private Key key;
	/**
	 * when create account were used as an account, whether perform auto recharge when receiving insufficient payer or
	 * insufficient transaction fee precheck
	 */
	private boolean recharging = false;
	private boolean advertiseCreation = false;
	/** The time window (unit of second) of not doing another recharge if just recharged recently */
	private Optional<Integer> rechargeWindow = Optional.empty();
	private String account;
	private Optional<Long> sendThresh = Optional.empty();
	private Optional<Long> receiveThresh = Optional.empty();
	private Optional<Long> initialBalance = Optional.empty();
	private Optional<Long> autoRenewDurationSecs = Optional.empty();
	private Optional<AccountID> proxy = Optional.empty();
	private Optional<Boolean> receiverSigRequired = Optional.empty();
	private Optional<String> keyName = Optional.empty();
	private Optional<String> entityMemo = Optional.empty();
	private Optional<KeyType> keyType = Optional.empty();
	private Optional<SigControl> keyShape = Optional.empty();
	private Optional<Function<HapiApiSpec, Long>> balanceFn = Optional.empty();


	@Override
	public HederaFunctionality type() {
		return HederaFunctionality.CryptoCreate;
	}

	@Override
	protected Key lookupKey(HapiApiSpec spec, String name) {
		return name.equals(account) ? key : spec.registry().getKey(name);
	}

	public HapiCryptoCreate advertisingCreation() {
		advertiseCreation = true;
		return this;
	}

	public HapiCryptoCreate(String account) {
		this.account = account;
	}

	public HapiCryptoCreate entityMemo(String memo) {
		entityMemo = Optional.of(memo);
		return this;
	}

	public HapiCryptoCreate sendThreshold(Long amount) {
		sendThresh = Optional.of(amount);
		return this;
	}
	public HapiCryptoCreate autoRenewSecs(long time) {
		autoRenewDurationSecs = Optional.of(time);
		return this;
	}
	public HapiCryptoCreate receiveThreshold(Long amount) {
		receiveThresh = Optional.of(amount);
		return this;
	}
	public HapiCryptoCreate receiverSigRequired(boolean isRequired) {
		receiverSigRequired = Optional.of(isRequired);
		return this;
	}
	public HapiCryptoCreate balance(Long amount) {
		initialBalance = Optional.of(amount);
		return this;
	}
	public HapiCryptoCreate balance(Function<HapiApiSpec, Long> fn) {
		balanceFn = Optional.of(fn);
		return this;
	}
	public HapiCryptoCreate key(String name) {
		keyName = Optional.of(name);
		return this;
	}
	public HapiCryptoCreate keyType(KeyType type) {
		keyType = Optional.of(type);
		return this;
	}
	public HapiCryptoCreate withRecharging() {
		recharging = true;
		return this;
	}

	public HapiCryptoCreate rechargeWindow(int window) {
		rechargeWindow = Optional.of(window);
		return this;
	}

	public HapiCryptoCreate keyShape(SigControl controller) {
		keyShape = Optional.of(controller);
		return this;
	}
	public HapiCryptoCreate proxy(String idLit) {
		proxy = Optional.of(HapiPropertySource.asAccount(idLit));
		return this;
	}

	@Override
	protected HapiCryptoCreate self() {
		return this;
	}

	@Override
	protected long feeFor(HapiApiSpec spec, Transaction txn, int numPayerKeys) throws Throwable {
		return spec.fees().forActivityBasedOp(
				HederaFunctionality.CryptoCreate, cryptoFees::getCryptoCreateTxFeeMatrices, txn, numPayerKeys);
	}

	@Override
	protected Consumer<TransactionBody.Builder> opBodyDef(HapiApiSpec spec) throws Throwable {
		key = netOf(spec, keyName, keyShape, keyType, Optional.of(this::effectiveKeyGen));
		long amount = balanceFn.map(fn -> fn.apply(spec)).orElse(initialBalance.orElse(-1L));
		initialBalance = (amount >= 0) ? Optional.of(amount) : Optional.empty();
		CryptoCreateTransactionBody opBody = spec
				.txns()
				.<CryptoCreateTransactionBody, CryptoCreateTransactionBody.Builder>body(
						CryptoCreateTransactionBody.class, b -> {
							b.setKey(key);
							proxy.ifPresent(p -> b.setProxyAccountID(p));
							entityMemo.ifPresent(m -> b.setMemo(m));
							sendThresh.ifPresent(a -> b.setSendRecordThreshold(a));
							receiveThresh.ifPresent(a -> b.setReceiveRecordThreshold(a));
							initialBalance.ifPresent(a -> b.setInitialBalance(a));
							receiverSigRequired.ifPresent(r -> b.setReceiverSigRequired(r));
							autoRenewDurationSecs.ifPresent(s -> b.setAutoRenewPeriod(Duration.newBuilder().setSeconds(s).build()));
						});
		return b -> b.setCryptoCreateAccount(opBody);
	}

	@Override
	protected List<Function<HapiApiSpec, Key>> defaultSigners() {
		return Arrays.asList(
				spec -> spec.registry().getKey(effectivePayer(spec)),
				ignore -> key);
	}

	@Override
	protected Function<Transaction, TransactionResponse> callToUse(HapiApiSpec spec) {
		return spec.clients().getCryptoSvcStub(targetNodeFor(spec), useTls)::createAccount;
	}

	@Override
	protected void updateStateOf(HapiApiSpec spec) {
		if (actualStatus != SUCCESS) {
			return;
		}
		if (recharging) {
			spec.registry().setRecharging(account, initialBalance.orElse(spec.setup().defaultBalance()));
			if(rechargeWindow.isPresent()){
				spec.registry().setRechargingWindow(account, rechargeWindow.get());
			}
		}
		spec.registry().saveKey(account, key);
		spec.registry().saveAccountId(account, lastReceipt.getAccountID());
		receiverSigRequired.ifPresent(r -> spec.registry().saveSigRequirement(account, r));

		if (advertiseCreation) {
			String banner = "\n\n" + bannerWith(
					String.format(
							"Created account '%s' with id '0.0.%d'.",
							account,
							lastReceipt.getAccountID().getAccountNum()));
			log.info(banner);
		}
	}

	@Override
	protected MoreObjects.ToStringHelper toStringHelper() {
		MoreObjects.ToStringHelper helper = super.toStringHelper()
				.add("account", account);
		keyType.ifPresent(kt -> helper.add("keyType", kt));
		initialBalance.ifPresent(b -> helper.add("balance", b));
		Optional
				.ofNullable(lastReceipt)
				.ifPresent(receipt -> {
					if (receipt.getAccountID().getAccountNum() != 0) {
						helper.add("created", receipt.getAccountID().getAccountNum());
					}
				});
		return helper;
	}

	public long numOfCreatedAccount() {
		return Optional
				.ofNullable(lastReceipt)
				.map(receipt -> receipt.getAccountID().getAccountNum())
				.orElse(-1L);
	}
}
