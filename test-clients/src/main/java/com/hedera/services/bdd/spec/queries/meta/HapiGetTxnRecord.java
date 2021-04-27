package com.hedera.services.bdd.spec.queries.meta;

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
import com.google.protobuf.ByteString;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.assertions.ErroringAsserts;
import com.hedera.services.bdd.spec.assertions.ErroringAssertsProvider;
import com.hedera.services.bdd.spec.assertions.TransactionRecordAsserts;
import com.hedera.services.bdd.spec.queries.HapiQueryOp;
import com.hedera.services.bdd.spec.transactions.TxnUtils;
import com.hedera.services.legacy.proto.utils.CommonUtils;
import com.hederahashgraph.api.proto.java.HederaFunctionality;
import com.hederahashgraph.api.proto.java.Query;
import com.hederahashgraph.api.proto.java.Response;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionGetRecordQuery;
import com.hederahashgraph.api.proto.java.TransactionID;
import com.hederahashgraph.api.proto.java.TransactionRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

import static com.hedera.services.bdd.spec.assertions.AssertUtils.rethrowSummaryError;
import static com.hedera.services.bdd.spec.queries.QueryUtils.answerCostHeader;
import static com.hedera.services.bdd.spec.queries.QueryUtils.answerHeader;
import static com.hedera.services.bdd.suites.crypto.CryptoTransferSuite.sdec;
import static com.hedera.services.bdd.spec.transactions.schedule.HapiScheduleCreate.correspondingScheduledTxnId;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;
import static org.junit.Assert.assertArrayEquals;

public class HapiGetTxnRecord extends HapiQueryOp<HapiGetTxnRecord> {
	private static final Logger log = LogManager.getLogger(HapiGetTxnRecord.class);

	private static final TransactionID defaultTxnId = TransactionID.getDefaultInstance();

	String txn;
	boolean scheduled = false;
	boolean assertNothing = false;
	boolean useDefaultTxnId = false;
	boolean requestDuplicates = false;
	boolean shouldBeTransferFree = false;
	boolean assertOnlyPriority = false;
	boolean assertNothingAboutHashes = false;
	boolean lookupScheduledFromRegistryId = false;
	Optional<TransactionID> explicitTxnId = Optional.empty();
	Optional<TransactionRecordAsserts> priorityExpectations = Optional.empty();
	Optional<BiConsumer<TransactionRecord, Logger>> format = Optional.empty();
	Optional<String> creationName = Optional.empty();
	Optional<String> saveTxnRecordToRegistry = Optional.empty();
	Optional<String> registryEntry = Optional.empty();
	Optional<String> topicToValidate = Optional.empty();
	Optional<byte[]> lastMessagedSubmitted = Optional.empty();
	Optional<LongConsumer> priceConsumer = Optional.empty();
	private Optional<ErroringAssertsProvider<List<TransactionRecord>>> duplicateExpectations = Optional.empty();

	public HapiGetTxnRecord(String txn) {
		this.txn = txn;
	}
	public HapiGetTxnRecord(TransactionID txnId) {
		this.explicitTxnId = Optional.of(txnId);
	}

	@Override
	public HederaFunctionality type() {
		return HederaFunctionality.TransactionGetRecord;
	}

	@Override
	protected HapiGetTxnRecord self() {
		return this;
	}

	public HapiGetTxnRecord scheduled() {
		scheduled = true;
		return this;
	}

	public HapiGetTxnRecord assertingOnlyPriority() {
		assertOnlyPriority = true;
		return this;
	}

	public HapiGetTxnRecord scheduledBy(String creation) {
		scheduled = true;
		creationName = Optional.of(creation);
		lookupScheduledFromRegistryId = true;
		return this;
	}

	public HapiGetTxnRecord andAnyDuplicates() {
		requestDuplicates = true;
		return this;
	}

	public HapiGetTxnRecord assertingNothingAboutHashes() {
		assertNothingAboutHashes = true;
		return this;
	}

	public HapiGetTxnRecord assertingNothing() {
		assertNothing = true;
		return this;
	}

	public HapiGetTxnRecord providingFeeTo(LongConsumer priceConsumer) {
		this.priceConsumer = Optional.of(priceConsumer);
		return this;
	}

	public HapiGetTxnRecord showsNoTransfers() {
		shouldBeTransferFree = true;
		return this;
	}

