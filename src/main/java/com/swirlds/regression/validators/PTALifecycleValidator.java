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
import java.util.List;
import java.util.Map;


public class PTALifecycleValidator extends NodeValidator {
	private static Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps;
	private static String expectedMapPath = "/data/expectedmap";
	private static boolean isValid;

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
			boolean isValid = validateExpectedMap();
		}
		isValid = validateExpectedMap();
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	private boolean validateExpectedMap(){
		return false;
	}
	private void deserializeExpectedMaps(int i){
		Map<MapKey, ExpectedValue> map = new HashMap<>();
//			if(new File(s).exists()){
//				map = SaveExpectedMapHandler.deserialize(i);
//			}
			expectedMaps.put(i, map);
		}

}
