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
public class HashLeaf extends AbstractMerkleLeaf {
	public static final long CLASS_ID = 0xe370929ba5429d9bL;
	public static final int CLASS_VERSION = 1;

	private Hash runningRecordsHash;

	public HashLeaf(final Hash runningRecordsHash) {
		this.runningRecordsHash = runningRecordsHash;
	}

	private HashLeaf(final HashLeaf hashLeaf) {
		this.runningRecordsHash = hashLeaf.runningRecordsHash;
		setImmutable(false);
		hashLeaf.setImmutable(true);
		setHash(hashLeaf.getHash());
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeSerializable(runningRecordsHash, true);
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
		runningRecordsHash = in.readSerializable();
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
		HashLeaf that = (HashLeaf) o;
		return new EqualsBuilder().append(this.runningRecordsHash, that.runningRecordsHash).isEquals();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(runningRecordsHash);
	}

	public HashLeaf copy() {
		// throwIfImmutable();
		return new HashLeaf(this);
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
