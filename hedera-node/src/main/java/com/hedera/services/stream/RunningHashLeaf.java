package com.hedera.services.stream;

import com.swirlds.common.crypto.Hash;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import com.swirlds.common.merkle.utility.AbstractMerkleLeaf;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * Contains a runningHash of all {@link RecordStreamObject}
 */
public class RunningHashLeaf extends AbstractMerkleLeaf {
	public static final long CLASS_ID = 0xe370929ba5429d9bL;
	public static final int CLASS_VERSION = 1;
	/**
	 * a runningHash of all RecordStreamObject
	 */
	private Hash runningRecordStreamHash;

	public RunningHashLeaf(final Hash runningRecordStreamHash) {
		this.runningRecordStreamHash = runningRecordStreamHash;
	}

	private RunningHashLeaf(final RunningHashLeaf runningHashLeaf) {
		this.runningRecordStreamHash = runningHashLeaf.runningRecordStreamHash;
		setImmutable(false);
		runningHashLeaf.setImmutable(true);
		setHash(runningHashLeaf.getHash());
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeSerializable(runningRecordStreamHash, true);
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		runningRecordStreamHash = in.readSerializable();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDataExternal() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RunningHashLeaf that = (RunningHashLeaf) o;
		return new EqualsBuilder().append(this.runningRecordStreamHash, that.runningRecordStreamHash).isEquals();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(runningRecordStreamHash);
	}

	public RunningHashLeaf copy() {
		// throwIfImmutable();
		return new RunningHashLeaf(this);
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
}
