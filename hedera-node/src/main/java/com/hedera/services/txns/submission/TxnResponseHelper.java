package com.hedera.services.txns.submission;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
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

import com.hedera.services.context.domain.trackers.IssEventInfo;
import com.hedera.services.legacy.exception.OnGoingISSException;
import com.hedera.services.queries.answering.QueryResponseHelper;
import com.hedera.services.txns.SubmissionFlow;
import com.hedera.services.utils.SignedTxnAccessor;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionResponse;
import com.hedera.services.legacy.services.stats.HederaNodeStats;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hedera.services.context.domain.trackers.IssEventStatus.ONGOING_ISS;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.PLATFORM_NOT_ACTIVE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;

public class TxnResponseHelper {
	private static final Logger log = LogManager.getLogger(QueryResponseHelper.class);

	static final TransactionResponse FAIL_INVALID_RESPONSE = TransactionResponse.newBuilder()
			.setNodeTransactionPrecheckCode(FAIL_INVALID)
			.build();

	static final TransactionResponse ONGOING_ISS_EXCEPTION_RESPONSE = TransactionResponse.newBuilder()
			.setNodeTransactionPrecheckCode(PLATFORM_NOT_ACTIVE)
			.build();

	private final SubmissionFlow submissionFlow;
	private final HederaNodeStats stats;
	private final IssEventInfo issEventInfo;

	public TxnResponseHelper(SubmissionFlow submissionFlow, HederaNodeStats stats, IssEventInfo issEventInfo) {
		this.stats = stats;
		this.submissionFlow = submissionFlow;
		this.issEventInfo = issEventInfo;
	}

	public void respondToHcs(
			Transaction signedTxn,
			StreamObserver<TransactionResponse> observer,
			String metric
	) {
		respondWithMetrics(
				signedTxn,
				observer,
				() -> stats.hcsTxnReceived(metric),
				() -> stats.hcsTxnSubmitted(metric));
	}

	public void respondToFile(
			Transaction signedTxn,
			StreamObserver<TransactionResponse> observer,
			String metric
	) {
		respondWithMetrics(
				signedTxn,
				observer,
				() -> stats.fileTransactionReceived(metric),
				() -> stats.fileTransactionSubmitted(metric));
	}

	public void respondToCrypto(
			Transaction signedTxn,
			StreamObserver<TransactionResponse> observer,
			String metric
	) {
		respondWithMetrics(
				signedTxn,
				observer,
				() -> stats.cryptoTransactionReceived(metric),
				() -> stats.cryptoTransactionSubmitted(metric));
	}

	private void respondWithMetrics(
			Transaction signedTxn,
			StreamObserver<TransactionResponse> observer,
			Runnable incReceivedCount,
			Runnable incSubmittedCount
	) {
		incReceivedCount.run();
		TransactionResponse response;

		try {
			if(issEventInfo.status() == ONGOING_ISS) {
				throw new OnGoingISSException("On Going ISS on the Platform");
			} else {
				response = submissionFlow.submit(signedTxn);
			}
		}
		catch (OnGoingISSException e){
			SignedTxnAccessor accessor = SignedTxnAccessor.uncheckedFrom(signedTxn);
			log.warn("Submission flow unable to submit {}! because of {}", accessor.getSignedTxn4Log(), e.getMessage());
			response = ONGOING_ISS_EXCEPTION_RESPONSE;
		}
		catch (Exception surprising) {
			SignedTxnAccessor accessor = SignedTxnAccessor.uncheckedFrom(signedTxn);
			log.warn("Submission flow unable to submit {}!", accessor.getSignedTxn4Log(), surprising);
			response = FAIL_INVALID_RESPONSE;
		}

		observer.onNext(response);
		observer.onCompleted();

		if (response.getNodeTransactionPrecheckCode() == OK) {
			incSubmittedCount.run();
		}
	}
}
