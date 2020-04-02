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

import com.swirlds.demo.platform.fcm.MapKey;
import com.swirlds.demo.platform.fcm.lifecycle.ExpectedValue;
import com.swirlds.demo.platform.fcm.lifecycle.LifecycleStatus;
import com.swirlds.demo.platform.fcm.lifecycle.TransactionState;
import com.swirlds.demo.platform.fcm.lifecycle.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swirlds.demo.platform.fcm.lifecycle.TransactionState.SUBMISSION_FAILED;

/**
 * Validator to validate lifecycle of all entities in ExpectedMap
 */
public class PTALifecycleValidator extends Validator {
	private static Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps;
	private static boolean isValid;
	private static boolean isValidated;
	public static final String EXPECTED_MAP_ZIP = "ExpectedMap.json.gz";

	public PTALifecycleValidator(ExpectedMapData mapData) {
		expectedMaps = mapData.getExpectedMaps();
		isValid = false;
		isValidated = false;
	}

	/**
	 * only for unit test
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
	private void validateExpectedMaps(){
		Map<MapKey, ExpectedValue> baselineMap = expectedMaps.get(0);

		for(int i=1; i< expectedMaps.size();i++){
			Map<MapKey, ExpectedValue> mapToCompare = expectedMaps.get(i);

			if(baselineMap.keySet() != mapToCompare.keySet()){
				checkMissingKeys(baselineMap.keySet(),mapToCompare.keySet(), i);
			}

			for(MapKey key : baselineMap.keySet()){
				if(mapToCompare.containsKey(key)){
					ExpectedValue baseValue = baselineMap.get(key);
					ExpectedValue compareValue = mapToCompare.get(key);
					if(!baseValue.equals(compareValue)){
						compareValues(key, baseValue, compareValue, i);
					}else {
						checkHandleRejectedStatus(key, baseValue, compareValue, i);
					}
					if(baseValue.isErrored() && compareValue.isErrored()){
						checkErrorCause(key, baseValue, 0);
						checkErrorCause(key, compareValue, i);
					}
				}
			}
		}
		addInfo("PTALifecycleValidator validated ExpectedMaps of "+ expectedMaps.size() +" nodes");
		if(errorMessages.size() == 0) {
			isValid = true;
			addInfo("Validator has no exceptions");
		}
		isValidated = true;
	}

	/**
	 * check if there are any entities that have latestHandledStatus as HANDLE_REJECTED
	 * @param key key of the entity
	 * @param baseValue expectedValue of entity in first node
	 * @param compareValue expectedValue of entity in other node that is being compared
	 * @param nodeNum Node number of the node on which entities are being compared
	 */
	public void checkHandleRejectedStatus(MapKey key, ExpectedValue baseValue, ExpectedValue compareValue, int nodeNum) {
		LifecycleStatus baseLifecycle = baseValue.getLatestHandledStatus();
		LifecycleStatus compareLifecycle = compareValue.getLatestHandledStatus();
		LifecycleStatus baseHistory = baseValue.getHistoryHandledStatus();
		LifecycleStatus compareHistory = compareValue.getHistoryHandledStatus();

		if((baseLifecycle == null && compareLifecycle == null) ||
				(baseLifecycle.getTransactionState() == null && compareLifecycle.getTransactionState() ==null))
			return;

		if(baseLifecycle == null || compareLifecycle == null){
			addError("latestHandleStatus of one of the expectedValues is null . " +
					"Node 0:"+ baseLifecycle +
					", Node "+nodeNum + ": "+ compareLifecycle);
			return;
		}

		if((baseLifecycle.getTransactionState().equals(TransactionState.HANDLE_REJECTED)))
			checkHistory(key, baseHistory.getTransactionType(), 0);

		if((compareLifecycle.getTransactionState().equals(TransactionState.HANDLE_REJECTED)))
			checkHistory(key, compareHistory.getTransactionType(), nodeNum);
	}

	/**
	 * Check the historyHandleStatus of an entity
	 * @param key key of the entity
	 * @param historyType TransactionType of historyHandleStatus
	 * @param nodeNum Node number of the node on which entities are being compared
	 */
	private void checkHistory(MapKey key , TransactionType historyType, int nodeNum) {
		if(historyType == null){
			addError("ExpectedValue of Key "+key+" on node "+ nodeNum+ " has the latestHandledStatus TransactionState " +
					"as HANDLE_REJECTED. But, the HistoryHandledStatus is null and not Deleted/Expired.");
			return;
		}else if(!(historyType.equals(TransactionType.Delete) ||
				historyType.equals(TransactionType.Expire))){
			addError("ExpectedValue of Key "+key+" on node "+ nodeNum+" has the latestHandledStatus TransactionState as "+
					"HANDLE_REJECTED.But, the HistoryHandledStatus is not Deleted/Expired." +
					" It is"+historyType);
		}
	}

