package com.hedera.services.stream;

import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionRecord;
import com.swirlds.common.crypto.AbstractSerializableHashable;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import com.swirlds.common.stream.Timestamped;

import java.io.IOException;
import java.time.Instant;

/**
 * Contains a TransactionRecord, its related Transaction, and consensus Timestamp of the Transaction
 */
public class RecordStreamObject extends AbstractSerializableHashable implements Timestamped {
	public static final long CLASS_ID = 0xe370929ba5429d8bL;
	public static final int CLASS_VERSION = 1;

	//TODO: confirm the max length;
	private static final int MAX_RECORD_LENGTH = 64 * 1024;
	private static final int MAX_TRANSACTION_LENGTH = 64 * 1024;

	private TransactionRecord transactionRecord;
	private Transaction transaction;
	private Instant consensusTimestamp;

	public RecordStreamObject(final TransactionRecord transactionRecord,
			final Transaction transaction, final Instant consensusTimestamp) {
		this.transactionRecord = transactionRecord;
		this.transaction = transaction;
		this.consensusTimestamp = consensusTimestamp;
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeByteArray(transactionRecord.toByteArray());
		out.writeByteArray(transaction.toByteArray());
		out.writeInstant(consensusTimestamp);
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		transactionRecord = TransactionRecord.parseFrom(in.readByteArray(MAX_RECORD_LENGTH));
		transaction = Transaction.parseFrom(in.readByteArray(MAX_TRANSACTION_LENGTH));
		consensusTimestamp = in.readInstant();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getClassId() {
		return CLASS_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getVersion() {
		return CLASS_VERSION;
	}

	@Override
	public Instant getTimestamp() {return consensusTimestamp; }
}
