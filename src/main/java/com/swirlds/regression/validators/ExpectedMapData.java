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
import com.swirlds.fcmap.test.pta.MapKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExpectedMap's data that is used to validate in LifecycleValidator
 */
public class ExpectedMapData {
	private Map<Integer, Map<MapKey, ExpectedValue>> expectedMaps;

	public ExpectedMapData() {
		expectedMaps = new ConcurrentHashMap<>();
	}

	public Map<Integer, Map<MapKey, ExpectedValue>> getExpectedMaps() {
		return expectedMaps;
	}
}