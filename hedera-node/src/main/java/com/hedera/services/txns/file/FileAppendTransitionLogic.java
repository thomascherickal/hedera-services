package com.hedera.services.txns.file;

/*-
 * ‌
 * Hedera Services Node
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

import com.hedera.services.context.TransactionContext;
import com.hedera.services.files.HederaFs;
import com.hedera.services.txns.TransitionLogic;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.TransactionBody;
import com.hedera.services.files.HFileMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.hedera.services.txns.file.FileUpdateTransitionLogic.mapToStatus;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.FILE_DELETED;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_FILE_ID;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.UNAUTHORIZED;

public class FileAppendTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(FileAppendTransitionLogic.class);

	private static final Function<TransactionBody, ResponseCodeEnum> SYNTAX_RUBBER_STAMP = ignore -> OK;

	private final HederaFs hfs;
	private final TransactionContext txnCtx;

	public FileAppendTransitionLogic(HederaFs hfs, TransactionContext txnCtx) {
		this.hfs = hfs;
		this.txnCtx = txnCtx;
	}

	@Override
	public void doStateTransition() {
		var op = txnCtx.accessor().getTxn().getFileAppend();

		try {
			var target = op.getFileID();
			var data = op.getContents().toByteArray();

			Optional<HFileMeta> attr = hfs.exists(target) ? Optional.of(hfs.getattr(target)) : Optional.empty();
			var validity = classify(attr);
			if (validity != OK) {
				txnCtx.setStatus(validity);
				return;
			}

			var result = hfs.append(target, data);
			txnCtx.setStatus(result.outcome());
		} catch (IllegalArgumentException iae) {
			mapToStatus(iae, txnCtx);
		} catch (Exception unknown) {
			log.warn("Unrecognized failure handling {}!", txnCtx.accessor().getSignedTxn4Log(), unknown);
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	private ResponseCodeEnum classify(Optional<HFileMeta> attr) {
		if (attr.isEmpty()) {
			return INVALID_FILE_ID;
		} else {
			var info = attr.get();
			if (info.isDeleted()) {
				return FILE_DELETED;
			} else if (info.getWacl().isEmpty()) {
				return UNAUTHORIZED;
			} else {
				return OK;
			}
		}
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasFileAppend;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_RUBBER_STAMP;
	}
}