	public HapiGetTxnRecord saveCreatedContractListToRegistry(String registryEntry) {
		this.registryEntry = Optional.of(registryEntry);
		return this;
	}

	public HapiGetTxnRecord saveTxnRecordToRegistry(String txnRecordEntry) {
		this.saveTxnRecordToRegistry = Optional.of(txnRecordEntry);
		return this;
	}

	public HapiGetTxnRecord useDefaultTxnId() {
		useDefaultTxnId = true;
		return this;
	}

	public HapiGetTxnRecord hasPriority(TransactionRecordAsserts provider) {
		priorityExpectations = Optional.of(provider);
		return this;
	}

	public HapiGetTxnRecord hasDuplicates(ErroringAssertsProvider<List<TransactionRecord>> provider) {
		duplicateExpectations = Optional.of(provider);
		return this;
	}

	public HapiGetTxnRecord hasCorrectRunningHash(String topic, byte[] lastMessage) {
		topicToValidate = Optional.of(topic);
		lastMessagedSubmitted = Optional.of(lastMessage);
		return this;
	}

	public HapiGetTxnRecord hasCorrectRunningHash(String topic, String lastMessage) {
		hasCorrectRunningHash(topic, lastMessage.getBytes());
		return this;
	}

	public HapiGetTxnRecord loggedWith(BiConsumer<TransactionRecord, Logger> customFormat) {
		super.logged();
		format = Optional.of(customFormat);
		return this;
	}

	public TransactionRecord getResponseRecord() {
		return response.getTransactionGetRecord().getTransactionRecord();
	}

	private void assertPriority(HapiApiSpec spec, TransactionRecord actualRecord) throws Throwable {
		if (priorityExpectations.isPresent()) {
			ErroringAsserts<TransactionRecord> asserts = priorityExpectations.get().assertsFor(spec);
			List<Throwable> errors = asserts.errorsIn(actualRecord);
			rethrowSummaryError(log, "Bad priority record!", errors);
		}
	}

	private void assertDuplicates(HapiApiSpec spec) throws Throwable {
		if (duplicateExpectations.isPresent()) {
			var asserts = duplicateExpectations.get().assertsFor(spec);
			var errors = asserts.errorsIn(response.getTransactionGetRecord().getDuplicateTransactionRecordsList());
			rethrowSummaryError(log, "Bad duplicate records!", errors);
		}
	}

	private void assertTransactionHash(HapiApiSpec spec, TransactionRecord actualRecord) throws Throwable {
		Transaction transaction = Transaction.parseFrom(spec.registry().getBytes(txn));
		assertArrayEquals("Bad transaction hash!", CommonUtils.sha384HashOf(transaction).toByteArray(),
				actualRecord.getTransactionHash().toByteArray());
	}

	private void assertTopicRunningHash(HapiApiSpec spec, TransactionRecord actualRecord) throws Throwable {
		if (topicToValidate.isPresent()) {
			if (actualRecord.getReceipt().getStatus().equals(ResponseCodeEnum.SUCCESS)) {
				var previousRunningHash = spec.registry().getBytes(topicToValidate.get());
				var payer = actualRecord.getTransactionID().getAccountID();
				var topicId = TxnUtils.asTopicId(topicToValidate.get(), spec);
				var boas = new ByteArrayOutputStream();
				try (var out = new ObjectOutputStream(boas)) {
					out.writeObject(previousRunningHash);
					out.writeLong(spec.setup().defaultTopicRunningHashVersion());
					out.writeLong(payer.getShardNum());
					out.writeLong(payer.getRealmNum());
					out.writeLong(payer.getAccountNum());
					out.writeLong(topicId.getShardNum());
					out.writeLong(topicId.getRealmNum());
					out.writeLong(topicId.getTopicNum());
					out.writeLong(actualRecord.getConsensusTimestamp().getSeconds());
					out.writeInt(actualRecord.getConsensusTimestamp().getNanos());
					out.writeLong(actualRecord.getReceipt().getTopicSequenceNumber());
					out.writeObject(CommonUtils.noThrowSha384HashOf(lastMessagedSubmitted.get()));
					out.flush();
					var expectedRunningHash = CommonUtils.noThrowSha384HashOf(boas.toByteArray());
					var actualRunningHash = actualRecord.getReceipt().getTopicRunningHash();
					assertArrayEquals("Bad running hash!", expectedRunningHash,
							actualRunningHash.toByteArray());
					spec.registry().saveBytes(topicToValidate.get(), actualRunningHash);
				}
			} else {
				if (verboseLoggingOn) {
					log.warn("Cannot validate running hash for an unsuccessful submit message transaction!");
				}
			}
		}
	}

