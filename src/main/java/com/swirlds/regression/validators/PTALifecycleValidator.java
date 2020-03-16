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
import com.swirlds.demo.platform.fcm.lifecycle.SaveExpectedMapHandler;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class PTALifecycleValidator extends NodeValidator {
	private static Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps;
	private static boolean isValid;

	List<String> errorMessages = new LinkedList<>();

	void addInfo(String msg) {
		infoMessages.add(msg);
	}

	void addWarning(String msg) { warnMessages.add(msg); }

	void addError(String msg) {
		errorMessages.add(msg);
	}

	public PTALifecycleValidator(List<NodeData> nodeData) {
		super(nodeData);
		expectedMaps = new HashMap<>();
		isValid = false;
	}

	@Override
	public void validate() {
		int nodeNum = nodeData.size();
		for (int i = 0; i < nodeNum; i++) {
			deserializeExpectedMaps(i);
		}
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
				if(mapToCompare.containsKey(key) && !baselineMap.get(key).equals(mapToCompare.get(key))){
					compareValues(key, baselineMap.get(key), mapToCompare.get(key), i);
				}
			}
		}
		if(errorMessages.size() == 0)
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
				} else if(!key.equalsAllFields(compareKey)){
					addError("Entity type of keys doesn't match : Nodes : 0, " + nodeNum + " , Key :"+ key);
				}
			}
		}
	}
	private void compareValues(MapKey key, ExpectedValue ev1, ExpectedValue ev2, int nodeNum){
		if(ev1.getHash() != ev2.getHash())
			addError("MapKey:" +key+ "Nodes :0, "+nodeNum+ "hash mismatched");
		if(ev1.getLatestHandledStatus() != ev2.getLatestHandledStatus())
			addError("MapKey:" +key+ "Nodes :0, "+nodeNum+ "latestHandledStatus mismatched");
		if(ev1.getLatestSubmitStatus() != ev2.getLatestSubmitStatus())
			addError("MapKey:" +key+ "Nodes :0, "+nodeNum+ "latestSubmitStatus mismatched");
		if(ev1.getHistoryHandledStatus() != ev2.getHistoryHandledStatus())
			addError("MapKey:" +key+ "Nodes :0, "+nodeNum+ "historyHandledStatus mismatched");
		if(ev1.isErrored() != ev2.isErrored())
			addError("MapKey:" +key+ "Nodes :0, "+nodeNum+ "isErrored mismatched");
	}
	boolean equalMaps(Map<MapKey,ExpectedValue>map1, Map<MapKey,ExpectedValue>map2, int nodeNum) {
		if (map1.size() != map2.size())
			return false;
		for (MapKey key: map1.keySet())
			if (!map1.get(key).equals(map2.get(key)))
				return false;
		return true;
	}

	private void deserializeExpectedMaps(int i){
		Map<MapKey, ExpectedValue> map = new HashMap<>();
			if(new File("ExpectedMap.json").exists()){
				map = SaveExpectedMapHandler.deserialize();
			}
			expectedMaps.put(i, map);
		}

}
