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

import com.swirlds.fcmap.test.lifecycle.ExpectedValue;
import com.swirlds.fcmap.test.lifecycle.LifecycleStatus;
import com.swirlds.fcmap.test.lifecycle.TransactionState;
import com.swirlds.fcmap.test.lifecycle.TransactionType;
import com.swirlds.fcmap.test.pta.MapKey;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.swirlds.fcmap.test.lifecycle.TransactionState.SUBMISSION_FAILED;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Delete;
import static com.swirlds.fcmap.test.lifecycle.TransactionType.Expire;


/**
 * Validator to validate lifecycle of all entities in ExpectedMap
 */
public class PTALifecycleValidator extends Validator {
	private static Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps = null;
	private static boolean isValid;
	private static boolean isValidated;
	public static final String EXPECTED_MAP_ZIP = "ExpectedMap.json.gz";

	public static final String HANDLE_REJECTED_ERROR = "ExpectedValue of Key %s on node %d has the " +
			"latestHandledStatus TransactionState as HANDLE_REJECTED. But, the HistoryHandledStatus " +
			"is not Deleted/Expired. It is %s";
	public static final String MISSING_KEYS_ERROR = "KeySet of the expectedMap of node %d doesn't match with " +
			"expectedMap of node %d. " +
			"Missing keys in node %d : %s, MissingKeys in node 0 : %s";
	public static final String FIELD_MISMATCH_ERROR = "Entity: %s has field %s mismatched. node0: %s; node%d: %s";

	public static final String NULL_LATEST_HANDLED_STATUS_ERROR = "latestHandleStatus of one of the expectedValues is" +
			" " +
			"null. " +
			"Node 0 : %s , Node %d : %s ";

	public PTALifecycleValidator(ExpectedMapData mapData) {
		if (mapData != null) {
			expectedMaps = mapData.getExpectedMaps();
		}
		isValid = false;
		isValidated = false;
	}

	/**
	 * only for unit test
	 *
	 * @param expectedMaps
	 */
	PTALifecycleValidator(Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps) {
		this.expectedMaps = expectedMaps;
		isValid = false;
		isValidated = false;
	}

	@Override
	public List<String> getErrorMessages() {
		return errorMessages;
	}

	/**
	 * Validate the expectedMaps downloaded in the results folder after an experiment is completed
	 */
	@Override
	public void validate() {
		validateExpectedMaps();
	}

	/**
	 * Check if the experiment completed successfully
	 *
	 * @return Boolean that signifies there are no error messages while validating expectedMaps from all nodes
	 */
	@Override
	public boolean isValid() {
		return isValid && isValidated;
	}

	/**
	 * ExpectedMaps are valid if there are no error messages recorded while
	 * expectedMaps from all nodes
	 */
	private void validateExpectedMaps() {
		if (expectedMaps == null) {
			addError("ExpectedMap doesn't exist on nodes for validation");
			isValid = false;
			isValidated = true;
			return;
		}
		Map<MapKey, ExpectedValue> baselineMap = expectedMaps.get(0);

		for (int i = 1; i < expectedMaps.size(); i++) {
			Map<MapKey, ExpectedValue> mapToCompare = expectedMaps.get(i);

			if (!baselineMap.keySet().equals(mapToCompare.keySet())) {
				checkMissingKeys(baselineMap.keySet(), mapToCompare.keySet(), i);
			}

			for (MapKey key : baselineMap.keySet()) {
				if (mapToCompare.containsKey(key)) {
					ExpectedValue baseValue = baselineMap.get(key);
					ExpectedValue compareValue = mapToCompare.get(key);
					if (!baseValue.equals(compareValue)) {
						compareValues(key, baseValue, compareValue, i);
					} else {
						checkHandleRejectedStatus(key, baseValue, compareValue, i);
					}
					if (baseValue.isErrored() && compareValue.isErrored()) {
						checkErrorCause(key, baseValue, 0);
						checkErrorCause(key, compareValue, i);
					}
				}
			}
		}
		addInfo("PTALifecycleValidator validated ExpectedMaps of " + expectedMaps.size() + " nodes");
		if (errorMessages.size() == 0) {
			isValid = true;
			addInfo("Validator has no exceptions");
		}
		isValidated = true;
	}

