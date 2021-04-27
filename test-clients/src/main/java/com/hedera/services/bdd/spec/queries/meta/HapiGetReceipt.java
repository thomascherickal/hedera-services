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
import com.hedera.services.bdd.spec.transactions.TxnUtils;
import com.hedera.services.bdd.spec.transactions.schedule.HapiScheduleCreate;
import com.hederahashgraph.api.proto.java.HederaFunctionality;
import com.hederahashgraph.api.proto.java.Query;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionID;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.queries.HapiQueryOp;
import com.hederahashgraph.api.proto.java.TransactionReceipt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import java.util.Optional;

import static com.hedera.services.bdd.spec.queries.QueryUtils.txnReceiptQueryFor;

public class HapiGetReceipt extends HapiQueryOp<HapiGetReceipt> {
	static final Logger log = LogManager.getLogger(HapiGetReceipt.class);

	String txn;
	boolean forgetOp = false;
	boolean requestDuplicates = false;
	boolean useDefaultTxnId = false;
	TransactionID defaultTxnId = TransactionID.getDefaultInstance();
	Optional<String> expectedSchedule = Optional.empty();
	Optional<String> expectedScheduledTxnId = Optional.empty();
	Optional<TransactionID> explicitTxnId = Optional.empty();
	Optional<ResponseCodeEnum> expectedPriorityStatus = Optional.empty();
	Optional<ResponseCodeEnum[]> expectedDuplicateStatuses = Optional.empty();

	@Override
	public HederaFunctionality type() {
		return HederaFunctionality.TransactionGetReceipt;
	}

	@Override
	protected HapiGetReceipt self() {
		return this;
	}

	public HapiGetReceipt(String txn) {
		this.txn = txn;
	}
	public HapiGetReceipt(TransactionID txnId) {
		explicitTxnId = Optional.of(txnId);
	}

	public HapiGetReceipt forgetOp() {
		forgetOp = true;
		return this;
	}

	public HapiGetReceipt andAnyDuplicates() {
		requestDuplicates = true;
		return this;
	}

	public HapiGetReceipt useDefaultTxnId() {
		useDefaultTxnId = true;
		return this;
	}

	public HapiGetReceipt hasPriorityStatus(ResponseCodeEnum status) {
		expectedPriorityStatus = Optional.of(status);
		return this;
	}

	public HapiGetReceipt hasDuplicateStatuses(ResponseCodeEnum... statuses) {
		expectedDuplicateStatuses = Optional.of(statuses);
		return this;
	}

	public HapiGetReceipt hasScheduledTxnId(String name) {
		expectedScheduledTxnId = Optional.of(HapiScheduleCreate.correspondingScheduledTxnId(name));
		return this;
	}

	public HapiGetReceipt hasSchedule(String name) {
		expectedSchedule = Optional.of(name);
		return this;
	}

	@Override
	protected void submitWith(HapiApiSpec spec, Transaction payment) {
		TransactionID txnId = explicitTxnId.orElseGet(() ->
				useDefaultTxnId ? defaultTxnId : spec.registry().getTxnId(txn));
		Query query = forgetOp
				? Query.newBuilder().build()
				: txnReceiptQueryFor(txnId, requestDuplicates);
		response = spec.clients().getCryptoSvcStub(targetNodeFor(spec), useTls).getTransactionReceipts(query);
		if (verboseLoggingOn) {
			log.info("Receipt: " + response.getTransactionGetReceipt().getReceipt());
		}
	}

	@Override
	protected void assertExpectationsGiven(HapiApiSpec spec) {
		var receipt = response.getTransactionGetReceipt().getReceipt();
		if (expectedPriorityStatus.isPresent()) {
			ResponseCodeEnum actualStatus = receipt.getStatus();
			Assert.assertEquals(expectedPriorityStatus.get(), actualStatus);
		}
		if (expectedDuplicateStatuses.isPresent()) {
			var duplicates = response.getTransactionGetReceipt().getDuplicateTransactionReceiptsList()
					.stream()
					.map(TransactionReceipt::getStatus)
					.toArray(n -> new ResponseCodeEnum[n]);
			Assert.assertArrayEquals(expectedDuplicateStatuses.get(), duplicates);
		}
		if (expectedScheduledTxnId.isPresent()) {
			var expected = spec.registry().getTxnId(expectedScheduledTxnId.get());
			var actual = response.getTransactionGetReceipt().getReceipt().getScheduledTransactionID();
			Assert.assertEquals("Wrong scheduled transaction id!", expected, actual);
		}
		if (expectedSchedule.isPresent()) {
			var schedule = TxnUtils.asScheduleId(expectedSchedule.get(), spec);
			Assert.assertEquals("Wrong/missing schedule id!", schedule, receipt.getScheduleID());
		}
	}

	@Override
	protected boolean needsPayment() {
		return false;
	}
	@Override
	protected long costOnlyNodePayment(HapiApiSpec spec) { return 0L; }
	@Override
	protected long lookupCostWith(HapiApiSpec spec, Transaction payment) { return 0L; }

	@Override
	protected MoreObjects.ToStringHelper toStringHelper() {
		return super.toStringHelper()
				.add("txn", txn)
				.add("explicit Txn :", explicitTxnId);
	}
}