	/**
	 * If the KeySet size of maps differ logs error with the missing keys
	 * @param baseKeySet KeySet of expectedMap of firstNode
	 * @param compareKeySet KeySet of expectedMap on the node that is being compared
	 * @param nodeNum Node number of the node on which entities are being compared
	 */
	public void checkMissingKeys(Set<MapKey> baseKeySet, Set<MapKey> compareKeySet, int nodeNum) {

		String missingKeysInCompare = baseKeySet.
				stream().
				filter(x -> !compareKeySet.contains(x)).
				map(key -> key.toString()).
				sorted().
				collect(Collectors.joining(","));
		if(!missingKeysInCompare.isEmpty()) {
			addError("KeySet of the expectedMap of node " + nodeNum +
					" doesn't match with expectedMap of node 0. " +
					"Missing keys in node " +nodeNum + ": "+  missingKeysInCompare);
		}

		String missingKeysInBase = compareKeySet.
				stream().
				filter(x -> !baseKeySet.contains(x)).
				map(key -> key.toString()).
				sorted().
				collect(Collectors.joining(","));;
		if(!missingKeysInBase.isEmpty()){
			addError("KeySet of the expectedMap of node 0 doesn't match with expectedMap of node " +nodeNum +
					". Missing keys in node 0 :" + missingKeysInBase);
		}
	}

	/**
	 * Build a String message to be used in compareValues()
	 * @param key key of the mismatched entity
	 * @param base First object to be compared
	 * @param other Other object to be compared
	 * @param nodeNum Node number of the node on which entities are being compared
	 * @param fieldName Field that is mismatched in the entity
	 * @return
	 */
	public static String buildFieldMissMatchMsg(final MapKey key, final Object base,
			final Object other, final int nodeNum, final String fieldName) {
		return String.format("Entity: %s has field %s mismatched. node0: %s; node%d: %s",
				key, fieldName, base, nodeNum, other);
	}

	/**
	 * If two ExpectedValues doesn't match checks all the fields of expectedValues
	 * and logs which fields mismatch
	 * @param key key of the entity
	 * @param ev1 ExpectedValue to be compared
	 * @param ev2 ExpectedValue of entity on other node to be compared
	 * @param nodeNum Node number of the node on which entities are being compared
	 */
	public void compareValues(MapKey key, ExpectedValue ev1, ExpectedValue ev2, int nodeNum){
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
	 * @param key key of the entity
	 * @param ev2 ExpectedValue of the entity
	 * @param nodeNum Node number of the node on which entities are being compared
	 */
	public void checkErrorCause(MapKey key, ExpectedValue ev2, int nodeNum) {
		LifecycleStatus latestHandleStatus = ev2.getLatestHandledStatus();
		LifecycleStatus latestSubmitStatus = ev2.getLatestSubmitStatus();

		if(latestSubmitStatus == null || latestSubmitStatus.getTransactionState() == null) {
			addError("latestSubmitStatus for Entity " + key + " is null");
		}else if(latestSubmitStatus.equals(SUBMISSION_FAILED)) {
			addError("Operation " + latestSubmitStatus.getTransactionType() +
					"failed to get successfully submitted on node " + nodeNum + " for entity " + key);
		}
		if(latestHandleStatus == null || latestHandleStatus.getTransactionState() == null)
			return;

		switch (latestHandleStatus.getTransactionState()) {
			case  INVALID_SIG:
				addError("Signature is not valid for Entity "+ key +" while performing operation "
						+ latestHandleStatus.getTransactionType() + " on Node "+ nodeNum);
				break;
			case  HANDLE_FAILED:
				addError("Entity "+ key + "on Node "+ nodeNum + " has Error. Please look at the log for more details");
				break;
			case  HANDLE_REJECTED:
				addError("Operation "+latestHandleStatus.getTransactionType()+ " on Entity "+ key
						+ "in Node "+ nodeNum+ " failed as entity already exists");
				break;
			case  HANDLE_ENTITY_TYPE_MISMATCH:
				addError("Operation "+ latestHandleStatus.getTransactionType()+
						" failed as it is performed on wrong entity type "+ ev2.getEntityType());
				break;
			default:
		}
	}
}
