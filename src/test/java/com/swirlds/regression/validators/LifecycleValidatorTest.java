/*
 * (c) 2016-2020 Swirlds, Inc.
 *
 * This software is the confidential and proprietary information of
 * Swirlds, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Swirlds.
 *
 * SWIRLDS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SWIRLDS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package com.swirlds.regression.validators;

import com.swirlds.common.crypto.Hash;


import com.swirlds.fcmap.test.lifecycle.ExpectedValue;
import com.swirlds.fcmap.test.lifecycle.LifecycleStatus;
import com.swirlds.fcmap.test.lifecycle.TransactionState;
import com.swirlds.fcmap.test.lifecycle.TransactionType;
import com.swirlds.fcmap.test.pta.MapKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.swirlds.fcmap.test.lifecycle.EntityType.Blob;
import static com.swirlds.fcmap.test.lifecycle.EntityType.Crypto;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.HANDLED;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.HANDLE_ENTITY_TYPE_MISMATCH;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.HANDLE_FAILED;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.HANDLE_REJECTED;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.INITIALIZED;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.INVALID_SIG;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.RECONNECT_ORIGIN;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.SUBMISSION_FAILED;
import static com.swirlds.fcmap.test.lifecycle.TransactionState.SUBMITTED;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Create;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Delete;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Rebuild;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Transfer;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * test class for LifecycleValidator
 */
public class LifecycleValidatorTest {

	private static final String positiveTestDir = "src/test/resources/logs/Lifecycle";
	private static final String negativeTestDir = "src/test/resources/logs/LifecycleNeg";
	private static final String missingMapsDir = "src/test/resources/logs/PTD-NodeKillReconnect";

	/**
	 * Positive test where ExpectedMaps of all 4 nodes are the same
	 */

	@Test
	void validateExpectedMapsPositiveTest() {
		LifecycleValidator validator = new LifecycleValidator(
				ValidatorTestUtil.loadExpectedMapData(positiveTestDir));
		validator.validate();
		System.out.println("LOGS: " + positiveTestDir);
		System.out.println(validator.concatAllMessages());
		assertEquals(0, validator.getErrorMessages().size());
		assertEquals(true, validator.isValid());
	}

	/**
	 * Test to check errors
	 * Modified the node 0's ExpectedMap to have different Entity type for MapKey[0,0,12]
	 * [0,0,11],[0,0,17],[0,0,14],[0,0,15] has 2 different fields compared to node 0.
	 * So that total mismatch errors will be 24
	 * [0,0,19,Blob] is missing in node 0 . So, total missing Keys will be 3
	 */
	@Test
	void validateExpectedMapsErrors() {
		LifecycleValidator validator = new LifecycleValidator(
				ValidatorTestUtil.loadExpectedMapData(negativeTestDir));
		validator.validate();

		System.out.println("Error messages : \n" + String.join("\n", validator.getErrorMessages()));

		assertEquals(30, validator.getErrorMessages().size());

		assertTrue(validator.getErrorMessages().contains("Entity: MapKey[0,0,11] has field isErrored mismatched. " +
				"node0: true; node1: false"));
		assertTrue(validator.getErrorMessages().contains("Entity: MapKey[0,0,12] has field EntityType " +
				"mismatched. node0: Crypto; node2: Blob"));
		assertTrue(validator.getErrorMessages().contains(
				"KeySet of the expectedMap of node 1 doesn't match with expectedMap of node 0. " +
						"Missing keys in node 1 : [MapKey[0,0,15]], MissingKeys in node 0 : [MapKey[0,0,19]]"));
		assertTrue(validator.getErrorMessages().contains("Entity: MapKey[0,0,14] has field latestHandledStatus " +
				"mismatched. node0: TransactionState: HANDLE_FAILED, TransactionType: Update, timestamp: 1584554112, " +
				"nodeId: 14; node2: TransactionState: HANDLED, TransactionType: Update, timestamp: 1584554112, " +
				"nodeId:" +
				" " +
				"14"));
		assertEquals(false, validator.isValid());
	}


	@Test
	void buildFieldMissMatchMsgTest() throws IOException {
		final MapKey key = new MapKey(0, 1, 2);

		assertEquals("Entity: MapKey[0,1,2] has field entityType mismatched. node0: Blob; node1: Crypto",
				LifecycleValidator.buildFieldMissMatchMsg(key, Blob,
						Crypto, 1, "entityType"));
		byte[] content = generateRandomContent();
		final Hash hash = new Hash(content);
		assertEquals("Entity: MapKey[0,1,2] has field Hash mismatched. node0: null; node1: " + hash,
				LifecycleValidator.buildFieldMissMatchMsg(key, null,
						hash, 1, "Hash"));
	}

