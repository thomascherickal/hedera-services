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
import com.swirlds.demo.platform.fcm.MapKey;
import com.swirlds.demo.platform.fcm.MapValueData;
import com.swirlds.demo.platform.fcm.lifecycle.EntityType;
import com.swirlds.demo.platform.fcm.lifecycle.ExpectedValue;
import com.swirlds.demo.platform.fcm.lifecycle.LifecycleStatus;
import com.swirlds.demo.platform.fcm.lifecycle.TransactionState;
import com.swirlds.demo.platform.fcm.lifecycle.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.swirlds.demo.platform.fcm.lifecycle.EntityType.Crypto;
import static com.swirlds.demo.platform.fcm.lifecycle.TransactionState.HANDLED;
import static com.swirlds.demo.platform.fcm.lifecycle.TransactionState.HANDLE_REJECTED;
import static com.swirlds.demo.platform.fcm.lifecycle.TransactionType.Create;
import static com.swirlds.demo.platform.fcm.lifecycle.TransactionType.Delete;
import static com.swirlds.demo.platform.fcm.lifecycle.TransactionType.Expire;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// test class for PTA LifecycleValidator
public class PTALifeCycleValidatorTest {

	private static final String positiveTestDir =  "src/test/resources/logs/Lifecycle";
	private static final String negativeTestDir =  "src/test/resources/logs/LifecycleNeg";

	// Positive test where ExpectedMaps of all 4 nodes are the same
	@Test
	void validateExpectedMapsPositiveTest() {
		PTALifecycleValidator validator = new PTALifecycleValidator(ValidatorTestUtil.loadExpectedMapData(positiveTestDir));
		validator.validate();
		System.out.println("LOGS: " + positiveTestDir);
		System.out.println(validator.concatAllMessages());
		assertEquals(0, validator.getErrorMessages().size());
		assertEquals(true, validator.isValid());
	}

	// Test to check errors
	// 	Modified the node 0's ExpectedMap to have different Entity type for MapKey[0,0,12]
	//	[0,0,11],[0,0,17],[0,0,14],[0,0,15] has 2 different fields compared to node 0.
	//	So that total mismatch errors will be 24
	//	[0,0,19,Blob] is missing in node 0 . So, total missing Keys will be 3
	@Test
	void validateExpectedMapsErrors() {
		PTALifecycleValidator validator = new PTALifecycleValidator(ValidatorTestUtil.loadExpectedMapData(negativeTestDir));
		validator.validate();

		assertEquals(30, validator.getErrorMessages().size());

		System.out.println("Error messages : \n"+ String.join("\n",validator.getErrorMessages()));

		assertTrue(validator.getErrorMessages().contains("Entity: MapKey[0,0,11] has field isErrored mismatched. " +
				"node0: true; node1: false"));
		assertTrue(validator.getErrorMessages().contains("Entity: MapKey[0,0,12] has field EntityType " +
				"mismatched. node0: Crypto; node2: Blob"));
		assertTrue(validator.getErrorMessages().contains("KeySet of the expectedMap of node 0 doesn't match " +
				"with expectedMap of node 3. Missing keys: MapKey[0,0,19]"));
		assertTrue(validator.getErrorMessages().contains("Entity: MapKey[0,0,14] has field latestHandledStatus " +
				"mismatched. node0: TransactionState: HANDLE_FAILED, TransactionType: Update, timestamp: 1584554112, " +
				"nodeId: 14; node2: TransactionState: HANDLED, TransactionType: Update, timestamp: 1584554112, nodeId: " +
				"14"));
		assertEquals(false, validator.isValid());
	}


	@Test
	void buildFieldMissMatchMsgTest() throws IOException {
		final MapKey key = new MapKey(0, 1, 2);

		assertEquals("Entity: MapKey[0,1,2] has field entityType mismatched. node0: Blob; node1: Crypto",
				PTALifecycleValidator.buildFieldMissMatchMsg(key, EntityType.Blob,
				EntityType.Crypto, 1, "entityType"));

		final Hash hash = new MapValueData().calculateHash();
		assertEquals("Entity: MapKey[0,1,2] has field Hash mismatched. node0: null; node1: " + hash,
				PTALifecycleValidator.buildFieldMissMatchMsg(key, null,
						hash, 1, "Hash"));
	}