	@Override
	protected void assertExpectationsGiven(HapiApiSpec spec) throws Throwable {
		if (assertNothing) {
			return;
		}
		TransactionRecord actualRecord = response.getTransactionGetRecord().getTransactionRecord();
		assertPriority(spec, actualRecord);
		if (scheduled || assertOnlyPriority) {
			return;
		}
		assertDuplicates(spec);
		if (!assertNothingAboutHashes) {
			assertTransactionHash(spec, actualRecord);
			assertTopicRunningHash(spec, actualRecord);
		}
		if (shouldBeTransferFree) {
			Assert.assertEquals(
					"Unexpected transfer list!",
					0,
					actualRecord.getTokenTransferListsCount());
		}
	}

	@Override
	protected void submitWith(HapiApiSpec spec, Transaction payment) {
		Query query = getRecordQuery(spec, payment, false);
		response = spec.clients().getCryptoSvcStub(targetNodeFor(spec), useTls).getTxRecordByTxID(query);
		TransactionRecord record = response.getTransactionGetRecord().getTransactionRecord();
		if (verboseLoggingOn) {
			if (format.isPresent()) {
				format.get().accept(record, log);
			} else {
				var fee = record.getTransactionFee();
				var rates = spec.ratesProvider();
				var priceInUsd = sdec(rates.toUsdWithActiveRates(fee), 4);
				log.info("Record (charged ${}): {}", priceInUsd,  record);
			}
		}
		if (response.getTransactionGetRecord().getHeader().getNodeTransactionPrecheckCode() == OK) {
			priceConsumer.ifPresent(pc -> pc.accept(record.getTransactionFee()));
		}
		if (registryEntry.isPresent()) {
			spec.registry().saveContractList(
					registryEntry.get() + "CreateResult",
					record.getContractCreateResult().getCreatedContractIDsList());
			spec.registry().saveContractList(
					registryEntry.get() + "CallResult",
					record.getContractCallResult().getCreatedContractIDsList());
		}
		if (saveTxnRecordToRegistry.isPresent()) {
			spec.registry().saveTransactionRecord(saveTxnRecordToRegistry.get(), record);
		}
	}

	@Override
	protected long lookupCostWith(HapiApiSpec spec, Transaction payment) throws Throwable {
		Query query = getRecordQuery(spec, payment, true);
		Response response = spec.clients().getCryptoSvcStub(targetNodeFor(spec), useTls).getTxRecordByTxID(query);
		return costFrom(response);
	}

	private Query getRecordQuery(HapiApiSpec spec, Transaction payment, boolean costOnly) {
		TransactionID txnId = useDefaultTxnId
				? defaultTxnId
				: explicitTxnId.orElseGet(() -> spec.registry().getTxnId(txn));
		if (lookupScheduledFromRegistryId) {
			txnId = spec.registry().getTxnId(correspondingScheduledTxnId(creationName.get()));
		} else {
			if (scheduled) {
				txnId = txnId.toBuilder()
						.setScheduled(true)
						.build();
			}
		}
		TransactionGetRecordQuery getRecordQuery = TransactionGetRecordQuery.newBuilder()
				.setHeader(costOnly ? answerCostHeader(payment) : answerHeader(payment))
				.setTransactionID(txnId)
				.setIncludeDuplicates(requestDuplicates)
				.build();
		return Query.newBuilder().setTransactionGetRecord(getRecordQuery).build();
	}

	@Override
	protected long costOnlyNodePayment(HapiApiSpec spec) throws Throwable {
		return spec.fees().forOp(
				HederaFunctionality.TransactionGetRecord,
				cryptoFees.getCostTransactionRecordQueryFeeMatrices());
	}

	@Override
	protected boolean needsPayment() {
		return true;
	}

	@Override
	protected MoreObjects.ToStringHelper toStringHelper() {
		if (explicitTxnId.isPresent()) {
			return super.toStringHelper().add("explicitTxnId", true);
		} else {
			return super.toStringHelper().add("txn", txn);
		}
	}
}