	@Test
	void checkMissingKeyTest() {
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = setUpMap();
		LifecycleValidator validator = new LifecycleValidator(expectedMaps);

		validator.checkMissingKeys(expectedMaps.get(0).keySet(),
				expectedMaps.get(2).keySet(), 2);
		List<String> errors = validator.getErrorMessages();
		assertEquals(1, errors.size());

		assertEquals("KeySet of the expectedMap of node 2 doesn't match with expectedMap of node 0. " +
				"Missing keys in node 2 : [MapKey[0,1,2], MapKey[1,2,3]], MissingKeys in node 0 : " +
				"[MapKey[2,2,4], MapKey[2,2,3], MapKey[2,0,2]]", errors.get(0));

		System.out.println(errors.get(0));

	}

	@Test
	public void checkHandleRejectedTests() {
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = setUpMap();
		Map<MapKey, ExpectedValue> map0 = expectedMaps.get(0);
		Map<MapKey, ExpectedValue> map2 = expectedMaps.get(2);

		MapKey key = new MapKey(0, 2, 3);
		MapKey key2 = new MapKey(0, 1, 3);
		MapKey key3 = new MapKey(0, 1, 5);
		map0.put(key3, new ExpectedValue());
		map2.put(key3, new ExpectedValue());

		setValueStatus(map0, key, null, buildLifeCycle(HANDLE_REJECTED, Create), buildLifeCycle(HANDLED, Delete));
		setValueStatus(map2, key, null, buildLifeCycle(HANDLE_REJECTED, Create), buildLifeCycle(HANDLED, Delete));
		setValueStatus(map0, key2, null, buildLifeCycle(HANDLE_REJECTED, Create), buildLifeCycle(HANDLED, Create));
		setValueStatus(map2, key2, null, buildLifeCycle(HANDLE_REJECTED, Create), buildLifeCycle(HANDLED, Create));
		setValueStatus(map0, key3, null, buildLifeCycle(HANDLE_REJECTED, Update), null);
		setValueStatus(map2, key3, null, buildLifeCycle(HANDLE_REJECTED, Update), null);

		LifecycleValidator validator = new LifecycleValidator(expectedMaps);

		validator.checkHandleRejectedStatus(key, map0.get(key),
				map2.get(key), 2);
		List<String> errors = validator.getErrorMessages();
		assertEquals(0, errors.size());

		validator.checkHandleRejectedStatus(key, map0.get(key2),
				map2.get(key2), 2);
		List<String> errors2 = validator.getErrorMessages();
		assertEquals(2, errors2.size());

		assertEquals("ExpectedValue of Key MapKey[0,2,3] on node 0 has the latestHandledStatus " +
				"TransactionState as HANDLE_REJECTED. But, the HistoryHandledStatus is not Deleted/Expired. " +
				"It is Create", errors.get(0));
		assertEquals("ExpectedValue of Key MapKey[0,2,3] on node 2 has the latestHandledStatus " +
				"TransactionState as HANDLE_REJECTED. But, the HistoryHandledStatus is not Deleted/Expired. " +
				"It is Create", errors.get(1));

		validator.checkHandleRejectedStatus(key, map0.get(key3),
				map2.get(key3), 2);
		List<String> errors3 = validator.getErrorMessages();
		assertEquals(4, errors3.size());

		assertEquals("LatestHandledStatus of key MapKey[0,2,3] on node 0 is HANDLE_REJECTED. But, the " +
				"historyHandledStatus is null. An operation Update is performed on non existing entity when " +
				"performOnNonExistingEntities is false", errors.get(2));
		assertEquals("LatestHandledStatus of key MapKey[0,2,3] on node 2 is HANDLE_REJECTED. But, the " +
				"historyHandledStatus is null. An operation Update is performed on non existing entity when " +
				"performOnNonExistingEntities is false", errors.get(3));

		for (String error : errors) {
			System.out.println(error);
		}

	}