	@Test
	void checkMissingKeyTest() {
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = setUpMap();
		PTALifecycleValidator validator = new PTALifecycleValidator(expectedMaps);

		validator.checkMissingKeys(expectedMaps.get(0).keySet(),
				expectedMaps.get(2).keySet(), 2);
		List<String> errors = validator.getErrorMessages();
		assertEquals(2, errors.size());
		assertEquals("KeySet of the expectedMap of node 2 doesn't match with expectedMap of node 0. Missing keys: MapKey[0,1,2],MapKey[1,2,3]", errors.get(0));

		assertEquals("KeySet of the expectedMap of node 0 doesn't match with expectedMap of node 2. Missing keys: MapKey[2,0,2],MapKey[2,2,3],MapKey[2,2,4]", errors.get(1));
		for (String error : errors) {
			System.out.println(error);
		}
	}

	@Test
	public void checkHandleRejectedTests(){
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = setUpMap();
		Map<MapKey, ExpectedValue> map0 = expectedMaps.get(0);
		Map<MapKey, ExpectedValue> map2 = expectedMaps.get(2);
		MapKey key = new MapKey(0, 2, 3);
		MapKey key2 = new MapKey(0, 1, 3);

		LifecycleStatus latestHandleStatus = LifecycleStatus.builder().
												setTransactionState(HANDLE_REJECTED).
												setTransactionType(Create).build();
		LifecycleStatus historyStatusDelete = LifecycleStatus.builder().
												setTransactionState(HANDLE_REJECTED).
												setTransactionType(Delete).build();
		LifecycleStatus historyStatusCreate = LifecycleStatus.builder().
												setTransactionState(HANDLE_REJECTED).
												setTransactionType(Create).build();
		setValueStatus(map0, key, latestHandleStatus, historyStatusDelete);
		setValueStatus(map2, key, latestHandleStatus, historyStatusDelete);
		setValueStatus(map0, key2, latestHandleStatus, historyStatusCreate);
		setValueStatus(map2, key2, latestHandleStatus, historyStatusCreate);

		PTALifecycleValidator validator = new PTALifecycleValidator(expectedMaps);

		validator.checkHandleRejectedStatus(key, map0.get(key),
				map2.get(key), 2);
		List<String> errors = validator.getErrorMessages();
		assertEquals(0, errors.size());

		validator.checkHandleRejectedStatus(key, map0.get(key2),
				map2.get(key2), 2);
		List<String> errors2 = validator.getErrorMessages();
		assertEquals(2, errors2.size());

		assertEquals("ExpectedValue of Key MapKey[0,2,3] on node 0 has the latestHandledStatus " +
				"TransactionState as HANDLE_REJECTED.But, the HistoryHandledStatus is not Deleted/Expired. " +
				"It isCreate", errors.get(0));
		assertEquals("ExpectedValue of Key MapKey[0,2,3] on node 2 has the latestHandledStatus " +
				"TransactionState as HANDLE_REJECTED.But, the HistoryHandledStatus is not Deleted/Expired. " +
				"It isCreate", errors.get(1));
		for (String error : errors) {
			System.out.println(error);
		}

	}

	private void setValueStatus(Map<MapKey, ExpectedValue> map, MapKey key,
			LifecycleStatus latestHandleStatus, LifecycleStatus historyStatusDelete) {
		ExpectedValue evKey = map.get(key);
		evKey.setLatestHandledStatus(latestHandleStatus).setHistoryHandledStatus(historyStatusDelete);
		map.put(key, evKey);
	}

	private Map<Integer, Map<MapKey, ExpectedValue>> setUpMap(){
		Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = new HashMap<>();
		Map<MapKey, ExpectedValue> map0 = new ConcurrentHashMap<>();
		Map<MapKey, ExpectedValue> map2 = new ConcurrentHashMap<>();
		expectedMaps.put(0, map0);
		expectedMaps.put(2, map2);

		// only in map0
		MapKey key1 = new MapKey(0, 1, 2);
		MapKey key2 = new MapKey(1, 2, 3);
		map0.put(key1, new ExpectedValue());
		map0.put(key2, new ExpectedValue());

		// two equal keys
		MapKey equalKey1 = new MapKey(0, 2, 3);
		MapKey equalKey2 = new MapKey(0, 2, 3);
		map0.put(equalKey1, new ExpectedValue());
		map2.put(equalKey2, new ExpectedValue());

		// only in map2
		MapKey key3 = new MapKey(2, 0, 2);
		MapKey key4 = new MapKey(2, 2, 3);
		MapKey key5 = new MapKey(2, 2, 4);
		map2.put(key3, new ExpectedValue());
		map2.put(key4, new ExpectedValue());
		map2.put(key5, new ExpectedValue());

		// another two equal keys get by copy()
		MapKey key = new MapKey(0, 1, 3);
		MapKey copy = new MapKey(0, 1, 3);
		map0.put(key, new ExpectedValue());
		map2.put(copy, new ExpectedValue());

		return expectedMaps;
	}
}
