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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static com.swirlds.regression.validators.RestartValidatorTest.loadNodeData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/reconnectFCM/1_restart_no_state_on_disk",
			"logs/reconnectFCM/2_restart_with_state_on_disk",
			"logs/reconnectFCM/3_disable_enable_network"
	})
	void validateReconnectLogs(String testDir) throws IOException {
		System.out.println("Dir: " + testDir);
		List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "ReconnectStats", 1);
		NodeValidator validator = new ReconnectValidator(nodeData);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println("INFO: " + msg);
		}
		for (String msg : validator.getErrorMessages()) {
			System.out.println("ERROR: " + msg);
		}
		assertEquals(true, validator.isValid());
	}

	@Test
	public void csvValidatorForNodeKillReconnect() throws IOException {
		List<NodeData> nodeData = loadNodeData("logs/PTD-NodeKillReconnect");
		NodeValidator validator = new ReconnectValidator(nodeData);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}
		for (String msg : validator.getErrorMessages()) {
			System.out.println(msg);
		}
		assertEquals(true, validator.isValid());
		assertEquals(0, validator.getErrorMessages().size());
	}

	/**
	 * node0 and node3 miss swirlds.log file
	 */
	@Test
	public void nodeLogIsNullTest() {
		List<NodeData> nodeData = loadNodeData("logs/PTD-MissLog03");
		ReconnectValidator validator = new ReconnectValidator(nodeData);
		assertFalse(validator.nodeLogIsNull(nodeData.get(1).getLogReader(), 1));
		assertFalse(validator.nodeLogIsNull(nodeData.get(2).getLogReader(), 2));

		assertTrue(validator.nodeLogIsNull(nodeData.get(0).getLogReader(), 0));
		assertTrue(validator.nodeLogIsNull(nodeData.get(3).getLogReader(), 3));

		assertEquals(false, validator.isValid());
		assertEquals(2, validator.getErrorMessages().size());
	}

	/**
	 * node0 and node2 miss csv file
	 */
	@Test
	public void nodeCsvIsNullTest() {
		List<NodeData> nodeData = loadNodeData("logs/PTD-MissCsv02");
		ReconnectValidator validator = new ReconnectValidator(nodeData);
		assertFalse(validator.nodeCsvIsNull(nodeData.get(1).getCsvReader(), 1));
		assertFalse(validator.nodeCsvIsNull(nodeData.get(3).getCsvReader(), 3));

		assertTrue(validator.nodeCsvIsNull(nodeData.get(0).getCsvReader(), 0));
		assertTrue(validator.nodeCsvIsNull(nodeData.get(2).getCsvReader(), 2));

		assertEquals(false, validator.isValid());
		assertEquals(2, validator.getErrorMessages().size());
	}

}