	@Test
	public void checkErrorCauseTests() {
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = setUpMap();
		Map<MapKey, ExpectedValue> map0 = expectedMaps.get(0);
		Map<MapKey, ExpectedValue> map2 = expectedMaps.get(2);
		MapKey key = new MapKey(0, 2, 3);
		MapKey key2 = new MapKey(0, 1, 3);
		MapKey key3 = new MapKey(2, 1, 2);
		MapKey key4 = new MapKey(2, 2, 4);

		setValueStatus(map0, key, null, buildLifeCycle(HANDLE_REJECTED, Create), null);
		setValueStatus(map2, key, buildLifeCycle(INITIALIZED, Create), buildLifeCycle(INVALID_SIG, Create), null);
		setValueStatus(map0, key2, buildLifeCycle(SUBMITTED, Create),
				buildLifeCycle(HANDLE_ENTITY_TYPE_MISMATCH, Create), null);
		setValueStatus(map2, key2, buildLifeCycle(SUBMISSION_FAILED, Create), buildLifeCycle(HANDLED, Create), null);
		setValueStatus(map2, key3, buildLifeCycle(INITIALIZED, Create), buildLifeCycle(HANDLE_FAILED, Create), null);
		setValueStatus(map2, key4, buildLifeCycle(INITIALIZED, Create), null, null);

		LifecycleValidator validator = new LifecycleValidator(expectedMaps);

		validator.checkErrorCause(key, map0.get(key), 0);
		validator.checkErrorCause(key, map2.get(key), 2);
		validator.checkErrorCause(key2, map0.get(key2), 0);
		validator.checkErrorCause(key2, map2.get(key2), 2);
		validator.checkErrorCause(key3, map2.get(key3), 2);

		List<String> errors = validator.getErrorMessages();

		assertEquals(6, errors.size());

		for (String error : errors) {
			System.out.println(error);
		}

		assertEquals("Operation Create on Entity MapKey[0,2,3] in Node 0 failed as entity is " +
				"Deleted and PerformOnDeleted is false or entity doesn't exist and " +
				"performOnNonExistingEntities is false", errors.get(0));
		assertEquals("Signature is not valid for Entity MapKey[0,2,3] while performing operation Create on Node 2",
				errors.get(1));
		assertEquals("Operation Create failed as it is performed on wrong entity type Blob", errors.get(2));
		assertEquals("Operation Create failed to get successfully submitted on node 2 for entity MapKey[0,1,3]",
				errors.get(3));
		assertEquals("Something went wrong and entity MapKey[0,1,3] on Node 2 has Error." +
				"Please look at the log for more details", errors.get(4));
		assertEquals("Entity MapKey[2,1,2] on Node 2 has Error. Please look at the log for more details",
				errors.get(5));
	}

	@Test
	public void compareValuesTest() {
		ExpectedValue ev1 = new ExpectedValue(Blob,
				new Hash(generateRandomContent()),
				false,
				buildLifeCycle(SUBMITTED, Create),
				buildLifeCycle(HANDLED, Create),
				null);

		ExpectedValue ev2 = new ExpectedValue(Crypto,
				new Hash(generateRandomContent()),
				true,
				buildLifeCycle(SUBMITTED, Update),
				buildLifeCycle(HANDLE_FAILED, Create),
				buildLifeCycle(HANDLED, Create));

		MapKey key = new MapKey(0, 0, 0);

		LifecycleValidator validator = new LifecycleValidator(setUpMap());
		validator.compareValues(key, ev1, ev2, 4);

		List<String> errors = validator.getErrorMessages();
		assertEquals(5, errors.size());

		assertEquals("Entity: MapKey[0,0,0] has field EntityType mismatched. node0: Blob; node4: Crypto",
				errors.get(0));
		assertEquals("Entity: MapKey[0,0,0] has field isErrored mismatched. node0: false; node4: true", errors.get(1));
		assertTrue(errors.get(2).contains("Entity: MapKey[0,0,0] has field getHash mismatched"));
		assertEquals("Entity: MapKey[0,0,0] has field latestHandledStatus mismatched. node0: " +
						"TransactionState: HANDLED, TransactionType: Create, timestamp: 0, nodeId: -1; node4: " +
						"TransactionState: HANDLE_FAILED, TransactionType: Create, timestamp: 0, nodeId: -1",
				errors.get(3));
		assertEquals("Entity: MapKey[0,0,0] has field historyHandledStatus mismatched. node0: " +
				"null; node4: TransactionState: HANDLED, TransactionType: Create, timestamp: 0, nodeId: " +
				"-1", errors.get(4));

		for (String error : errors) {
			System.out.println(error);
		}
	}

	@Test
	public void compareValuesRebuildTest() {
		Hash hash = new Hash(generateRandomContent());
		ExpectedValue ev1 = new ExpectedValue(Crypto,
				hash,
				false,
				null,
				buildLifeCycle(HANDLED, Create),
				buildLifeCycle(HANDLED, Update));

		ExpectedValue ev2 = new ExpectedValue(Crypto,
				hash,
				false,
				null,
				buildLifeCycle(RECONNECT_ORIGIN, Rebuild),
				null);

		MapKey key = new MapKey(0, 0, 0);

		LifecycleValidator validator = new LifecycleValidator(setUpMap());
		validator.compareValues(key, ev1, ev2, 4);

		List<String> errors = validator.getErrorMessages();
		for (String error : errors) {
			System.out.println(error);
		}
		assertEquals(0, errors.size());
	}