	/**
	 * check if there are any entities that have latestHandledStatus as HANDLE_REJECTED
	 *
	 * @param key
	 * 		key of the entity
	 * @param baseValue
	 * 		expectedValue of entity in first node
	 * @param compareValue
	 * 		expectedValue of entity in other node that is being compared
	 * @param nodeNum
	 * 		Node number of the node on which entities are being compared
	 */
	public void checkHandleRejectedStatus(MapKey key, ExpectedValue baseValue, ExpectedValue compareValue,
			int nodeNum) {
		LifecycleStatus baseLifecycle = baseValue.getLatestHandledStatus();
		LifecycleStatus compareLifecycle = compareValue.getLatestHandledStatus();
		LifecycleStatus baseHistory = baseValue.getHistoryHandledStatus();
		LifecycleStatus compareHistory = compareValue.getHistoryHandledStatus();

		if ((baseLifecycle == null && compareLifecycle == null) ||
				(baseLifecycle.getTransactionState() == null && compareLifecycle.getTransactionState() == null))
			return;

		if (baseLifecycle == null || compareLifecycle == null) {
			addError(String.format(NULL_LATEST_HANDLED_STATUS_ERROR, baseLifecycle, nodeNum, compareLifecycle));
			return;
		}

		if ((baseLifecycle.getTransactionState().equals(TransactionState.HANDLE_REJECTED)))
			checkHistory(key, baseHistory.getTransactionType(), 0);

		if ((compareLifecycle.getTransactionState().equals(TransactionState.HANDLE_REJECTED)))
			checkHistory(key, compareHistory.getTransactionType(), nodeNum);
	}

	/**
	 * Check the historyHandleStatus of an entity
	 *
	 * @param key
	 * 		key of the entity
	 * @param historyType
	 * 		TransactionType of historyHandleStatus
	 * @param nodeNum
	 * 		Node number of the node on which entities are being compared
	 */
	private void checkHistory(MapKey key, TransactionType historyType, int nodeNum) {
		if (historyType == null) {
			addError(String.format(HANDLE_REJECTED_ERROR, key, nodeNum, null));
			return;
		} else if (!(historyType.equals(Delete) ||
				historyType.equals(Expire))) {
			addError(String.format(HANDLE_REJECTED_ERROR, key, nodeNum, historyType));
		}
	}

	/**
	 * If the KeySet size of maps differ logs error with the missing keys
	 *
	 * @param baseKeySet
	 * 		KeySet of expectedMap of firstNode
	 * @param compareKeySet
	 * 		KeySet of expectedMap on the node that is being compared
	 * @param nodeNum
	 * 		Node number of the node on which entities are being compared
	 */
	public void checkMissingKeys(Set<MapKey> baseKeySet, Set<MapKey> compareKeySet, int nodeNum) {
		Set<MapKey> missingKeysInCompare = new HashSet<>();
		Set<MapKey> missingKeysInBase = new HashSet<>();

		missingKeysInBase.addAll(compareKeySet);
		missingKeysInCompare.addAll(baseKeySet);

		missingKeysInBase.removeAll(baseKeySet);
		missingKeysInCompare.removeAll(compareKeySet);

		missingKeysInBase = checkRemoved(nodeNum, missingKeysInBase);
		missingKeysInCompare = checkRemoved(0, missingKeysInCompare);

		if (missingKeysInBase.size() > 0 || missingKeysInCompare.size() > 0) {
			addError(String.format(MISSING_KEYS_ERROR, nodeNum, 0, nodeNum, missingKeysInCompare, missingKeysInBase));
		}
	}

	/**
	 * check if the missing keys are Deleted or Expired in other node. If so, ignore them as missing Keys.
	 *
	 * @param nodeNum
	 * @param missingKeys
	 * @return Set of missing keys other than expired or deleted ones
	 */
	private Set<MapKey> checkRemoved(int nodeNum, Set<MapKey> missingKeys) {
		Set<MapKey> missingKeysSet = new HashSet<>();
		for (MapKey key : missingKeys) {
			LifecycleStatus latestHandleStatus = expectedMaps.get(nodeNum).get(key).getLatestHandledStatus();
			if (latestHandleStatus != null && latestHandleStatus.getTransactionType() != null &&
					(!latestHandleStatus.getTransactionType().equals(Delete) &&
							!latestHandleStatus.getTransactionType().equals(Expire))) {
				missingKeysSet.add(key);
			}
		}
		return missingKeysSet;
	}

