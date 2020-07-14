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

import com.swirlds.regression.validators.services.HAPIClientValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HAPIClientValidatorTest {
	private static final String negativeTestDir = "src/test/resources/logs/ServicesRegression/ServicesDemoNegative/";
	private static final String positiveTestDir = "src/test/resources/logs/ServicesRegression/ServicesDemoPositive/";
	private static final int numberOfTestClients = 1;

	@Test
	void validateLogsNegativeTest() throws IOException {
		HAPIClientValidator validator = new HAPIClientValidator(
				ValidatorTestUtil.loadTestClientNodeData(negativeTestDir, numberOfTestClients));
		validator.validate();
		System.out.println("LOGS: " + negativeTestDir);
		System.out.println(validator.concatAllMessages());
		assertFalse(validator.isValid());
		assertTrue(validator.getErrorMessages().size() > 0);
	}

	@Test
	void validateLogsPositiveTest() throws IOException {
		HAPIClientValidator validator = new HAPIClientValidator(
				ValidatorTestUtil.loadTestClientNodeData(positiveTestDir, numberOfTestClients));
		validator.validate();
		System.out.println("LOGS: " + positiveTestDir);
		System.out.println(validator.concatAllMessages());
		assertTrue(validator.isValid());
		assertEquals(0, validator.getErrorMessages().size());
	}
}
