package com.hedera.services.stream;

import com.google.protobuf.ByteString;
import com.hederahashgraph.api.proto.java.Timestamp;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionRecord;
import com.swirlds.common.constructable.ClassConstructorPair;
import com.swirlds.common.constructable.ConstructableRegistry;
import com.swirlds.common.constructable.ConstructableRegistryException;
import com.swirlds.common.io.SelfSerializable;
import com.swirlds.common.merkle.io.MerkleDataInputStream;
import com.swirlds.common.merkle.io.MerkleDataOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecordStreamObjectTest {

	private static Transaction transaction;
	private static TransactionRecord record;
	private static Instant consensusTimestamp;

	@BeforeAll
	public static void init() {
		// register constructor for RecordStreamObject
		try {
			ConstructableRegistry.registerConstructable(new ClassConstructorPair(
					RecordStreamObject.class, RecordStreamObject::new));
		} catch (Throwable ex) {
			ex.printStackTrace();
		}

		byte[] bytes = new byte[48];
		ThreadLocalRandom.current().nextBytes(bytes);
		transaction = Transaction.newBuilder().setBodyBytes(ByteString.copyFrom(bytes)).build();
		record = TransactionRecord.newBuilder().setConsensusTimestamp(
				Timestamp.newBuilder().getDefaultInstanceForType()).build();
		consensusTimestamp = Instant.now();
	}

	@Test
	public void serializeAndDeserializeTest() throws IOException {
		RecordStreamObject recordStreamObject = new RecordStreamObject(record, transaction, consensusTimestamp);
		assertTrue(checkSerializeDeserializeEqual(recordStreamObject));
	}

	private boolean checkSerializeDeserializeEqual(final SelfSerializable selfSerializable) throws IOException {
		final boolean abbreviated = false;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		MerkleDataOutputStream outputStream = new MerkleDataOutputStream(byteArrayOutputStream, abbreviated);
		outputStream.writeSerializable(selfSerializable, true);
		outputStream.flush();
		MerkleDataInputStream inStream = new MerkleDataInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), abbreviated);
		SelfSerializable deserialized = inStream.readSerializable();
		return deserialized.equals(selfSerializable);
	}
}
