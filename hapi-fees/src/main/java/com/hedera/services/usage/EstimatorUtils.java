package com.hedera.services.usage;

/*-
 * ‌
 * Hedera Services API Fees
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

import com.hederahashgraph.api.proto.java.FeeComponents;
import com.hederahashgraph.api.proto.java.FeeData;
import com.hederahashgraph.api.proto.java.TransactionBody;

import static com.hederahashgraph.fee.FeeBuilder.BASIC_TX_BODY_SIZE;

public interface EstimatorUtils {
	default int baseBodyBytes(TransactionBody txn) {
		return BASIC_TX_BODY_SIZE + txn.getMemoBytes().size();
	}

	default long nonDegenerateDiv(long dividend, int divisor) {
		return (dividend == 0) ? 0 : Math.max(1, dividend / divisor);
	}

	default long relativeLifetime(TransactionBody txn, long expiry) {
		long effectiveNow = txn.getTransactionID().getTransactionValidStart().getSeconds();
		return expiry - effectiveNow;
	}

	default long changeInBsUsage(long oldB, long oldLifetimeSecs, long newB, long newLifetimeSecs) {
		newLifetimeSecs = Math.max(oldLifetimeSecs, newLifetimeSecs);
		long oldBs = oldB * oldLifetimeSecs;
		long newBs = newB * newLifetimeSecs;
		return Math.max(0, newBs - oldBs);
	}

	long baseNetworkRbs();
	FeeData withDefaultTxnPartitioning(FeeComponents usage, long networkRbh, int numPayerKeys);
	FeeData withDefaultQueryPartitioning(FeeComponents usage);
	UsageEstimate baseEstimate(TransactionBody txn, SigUsage sigUsage);
}
