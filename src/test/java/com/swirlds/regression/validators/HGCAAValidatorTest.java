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

import com.swirlds.regression.validators.services.HGCAAValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class HGCAAValidatorTest {
    private static final String negativeTestDir = "src/test/resources/logs/ServicesRegression/ServicesDemoNegative";
    private static final String positiveTestDir = "src/test/resources/logs/ServicesRegression/ServicesDemoPositive";
    private static final int numberOfHederaNodes = 2;

    @Test
    void validateLogsNegativeTest() throws IOException {
        HGCAAValidator validator = new HGCAAValidator(
                ValidatorTestUtil.loadHederaNodeHGCAAData(negativeTestDir, numberOfHederaNodes));
        validator.validate();
        System.out.println("LOGS: " + negativeTestDir);
        System.out.println(validator.concatAllMessages());
        assertFalse(validator.isValid());
        assertTrue(validator.errorMessages.size() > 0);
    }

    @Test
    void validateLogsPositiveTest() throws IOException {
        HGCAAValidator validator = new HGCAAValidator(
                ValidatorTestUtil.loadHederaNodeHGCAAData(positiveTestDir, numberOfHederaNodes));
        validator.validate();
        System.out.println("LOGS: " + positiveTestDir);
        System.out.println(validator.concatAllMessages());
        assertTrue(validator.isValid());
        assertEquals(0, validator.errorMessages.size());
    }
}
