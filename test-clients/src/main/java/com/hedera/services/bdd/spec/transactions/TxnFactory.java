package com.hedera.services.bdd.spec.transactions;

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

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.hederahashgraph.api.proto.java.ConsensusCreateTopicTransactionBody;
import com.hederahashgraph.api.proto.java.ConsensusDeleteTopicTransactionBody;
import com.hederahashgraph.api.proto.java.ConsensusSubmitMessageTransactionBody;
import com.hederahashgraph.api.proto.java.ConsensusUpdateTopicTransactionBody;
import com.hederahashgraph.api.proto.java.ContractCallTransactionBody;
import com.hederahashgraph.api.proto.java.ContractCreateTransactionBody;
import com.hederahashgraph.api.proto.java.ContractDeleteTransactionBody;
import com.hederahashgraph.api.proto.java.ContractUpdateTransactionBody;
import com.hederahashgraph.api.proto.java.CryptoCreateTransactionBody;
import com.hederahashgraph.api.proto.java.CryptoDeleteTransactionBody;
import com.hederahashgraph.api.proto.java.CryptoTransferTransactionBody;
import com.hederahashgraph.api.proto.java.CryptoUpdateTransactionBody;
import com.hederahashgraph.api.proto.java.FileAppendTransactionBody;
import com.hederahashgraph.api.proto.java.FileCreateTransactionBody;
import com.hederahashgraph.api.proto.java.FileDeleteTransactionBody;
import com.hederahashgraph.api.proto.java.FileUpdateTransactionBody;
import com.hederahashgraph.api.proto.java.FreezeTransactionBody;
import com.hederahashgraph.api.proto.java.ScheduleCreateTransactionBody;
import com.hederahashgraph.api.proto.java.ScheduleCreateTransactionBody;
import com.hederahashgraph.api.proto.java.ScheduleDeleteTransactionBody;
import com.hederahashgraph.api.proto.java.ScheduleSignTransactionBody;
import com.hederahashgraph.api.proto.java.SystemDeleteTransactionBody;
import com.hederahashgraph.api.proto.java.SystemUndeleteTransactionBody;
import com.hederahashgraph.api.proto.java.Timestamp;
import com.hederahashgraph.api.proto.java.TokenAssociateTransactionBody;
import com.hederahashgraph.api.proto.java.TokenBurnTransactionBody;
import com.hederahashgraph.api.proto.java.TokenCreateTransactionBody;
import com.hederahashgraph.api.proto.java.TokenDeleteTransactionBody;
import com.hederahashgraph.api.proto.java.TokenDissociateTransactionBody;
import com.hederahashgraph.api.proto.java.TokenFreezeAccountTransactionBody;
import com.hederahashgraph.api.proto.java.TokenGrantKycTransactionBody;
import com.hederahashgraph.api.proto.java.TokenRevokeKycTransactionBody;
import com.hederahashgraph.api.proto.java.TokenUpdateTransactionBody;
import com.hederahashgraph.api.proto.java.TokenMintTransactionBody;
import com.hederahashgraph.api.proto.java.TokenUnfreezeAccountTransactionBody;
import com.hederahashgraph.api.proto.java.TokenWipeAccountTransactionBody;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionBody;
import com.hederahashgraph.api.proto.java.TransactionID;
import com.hedera.services.bdd.spec.HapiSpecSetup;
import com.hedera.services.bdd.spec.keys.KeyFactory;
import com.hederahashgraph.api.proto.java.UncheckedSubmitBody;

