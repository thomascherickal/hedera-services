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

package com.swirlds.regression.validators;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PtdValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/PTD-FCM1K-success",
			"logs/PTD-slow-hashing",
			"logs/PTD-FCM1K-MemLow",
			"logs/PTD-FCM1K-TotalMemTooHigh",
			"logs/PTD-FCM1K-Check-No-Pause",
			"logs/PTD-FCM1K-Check-Pause",
			"logs/PTD-FCM1K-No-Check-No-Pause",
			"logs/PTD-FCM1K-No-Check-Pause"
	})
	void validatePtdLogs(String testDir) throws IOException {
		List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "PlatformTesting", 1);
		NodeValidator validator = new PtdValidator(nodeData);
		validator.validate();
		System.out.println("LOGS: " + testDir);
		System.out.println(validator.concatAllMessages());
		assertEquals(true, validator.isValid());
	}
}