	/**
	 * Build a String message to be used in compareValues()
	 *
	 * @param key
	 * 		key of the mismatched entity
	 * @param base
	 * 		First object to be compared
	 * @param other
	 * 		Other object to be compared
	 * @param nodeNum
	 * 		Node number of the node on which entities are being compared
	 * @param fieldName
	 * 		Field that is mismatched in the entity
	 * @return
	 */
	public static String buildFieldMissMatchMsg(final MapKey key, final Object base,
			final Object other, final int nodeNum, final String fieldName) {
		return String.format(FIELD_MISMATCH_ERROR, key, fieldName, base, nodeNum, other);
	}

	/**
	 * If two ExpectedValues doesn't match checks all the fields of expectedValues
	 * and logs which fields mismatch
	 *
	 * @param key
	 * 		key of the entity
	 * @param ev1
	 * 		ExpectedValue to be compared
	 * @param ev2
	 * 		ExpectedValue of entity on other node to be compared
	 * @param nodeNum
	 * 		Node number of the node on which entities are being compared
	 */
	public void compareValues(MapKey key, ExpectedValue ev1, ExpectedValue ev2, int nodeNum) {
		if (!Objects.equals(ev1.getEntityType(), ev2.getEntityType())) {
			addError(buildFieldMissMatchMsg(key, ev1.getEntityType(),
					ev2.getEntityType(), nodeNum, "EntityType"));
		}

		if (!Objects.equals(ev1.isErrored(), ev2.isErrored())) {
			addError(buildFieldMissMatchMsg(key, ev1.isErrored(),
					ev2.isErrored(), nodeNum, "isErrored"));
		}

		if (!Objects.equals(ev1.getHash(), ev2.getHash())) {
			addError(buildFieldMissMatchMsg(key, ev1.getHash(),
					ev2.getHash(), nodeNum, "getHash"));
		}

		if (!Objects.equals(ev1.getLatestHandledStatus(), ev2.getLatestHandledStatus())) {
			addError(buildFieldMissMatchMsg(key, ev1.getLatestHandledStatus(),
					ev2.getLatestHandledStatus(), nodeNum, "latestHandledStatus"));
		}

		if (!Objects.equals(ev1.getHistoryHandledStatus(), ev2.getHistoryHandledStatus())) {
			addError(buildFieldMissMatchMsg(key, ev1.getHistoryHandledStatus(),
					ev2.getHistoryHandledStatus(), nodeNum, "historyHandledStatus"));
		}
	}

	/**
	 * If isErrored flag is set to true on an ExpectedValue in expectedMap, it means some error
	 * occurred during the experiment. Checks the causes for error.
	 *
	 * @param key
	 * 		key of the entity
	 * @param ev2
	 * 		ExpectedValue of the entity
	 * @param nodeNum
	 * 		Node number of the node on which entities are being compared
	 */
	public void checkErrorCause(MapKey key, ExpectedValue ev2, int nodeNum) {
		LifecycleStatus latestHandleStatus = ev2.getLatestHandledStatus();
		LifecycleStatus latestSubmitStatus = ev2.getLatestSubmitStatus();

		if (latestSubmitStatus != null && latestSubmitStatus.getTransactionState() != null &&
				latestSubmitStatus.getTransactionState().equals(SUBMISSION_FAILED)) {
			addError("Operation " + latestSubmitStatus.getTransactionType() +
					" failed to get successfully submitted on node " + nodeNum + " for entity " + key);
		}
		if (latestHandleStatus == null || latestHandleStatus.getTransactionState() == null)
			return;

		switch (latestHandleStatus.getTransactionState()) {
			case INVALID_SIG:
				addError(String.format("Signature is not valid for Entity %s while performing operation " +
						"%s on Node %d", key, latestHandleStatus.getTransactionType(), nodeNum));
				break;
			case HANDLE_FAILED:
				addError(String.format("Entity %s on Node %d has Error. Please look at the log for " +
						"more details", key, nodeNum));
				break;
			case HANDLE_REJECTED:
				addError(String.format("Operation %s on Entity %s in Node %d failed as entity is Deleted " +
						"and PerformOnDeleted is false", latestHandleStatus.getTransactionType(), key, nodeNum));
				break;
			case HANDLE_ENTITY_TYPE_MISMATCH:
				addError(String.format("Operation %s failed as it is performed on wrong entity type %s",
						latestHandleStatus.getTransactionType(), ev2.getEntityType()));
				break;
			default:
				addError(String.format("Something went wrong and entity %s on Node %d has Error." +
						"Please look at the log for more details", key, nodeNum));
		}
	}
}
