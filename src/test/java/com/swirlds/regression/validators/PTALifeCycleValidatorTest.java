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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// test class for PTA LifecycleValidator
public class PTALifeCycleValidatorTest {

	private static final String positiveTestDir =  "src/test/resources/logs/Lifecycle";
	private static final String negativeTestDir =  "src/test/resources/logs/LifecycleNeg";

	// Positive test where ExpectedMaps of all 4 nodes are the same
	@Test
	void validateExpectedMapsPositiveTest() {
		PTALifecycleValidator validator = new PTALifecycleValidator(ValidatorTestUtil.loadExpectedMapData(positiveTestDir));
		validator.validate();
		System.out.println("LOGS: " + positiveTestDir);
		System.out.println(validator.concatAllMessages());
		assertEquals(0, validator.getErrorMessages().size());
		assertEquals(true, validator.isValid());
	}

	// Test to check errors
	// 	Modified the node 0's ExpectedMap to have different Entity type for MapKey[0,0,12]
	//	[0,0,11],[0,0,17],[0,0,14],[0,0,15] has 2 different fields compared to node 0.
	//	So that total mismatch errors will be 24
	//	[0,0,19,Blob] is missing in node 0 . So, total missing Keys will be 3
	@Test
	void validateExpectedMapsErrors() {
		PTALifecycleValidator validator = new PTALifecycleValidator(ValidatorTestUtil.loadExpectedMapData(negativeTestDir));
		validator.validate();

		assertEquals(30, validator.getErrorMessages().size());

		System.out.println("Error messages : \n"+ String.join("\n",validator.getErrorMessages()));

		assertTrue(validator.getErrorMessages().contains("Entity:MapKey[0,0,11] has the field " +
				"isErrored mismatched for the Nodes :0, 1"));
		assertTrue(validator.getErrorMessages().contains("Entity type of values doesn't match : " +
				"Nodes : 0, 1 , Key :MapKey[0,0,12]"));
		assertTrue(validator.getErrorMessages().contains("KeySet size of Map of node 0 doesn't match with " +
				"Map of node 1. Missing keys :MapKey[0,0,19]"));
		assertTrue(validator.getErrorMessages().contains("Entity:MapKey[0,0,14] has the field " +
				"latestHandledStatus mismatched for the Nodes :0, 1"));
		assertEquals(false, validator.isValid());
	}
}
