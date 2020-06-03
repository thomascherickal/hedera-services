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

import com.swirlds.regression.Experiment;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.jsonConfigs.MemoryLeakCheckConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.swirlds.regression.RegressionUtilities.GC_LOG_ZIP_FILE;
import static com.swirlds.regression.validators.MemoryLeakValidator.FAULT_FIELD_MSG;
import static com.swirlds.regression.validators.MemoryLeakValidator.GCEASY_URL;
import static com.swirlds.regression.validators.MemoryLeakValidator.PROBLEM_FIELD_MSG;
import static com.swirlds.regression.validators.MemoryLeakValidator.RESPONSE_CODE_OK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemoryLeakValidatorTest {

	@Test
	public void getGCLogsTest() {
		String folder = getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/zipTest").getPath();
		File[] files = RegressionUtilities.getGCLogs(folder);
		assertTrue(files.length == 3);
	}

	@Test
	public void zipTest() {
		String folder = getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/zipTest/").getPath();
		File[] files = RegressionUtilities.getGCLogs(folder);
		String zipPath = folder + GC_LOG_ZIP_FILE;
		File zipFile = new File(zipPath);
		zipFile.deleteOnExit();
		assertFalse(zipFile.exists());
		RegressionUtilities.zip(files, zipFile);
		assertTrue(zipFile.exists());
		assertTrue(zipFile.canRead());
		zipFile.delete();
	}

	@Test
	public void checkGCFileTest() throws Exception {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(new HashMap<>());
		File path = new File(getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip").toURI());
		System.out.println(path);
		memoryLeakValidator.checkGCFile(path, memoryLeakValidator.buildURL());
		assertTrue(memoryLeakValidator.getInfoMessages().stream()
				.anyMatch((str) -> (str.contains(RESPONSE_CODE_OK))));
		assertTrue(memoryLeakValidator.getInfoMessages().stream()
				.anyMatch((str) -> (str.contains(RESPONSE_CODE_OK))));
		assertTrue(memoryLeakValidator.getErrorMessages().stream()
				.anyMatch((str) -> (str.contains(PROBLEM_FIELD_MSG))));
	}

	/**
	 * if not provide GC_API_KEY, the response would contain: "fault":{"reason":"apiKey is missing"}
	 * @throws Exception
	 */
	@Test
	public void checkGCFile_Negative_Test() throws Exception {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(new HashMap<>());
		File path = new File(getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip").toURI());
		memoryLeakValidator.checkGCFile(path, new URL(GCEASY_URL));
		assertTrue(memoryLeakValidator.getWarningMessages().stream()
				.anyMatch((str) -> (str.contains(FAULT_FIELD_MSG))));
	}
}
