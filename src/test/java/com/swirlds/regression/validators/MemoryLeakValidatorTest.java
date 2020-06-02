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

import com.swirlds.regression.jsonConfigs.MemoryLeakCheckConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.swirlds.regression.validators.MemoryLeakValidator.GCEASY_URL;
import static com.swirlds.regression.validators.MemoryLeakValidator.RESPONSE_CODE_OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemoryLeakValidatorTest {

	@Test
	public void checkGCFileTest() throws Exception {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(
				new MemoryLeakCheckConfig(), 4);
		File path = new File(getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip").toURI());
		System.out.println(path);
		memoryLeakValidator.checkGCFile(path, memoryLeakValidator.buildURL());
		assertTrue(memoryLeakValidator.getInfoMessages().contains(RESPONSE_CODE_OK));
	}

	/**
	 * if not provide GC_API_KEY, the response would contain: "fault":{"reason":"apiKey is missing"}
	 * @throws Exception
	 */
	@Test
	public void checkGCFile_Negative_Test() throws Exception {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(
				new MemoryLeakCheckConfig(), 4);
		File path = new File(getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip").toURI());
		System.out.println(path);
		memoryLeakValidator.checkGCFile(path, new URL(GCEASY_URL));
		System.out.println(memoryLeakValidator.getErrorMessages());
		//assertTrue(memoryLeakValidator.getInfoMessages().contains(RESPONSE_CODE_OK));
	}
}
