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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HAPIClientValidatorTest {
	private static final String testDir = "src/test/resources/logs/ServicesRegression/ServicesDemo/";
	private static final int numberOfTestClients = 1;

	@Test
	void validateLogsPositiveTest() throws IOException {
		HAPIClientValidator validator = new HAPIClientValidator(
				ValidatorTestUtil.loadTestClientNodeData(testDir, numberOfTestClients));
		validator.validate();
		System.out.println("LOGS: " + testDir);
		System.out.println(validator.concatAllMessages());
	}
}
