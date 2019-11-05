/*
 * (c) 2016-2019 Swirlds, Inc.
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

package com.swirlds.regression.jsonConfigs;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static com.swirlds.regression.jsonConfigs.NodeGroupIdentifier.*;

class TestConfigTest {

	@Test
	void getSavedStateForNode() {
		List<NodeGroupIdentifier> groups = Arrays.asList(ALL, FIRST, LAST, ALL_BUT_LAST, ALL_BUT_FIRST);
		List<SavedState> states = groups.stream()
				.map(ngi -> {
					SavedState ss =  new SavedState();
					ss.setLocation(ngi.toString());
					ss.setNodeIdentifier(ngi);
					return ss;
				})
				.collect(Collectors.toList());

		TestConfig all = new TestConfig();
		all.setStartSavedState(states.get(0));
		assertEquals(ALL.toString(), all.getSavedStateForNode(0,4).getLocation());
		assertEquals(ALL.toString(), all.getSavedStateForNode(3,4).getLocation());

		TestConfig lastDifferent = new TestConfig();
		lastDifferent.setStartSavedStates(Collections.singletonList(states.get(3)));
		assertEquals(ALL_BUT_LAST.toString(), lastDifferent.getSavedStateForNode(2,4).getLocation());
		assertNull(lastDifferent.getSavedStateForNode(3,4));
	}
}