	@Test
	public void validateExpectedMapTests() {
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = setUpMap();
		Map<MapKey, ExpectedValue> map1 = new ConcurrentHashMap<>();
		expectedMaps.put(1, map1);

		LifecycleValidator validator = new LifecycleValidator(expectedMaps);
		validator.validate();

		List<String> errors = validator.getErrorMessages();
		assertEquals(2, errors.size());
		assertEquals("KeySet of the expectedMap of node 1 doesn't match with expectedMap of node 0. " +
				"Missing keys in node 1 : [MapKey[0,2,3], MapKey[0,1,3], MapKey[0,1,2], MapKey[1,2,3]], " +
				"MissingKeys in node 0 : []", errors.get(0));
		assertEquals("KeySet of the expectedMap of node 2 doesn't match with expectedMap of node 0. " +
				"Missing keys in node 2 : [MapKey[0,1,2], MapKey[1,2,3]], MissingKeys in node 0 : " +
				"[MapKey[2,2,4], MapKey[2,2,3], MapKey[2,0,2]]", errors.get(1));
		for (String error : errors) {
			System.out.println(error);
		}
	}

	@Test
	public void checkMissingExpectedMapsTest() {
		LifecycleValidator validator = null;
		try {
			validator = new LifecycleValidator(ValidatorTestUtil.loadExpectedMapData(missingMapsDir));
		} catch (Exception e) {
			assertEquals(" expectedMap in node 0 doesn't exist", e.getMessage());
		}
	}

	private void setValueStatus(Map<MapKey, ExpectedValue> map, MapKey key,
			LifecycleStatus latestSubmissionStatus, LifecycleStatus latestHandleStatus,
			LifecycleStatus historyStatusDelete) {
		ExpectedValue evKey = map.get(key);
		if (evKey == null) {
			evKey = new ExpectedValue();
		}
		evKey.setLatestSubmitStatus(latestSubmissionStatus).
				setLatestHandledStatus(latestHandleStatus).
				setHistoryHandledStatus(historyStatusDelete);
		map.put(key, evKey);
	}

	/**
	 * build LifecycleStatus object from the state and transaction type
	 */
	private LifecycleStatus buildLifeCycle(TransactionState state, TransactionType type) {
		return LifecycleStatus.builder().
				setTransactionState(state).
				setTransactionType(type).build();
	}

	/**
	 * generate random bytes to generate Hash
	 */
	private byte[] generateRandomContent() {
		int contentSize = 48;
		Random random = new Random();
		final byte[] content = new byte[contentSize];
		random.nextBytes(content);
		return content;
	}

	@Test
	public void compareValuesHistoryRebuildTest() {
		Hash hash = new Hash(generateRandomContent());
		ExpectedValue ev1 = new ExpectedValue(Crypto,
				hash,
				false,
				null,
				buildLifeCycle(HANDLED, Create),
				buildLifeCycle(HANDLED, Update));

		ExpectedValue ev2 = new ExpectedValue(Crypto,
				hash,
				false,
				null,
				buildLifeCycle(HANDLED, Create),
				buildLifeCycle(RECONNECT_ORIGIN, Rebuild));

		MapKey key = new MapKey(0, 0, 0);

		LifecycleValidator validator = new LifecycleValidator(setUpMap());
		validator.compareValues(key, ev1, ev2, 4);

		List<String> errors = validator.getErrorMessages();
		for (String error : errors) {
			System.out.println(error);
		}
		assertEquals(0, errors.size());
	}

	private Map<Integer, Map<MapKey, ExpectedValue>> setUpMap() {
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = new HashMap<>();
		Map<MapKey, ExpectedValue> map0 = new ConcurrentHashMap<>();
		Map<MapKey, ExpectedValue> map2 = new ConcurrentHashMap<>();
		expectedMaps.put(0, map0);
		expectedMaps.put(2, map2);

		// only in map0
		MapKey key1 = new MapKey(0, 1, 2);
		MapKey key2 = new MapKey(1, 2, 3);
		map0.put(key1, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));
		map0.put(key2, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));

		// two equal keys
		MapKey equalKey1 = new MapKey(0, 2, 3);
		MapKey equalKey2 = new MapKey(0, 2, 3);
		map0.put(equalKey1, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));
		map2.put(equalKey2, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));

		// only in map2
		MapKey key3 = new MapKey(2, 0, 2);
		MapKey key4 = new MapKey(2, 2, 3);
		MapKey key5 = new MapKey(2, 2, 4);
		map2.put(key3, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));
		map2.put(key4, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));
		map2.put(key5, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));

		// another two equal keys get by copy()
		MapKey key = new MapKey(0, 1, 3);
		MapKey copy = new MapKey(0, 1, 3);
		map0.put(key, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));
		map2.put(copy, new ExpectedValue(Blob, new Hash(), false,
				new LifecycleStatus(INITIALIZED, Update, Instant.now().toEpochMilli(), 0),
				new LifecycleStatus(HANDLED, Transfer, Instant.now().toEpochMilli(), 0), null));

		return expectedMaps;
	}
}
