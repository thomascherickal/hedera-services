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

	@Test
	void validateExpectedMapsPositiveTest() {
		PTALifecycleValidator validator = new PTALifecycleValidator(ValidatorTestUtil.loadExpectedMapData(positiveTestDir));
		validator.validate();
		System.out.println("LOGS: " + positiveTestDir);
		System.out.println(validator.concatAllMessages());
		assertEquals(true, validator.isValid());
	}

	@Test
	void validateExpectedMapsErrors() {
		// Modified the node 0's ExpectedMap to have different Entity type for MapKey[0,0,12,Crypto]
		// [0,0,11,Blob],[0,0,17,Blob],[0,0,14,Blob],0,0,15,Blob] has 2 different fields compared to node 0.
		// So that total errors will be 24
		PTALifecycleValidator validator = new PTALifecycleValidator(ValidatorTestUtil.loadExpectedMapData(negativeTestDir));
		validator.validate();
		assertTrue(validator.getErrorMessages().size() > 0);
		assertTrue(validator.getMismatchErrors().size() > 0);
		System.out.println("Error messages :"+validator.getErrorMessages());
		System.out.println("Entity mismatch error messages :"+validator.getMismatchErrors());
		assertTrue(validator.getMismatchErrors().contains("Entity:MapKey[0,0,11,Blob] has the field " +
				"isErrored mismatched for the Nodes :0, 1"));
		assertTrue(validator.getMismatchErrors().get(2).contains("[0,0,17,Blob]"));
		assertTrue(validator.getMismatchErrors().get(4).contains("[0,0,14,Blob]"));
		assertTrue(validator.getMismatchErrors().get(6).contains("[0,0,15,Blob]"));

		assertEquals(24, validator.getMismatchErrors().size());
		assertEquals(3, validator.getErrorMessages().size());
		assertTrue(validator.getErrorMessages().get(0).contains("MapKey[0,0,12,Crypto]"));
		assertTrue(validator.getErrorMessages().get(1).contains("MapKey[0,0,12,Crypto]"));
		assertTrue(validator.getErrorMessages().get(2).contains("MapKey[0,0,12,Crypto]"));
		assertEquals(false, validator.isValid());
	}
}
