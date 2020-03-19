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

/**
 * Validator to validate lifecycle of all entities in ExpectedMap
 */
public class PTALifecycleValidator extends Validator {
	private static Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps;
	private static boolean isValid;
	public static final String EXPECTED_MAP = "ExpectedMap.json";

	/**
	 * List of errors
	 */
	private List<String> errorMessages = new ArrayList<>();

	void addError(String msg) {
		errorMessages.add(msg);
	}

	public PTALifecycleValidator(ExpectedMapData mapData) {
		expectedMaps = mapData.getExpectedMaps();
		isValid = false;
	}

	/**
	 * only for unit test
	 * @param expectedMaps
	 */
	PTALifecycleValidator(Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps) {
		this.expectedMaps = expectedMaps;
		isValid = false;
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
		isValid = validateExpectedMaps();
	}

	/**
	 * Check if the experiment completed successfully
	 * @return Boolean that signifies there are no error messages while validating expectedMaps from all nodes
	 */
	@Override
	public boolean isValid() {
		return isValid;
	}

	/**
	 * ExpectedMaps are valid if there are no error messages recorded while
	 * expectedMaps from all nodes
	 */
	private boolean validateExpectedMaps(){
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
					}else{
						checkHandleRejectedStatus(key, baseValue, compareValue, i);
					}
					if(baseValue.isErrored() && compareValue.isErrored()){
						checkErrorCause(key, compareValue, i);
					}
				}
			}
		}
		if(errorMessages.size() == 0)
			isValid = true;

		return isValid;
	}

	private void checkHandleRejectedStatus(MapKey key, ExpectedValue baseValue, ExpectedValue compareValue, int i) {
		TransactionState baseType = baseValue.getLatestHandledStatus().getTransactionState();
		TransactionState compareType = compareValue.getLatestHandledStatus().getTransactionState();
		TransactionType baseHistoryType = baseValue.getHistoryHandledStatus().getTransactionType();
		TransactionType compareHistoryType = compareValue.getHistoryHandledStatus().getTransactionType();

		if((baseType.equals(TransactionState.HANDLE_REJECTED) &&
				!(baseHistoryType.equals(TransactionType.Delete) ||
						baseHistoryType.equals(TransactionType.Expire)))){
			addError("ExpectedValue of Key "+key+" has the latestHandledStatus as "
					+baseType + ".But, the HistoryHandledStatus is not Deleted/Expired." +
					" It is"+baseHistoryType);
		}

		if(compareType.equals(TransactionState.HANDLE_REJECTED) &&
				!(compareHistoryType.equals(TransactionType.Delete) ||
						compareHistoryType.equals(TransactionType.Expire))){
				addError("ExpectedValue of Key "+key+" has the latestHandledStatus as "
						+compareType + ".But, the HistoryHandledStatus is not Deleted/Expired." +
						" It is"+compareHistoryType);
			}
	}

	/**
	 * If the KeySet size of maps differ logs error with the missing keys
	 */
	void checkMissingKeys(Set<MapKey> baseKeySet, Set<MapKey> compareKeySet, int nodeNum) {

		String missingKeysInCompare = baseKeySet.
				stream().
				filter(x -> !compareKeySet.contains(x)).
				map(key -> key.toString()).
				sorted().
				collect(Collectors.joining(","));
		if(!missingKeysInCompare.isEmpty()) {
			addError("KeySet of the expectedMap of node " + nodeNum +
					" doesn't match with expectedMap of node 0. " +
					"Missing keys: " + missingKeysInCompare);
		}

		String missingKeysInBase = compareKeySet.
				stream().
				filter(x -> !baseKeySet.contains(x)).
				map(key -> key.toString()).
				sorted().
				collect(Collectors.joining(","));;
		if(!missingKeysInBase.isEmpty()){
			addError("KeySet of the expectedMap of node 0 doesn't match with expectedMap of node " +nodeNum +
					". Missing keys: " + missingKeysInBase);
		}
	}

	/**
	 * Build a String message to be used in compareValues()
	 * @param key
	 * @param base
	 * @param other
	 * @param nodeNum
	 * @param fieldName
	 * @return
	 */
	static String buildFieldMissMatchMsg(final MapKey key, final Object base,
			final Object other, final int nodeNum, final String fieldName) {
		return String.format("Entity: %s has field %s mismatched. node0: %s; node%d: %s",
				key, fieldName, base, nodeNum, other);
	}

	/**
	 * If two ExpectedValues doesn't match checks all the fields of expectedValues
	 * and logs which fields mismatch
	 */
	private void compareValues(MapKey key, ExpectedValue ev1, ExpectedValue ev2, int nodeNum){
		if (Objects.equals(ev1.getEntityType(), ev2.getEntityType())) {
			addError(buildFieldMissMatchMsg(key, ev1.getEntityType(),
					ev2.getEntityType(), nodeNum, "EntityType"));
		}

		if (Objects.equals(ev1.isErrored(), ev2.isErrored())) {
			addError(buildFieldMissMatchMsg(key, ev1.isErrored(),
					ev2.isErrored(), nodeNum, "isErrored"));
		}

		if (Objects.equals(ev1.getHash(), ev2.getHash())) {
			addError(buildFieldMissMatchMsg(key, ev1.getHash(),
					ev2.getHash(), nodeNum, "getHash"));
		}

		if (Objects.equals(ev1.getLatestHandledStatus(), ev2.getLatestHandledStatus())) {
			addError(buildFieldMissMatchMsg(key, ev1.getLatestHandledStatus(),
					ev2.getLatestHandledStatus(), nodeNum, "latestHandledStatus"));
		}

		if (Objects.equals(ev1.getHistoryHandledStatus(), ev2.getHistoryHandledStatus())) {
			addError(buildFieldMissMatchMsg(key, ev1.getHistoryHandledStatus(),
					ev2.getHistoryHandledStatus(), nodeNum, "historyHandledStatus"));
		}
	}

	/**
	 * If isErrored flag is set to true on an ExpectedValue in expectedMap, it means some error
	 * occurred during the experiment. Checks the causes for error.
	 */
	private void checkErrorCause(MapKey key, ExpectedValue ev2, int nodeNum) {
		LifecycleStatus latestHandleStatus = ev2.getLatestHandledStatus();
		LifecycleStatus latestSubmitStatus = ev2.getLatestSubmitStatus();
		switch (latestHandleStatus.getTransactionState()) {
			case  INVALID_SIG:
				addError("Signature is not valid for Entity "+ key +" while performing operation "
						+ latestHandleStatus.getTransactionType() + " on Node "+ nodeNum);
				break;
			case  HANDLE_FAILED:
				addError("Entity "+ key + "on Node "+ nodeNum + "has Error. Please look at the log for more details");
				break;
			case  HANDLE_REJECTED:
				addError("Operation "+latestHandleStatus.getTransactionType()+ " on Entity "+ key
						+ "in Node "+ nodeNum+ " failed as entity already exists");
				break;
			case  HANDLE_ENTITY_TYPE_MISMATCH:
				addError("Operation "+ latestHandleStatus.getTransactionType()+
						"failed as it is performed on wrong entity type"+ ev2.getEntityType());
				break;
			case  SUBMISSION_FAILED:
				addError("Operation "+ latestSubmitStatus.getTransactionType()+
						"failed to get successfully submitted on node "+ nodeNum + "for entity " +key);
				break;
			default:
		}
	}
}
