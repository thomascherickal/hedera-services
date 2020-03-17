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
import com.swirlds.demo.platform.fcm.lifecycle.SaveExpectedMapHandler;

import java.io.File;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class PTALifecycleValidator extends Validator {
	private static Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps;
	public static boolean isValid;
	public static final String EXPECTED_MAP = "ExpectedMap.json";

	private List<String> errorMessages = new ArrayList<>();
	private List<String> mismatchErrors = new ArrayList<>();

	void addError(String msg) {
		errorMessages.add(msg);
	}

	void addMismatchError(String msg) {
		mismatchErrors.add(msg);
	}

	public PTALifecycleValidator(ExpectedMapData mapData) {
		expectedMaps = mapData.getExpectedMaps();
		isValid = false;
	}

	public static Map<Integer, Map<MapKey, ExpectedValue>> getExpectedMaps() {
		return expectedMaps;
	}

	public static void setExpectedMaps(
			Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps) {
		PTALifecycleValidator.expectedMaps = expectedMaps;
	}

	@Override
	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public List<String> getMismatchErrors() {
		return mismatchErrors;
	}

	public void setMismatchErrors(List<String> mismatchErrors) {
		this.mismatchErrors = mismatchErrors;
	}


	@Override
	public void validate() {
		isValid = validateExpectedMaps();
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	private boolean validateExpectedMaps(){
		Map<MapKey, ExpectedValue> baselineMap = expectedMaps.get(0);

		for(int i=1; i< expectedMaps.size();i++){
			Map<MapKey, ExpectedValue> mapToCompare = expectedMaps.get(i);

			checkKeySet(baselineMap, mapToCompare, i);

			for(MapKey key : baselineMap.keySet()){
				if(mapToCompare.containsKey(key)){
					ExpectedValue baseValue = baselineMap.get(key);
					ExpectedValue compareValue = mapToCompare.get(key);
					 if(!baseValue.equals(compareValue)){
						compareValues(key, baseValue, compareValue, i);
					 }
					 if(baseValue.isErrored() && compareValue.isErrored()){
					 	checkErrorCause(key, compareValue, i);
					 }
				}
			}
		}
		if(errorMessages.size() == 0 && mismatchErrors.size() == 0)
			isValid = true;

		return isValid;
	}

	private void checkKeySet(Map<MapKey, ExpectedValue> baselineMap, Map<MapKey, ExpectedValue> mapToCompare, int i) {
		Set<MapKey> baseKeySet = baselineMap.keySet();
		Set<MapKey> compareKeySet = mapToCompare.keySet();

		if(baseKeySet.size() != compareKeySet.size()){
			LogMissingkeys(baseKeySet,compareKeySet, i);
		}
		checkEntityType(baselineMap.keySet(), mapToCompare.keySet(), i);
	}

	private void LogMissingkeys(Set<MapKey> baseKeySet, Set<MapKey> compareKeySet, int nodeNum) {

		List<MapKey> missingKeys =baseKeySet.
							stream().
							filter(x -> !compareKeySet.contains(x)).collect(
							Collectors.toList());
		if(missingKeys.size() > 0) {
			addError("KeySet size of Map of node " + nodeNum + " doesn't match with Map of node 0. " +
					"Missing keys :" + missingKeys);
		}
	}

	private void checkEntityType(Set<MapKey> baseKeySet, Set<MapKey> compareKeySet, int nodeNum){
		for(MapKey key : baseKeySet) {
			for (MapKey compareKey : compareKeySet) {
				if (!compareKeySet.contains(key)) {
					addError("Key missing in ExpectedMap of node : " + nodeNum);
				} else if(key.equals(compareKey) && !key.equalsAllFields(compareKey)){
					addError("Entity type of keys doesn't match : Nodes : 0, " + nodeNum + " , Key :"+ key);
				}
			}
		}
	}
	private void compareValues(MapKey key, ExpectedValue ev1, ExpectedValue ev2, int nodeNum){
		if(ev1.isErrored() != ev2.isErrored())
			addMismatchError("Entity:" + key + " has the field isErrored mismatched for the Nodes :0, " + nodeNum);
		if(ev1.getHash()!=null && ev2.getHash()!=null && !ev1.getHash().equals(ev2.getHash()))
			addMismatchError("Entity:" +key+ " has the field Hash mismatched for theNodes :0, "+nodeNum);
		if(ev1.getLatestHandledStatus() !=null && ev2.getLatestHandledStatus() !=null &&
				!ev1.getLatestHandledStatus().getTransactionState().equals(ev2.getLatestHandledStatus().getTransactionState()))
			addMismatchError("Entity:" +key+ " has the field latestHandledStatus mismatched for the Nodes :0, "+nodeNum);
		if(ev1.getHistoryHandledStatus() !=null && ev2.getLatestHandledStatus() !=null &&
				!ev1.getHistoryHandledStatus().getTransactionState().equals(ev2.getHistoryHandledStatus().getTransactionState()))
			addMismatchError("Entity:" +key+ " has the field historyHandledStatus mismatched for the Nodes :0, "+nodeNum);

	}

	private void checkErrorCause(MapKey key, ExpectedValue ev2, int nodeNum) {
		LifecycleStatus latestHandleStatus = ev2.getLatestHandledStatus();
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
						"failed as it is performed on wrong entity type"+ key.getEntityType());
				break;
			default:
		}
	}

	boolean equalMaps(Map<MapKey,ExpectedValue>map1, Map<MapKey,ExpectedValue>map2, int nodeNum) {
		if (map1.size() != map2.size())
			return false;
		for (MapKey key: map1.keySet())
			if (!map1.get(key).equals(map2.get(key)))
				return false;
		return true;
	}
}