import static com.hedera.services.bdd.spec.transactions.TxnUtils.*;
import static com.hedera.services.bdd.spec.HapiApiSpec.UTF8Mode.TRUE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TxnFactory {
	KeyFactory keys;
	HapiSpecSetup setup;

	private static final int BANNER_WIDTH = 80;
	private static final int BANNER_BOUNDARY_THICKNESS = 2;
	private static final double TXN_ID_SAMPLE_PROBABILITY = 1.0 / 500;

	AtomicReference<TransactionID> sampleTxnId = new AtomicReference<>(TransactionID.getDefaultInstance());
	SplittableRandom r = new SplittableRandom();

	public static String bannerWith(String... msgs) {
		var sb = new StringBuilder();
		var partial = IntStream.range(0, BANNER_BOUNDARY_THICKNESS).mapToObj(ignore -> "*").collect(joining());
		int printableWidth = BANNER_WIDTH - 2 * (partial.length() + 1);
		addFullBoundary(sb);
		List<String> allMsgs = Stream.concat(Stream.of(""), Stream.concat(Arrays.stream(msgs), Stream.of("")))
				.collect(toList());
		for (String msg : allMsgs) {
			int rightPaddingLen = printableWidth - msg.length();
			var rightPadding = IntStream.range(0, rightPaddingLen).mapToObj(ignore -> " ").collect(joining());
			sb.append(partial + " ")
					.append(msg)
					.append(rightPadding)
					.append(" " + partial)
					.append("\n");
		}
		addFullBoundary(sb);
		return sb.toString();
	}

	private static void addFullBoundary(StringBuilder sb) {
		var full = IntStream.range(0, BANNER_WIDTH).mapToObj(ignore -> "*").collect(joining());
		for (int i = 0; i < BANNER_BOUNDARY_THICKNESS; i++) {
			sb.append(full).append("\n");
		}
	}

	public TxnFactory(HapiSpecSetup setup, KeyFactory keys) {
		this.keys = keys;
		this.setup = setup;
	}

	public Transaction.Builder getReadyToSign(Consumer<TransactionBody.Builder> spec) {
		Consumer<TransactionBody.Builder> composedBodySpec = defaultBodySpec().andThen(spec);
		TransactionBody.Builder bodyBuilder = TransactionBody.newBuilder();
		composedBodySpec.accept(bodyBuilder);
		return Transaction.newBuilder().setBodyBytes(ByteString.copyFrom(bodyBuilder.build().toByteArray()));
	}

	public TransactionID sampleRecentTxnId() {
		return sampleTxnId.get();
	}

	public TransactionID defaultTransactionID() {
		return TransactionID.newBuilder()
				.setTransactionValidStart(getUniqueTimestampPlusSecs(setup.txnStartOffsetSecs()))
				.setAccountID(setup.defaultPayer()).build();
	}

	public Consumer<TransactionBody.Builder> defaultBodySpec() {
		TransactionID defaultTxnId = defaultTransactionID();
		if (r.nextDouble() < TXN_ID_SAMPLE_PROBABILITY) {
			sampleTxnId.set(defaultTxnId);
		}

		final var memoToUse = (setup.isMemoUTF8() == TRUE ) ? setup.defaultUTF8memo() : setup.defaultMemo();

		return builder -> builder
				.setTransactionID(defaultTxnId)
				.setMemo(memoToUse)
				.setTransactionFee(setup.defaultFee())
				.setTransactionValidDuration(setup.defaultValidDuration())
				.setNodeAccountID(setup.defaultNode());
	}

	public <T, B extends Message.Builder> T body(Class<T> tClass, Consumer<B> def) throws Throwable {
		Method newBuilder = tClass.getMethod("newBuilder");
		B opBuilder = (B)newBuilder.invoke(null);
		String defaultBodyMethod = String.format("defaultDef_%s", tClass.getSimpleName());
		Method defaultBody = this.getClass().getMethod(defaultBodyMethod);
		((Consumer<B>)defaultBody.invoke(this)).andThen(def).accept(opBuilder);
		return (T)opBuilder.build();
	}

	public Consumer<TokenAssociateTransactionBody.Builder> defaultDef_TokenAssociateTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenDissociateTransactionBody.Builder> defaultDef_TokenDissociateTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenWipeAccountTransactionBody.Builder> defaultDef_TokenWipeAccountTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenRevokeKycTransactionBody.Builder> defaultDef_TokenRevokeKycTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenGrantKycTransactionBody.Builder> defaultDef_TokenGrantKycTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenBurnTransactionBody.Builder> defaultDef_TokenBurnTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenMintTransactionBody.Builder> defaultDef_TokenMintTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenDeleteTransactionBody.Builder> defaultDef_TokenDeleteTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenFreezeAccountTransactionBody.Builder> defaultDef_TokenFreezeAccountTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenUnfreezeAccountTransactionBody.Builder> defaultDef_TokenUnfreezeAccountTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenUpdateTransactionBody.Builder> defaultDef_TokenUpdateTransactionBody() {
		return builder -> {};
	}

	public Consumer<TokenCreateTransactionBody.Builder> defaultDef_TokenCreateTransactionBody() {
		return builder -> {
			builder.setTreasury(setup.defaultPayer());
			builder.setDecimals(setup.defaultTokenDecimals());
			builder.setInitialSupply(setup.defaultTokenInitialSupply());
			builder.setSymbol(TxnUtils.randomUppercase(8));
			builder.setExpiry(defaultExpiry());
		};
	}

	public Consumer<UncheckedSubmitBody.Builder> defaultDef_UncheckedSubmitBody() {
		return builder -> {};
	}

	public Consumer<ConsensusSubmitMessageTransactionBody.Builder> defaultDef_ConsensusSubmitMessageTransactionBody() {
		return builder -> {
			builder.setMessage(ByteString.copyFrom(setup.defaultConsensusMessage().getBytes()));
		};
	}

	public Consumer<ConsensusUpdateTopicTransactionBody.Builder> defaultDef_ConsensusUpdateTopicTransactionBody() {
		return builder -> {};
	}

	public Consumer<ConsensusCreateTopicTransactionBody.Builder> defaultDef_ConsensusCreateTopicTransactionBody() {
		return builder -> {
			builder.setAutoRenewPeriod(setup.defaultAutoRenewPeriod());
		};
	}

	public Consumer<ConsensusDeleteTopicTransactionBody.Builder> defaultDef_ConsensusDeleteTopicTransactionBody() {
		return builder -> {};
	}

	public Consumer<FreezeTransactionBody.Builder> defaultDef_FreezeTransactionBody() {
		return builder -> {};
	}

	public Consumer<FileDeleteTransactionBody.Builder> defaultDef_FileDeleteTransactionBody() {
		return builder -> {};
	}

	public Consumer<ContractDeleteTransactionBody.Builder> defaultDef_ContractDeleteTransactionBody() {
		return builder -> {
			builder.setTransferAccountID(setup.defaultTransfer());
		};
	}

	public Consumer<ContractUpdateTransactionBody.Builder> defaultDef_ContractUpdateTransactionBody() {
		return builder -> {};
	}

	public Consumer<ContractCallTransactionBody.Builder> defaultDef_ContractCallTransactionBody() {
		return builder -> builder
				.setGas(setup.defaultCallGas());
	}

	public Consumer<ContractCreateTransactionBody.Builder> defaultDef_ContractCreateTransactionBody() {
		return builder -> builder
				.setAutoRenewPeriod(setup.defaultAutoRenewPeriod())
				.setGas(setup.defaultCreateGas())
				.setInitialBalance(setup.defaultContractBalance())
				.setMemo(setup.defaultMemo())
				.setShardID(setup.defaultShard())
				.setRealmID(setup.defaultRealm())
				.setProxyAccountID(setup.defaultProxy());
	}

	public Consumer<FileCreateTransactionBody.Builder> defaultDef_FileCreateTransactionBody() {
		return builder -> builder
				.setRealmID(setup.defaultRealm())
				.setShardID(setup.defaultShard())
				.setContents(ByteString.copyFrom(setup.defaultFileContents()))
				.setExpirationTime(defaultExpiry());
	}

	public Consumer<FileAppendTransactionBody.Builder> defaultDef_FileAppendTransactionBody() {
		return builder -> builder
				.setContents(ByteString.copyFrom(setup.defaultFileContents()));
	}

	public Consumer<FileUpdateTransactionBody.Builder> defaultDef_FileUpdateTransactionBody() {
		return builder -> {};
	}

	private Timestamp defaultExpiry() {
		return expiryGiven(setup.defaultExpirationSecs());
	}

	public static Timestamp expiryGiven(long lifetimeSecs) {
		Instant expiry = Instant.now(Clock.systemUTC()).plusSeconds(lifetimeSecs);
		return Timestamp.newBuilder().setSeconds(expiry.getEpochSecond()).setNanos(expiry.getNano()).build();
	}

	public Consumer<CryptoDeleteTransactionBody.Builder> defaultDef_CryptoDeleteTransactionBody() {
		return builder -> builder
				.setTransferAccountID(setup.defaultTransfer());
	}

	public Consumer<SystemDeleteTransactionBody.Builder> defaultDef_SystemDeleteTransactionBody() {
		return builder -> {};
	}

	public Consumer<SystemUndeleteTransactionBody.Builder> defaultDef_SystemUndeleteTransactionBody() {
		return builder -> {};
	}

	public Consumer<CryptoCreateTransactionBody.Builder> defaultDef_CryptoCreateTransactionBody() {
		return builder -> builder
				.setInitialBalance(setup.defaultBalance())
				.setAutoRenewPeriod(setup.defaultAutoRenewPeriod())
				.setReceiveRecordThreshold(setup.defaultReceiveThreshold())
				.setSendRecordThreshold(setup.defaultSendThreshold())
				.setProxyAccountID(setup.defaultProxy())
				.setReceiverSigRequired(setup.defaultReceiverSigRequired());
	}

	public Consumer<CryptoUpdateTransactionBody.Builder> defaultDef_CryptoUpdateTransactionBody() {
		return builder -> builder
				.setAutoRenewPeriod(setup.defaultAutoRenewPeriod());
	}

	public Consumer<CryptoTransferTransactionBody.Builder> defaultDef_CryptoTransferTransactionBody() {
		return builder -> {};
	}

	public Consumer<ScheduleCreateTransactionBody.Builder> defaultDef_ScheduleCreateTransactionBody() {
		return builder -> {};
	}

	public Consumer<ScheduleSignTransactionBody.Builder> defaultDef_ScheduleSignTransactionBody() {
		return builder -> {};
	}

	public Consumer<ScheduleDeleteTransactionBody.Builder> defaultDef_ScheduleDeleteTransactionBody() {
		return builder -> {
		};
	}
}
