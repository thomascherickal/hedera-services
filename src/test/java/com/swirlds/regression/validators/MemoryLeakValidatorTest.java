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

import com.swirlds.regression.RegressionUtilities;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swirlds.regression.RegressionUtilities.GC_LOG_ZIP_FILE;
import static com.swirlds.regression.validators.MemoryLeakValidator.FAULT_FIELD_MSG;
import static com.swirlds.regression.validators.MemoryLeakValidator.GCEASY_URL;
import static com.swirlds.regression.validators.MemoryLeakValidator.PROBLEM_FIELD_MSG;
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
	public void checkGCFile_HasProblemTest() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.getErrorMessages().stream()
				.anyMatch((str) -> (str.contains(PROBLEM_FIELD_MSG))));
		assertFalse(memoryLeakValidator.isValid());
	}

	@Test
	public void checkGCFile_SUCCESS_Test() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gcLog.zip";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.isValid());
	}

	/**
	 * if not provide GC_API_KEY, the response would contain: "fault":{"reason":"apiKey is missing"}
	 *
	 * @throws Exception
	 */
	@Test
	public void checkGCFile_Fault_Test() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)),
				new URL(GCEASY_URL));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.getWarningMessages().stream()
				.anyMatch((str) -> (str.contains(FAULT_FIELD_MSG))));
		assertTrue(memoryLeakValidator.isValid());
	}

	/**
	 * four GC logs which don't have `problem` in response
	 *
	 * @throws Exception
	 */
	@Test
	public void checkGCFileForNodesTest() throws Exception {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(
				Arrays.asList("logs/MemoryLeak/nodes/gcLog0.zip",
						"logs/MemoryLeak/nodes/gc.log1.zip",
						"logs/MemoryLeak/nodes/gc.log2.zip",
						"logs/MemoryLeak/nodes/gc.log3.zip")));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.isValid());
	}

	@Test
	public void showResponseTest() {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(new HashMap<>());
		memoryLeakValidator.showResponseCode(HttpURLConnection.HTTP_OK);
		assertTrue(memoryLeakValidator.getWarningMessages().isEmpty());
		assertTrue(memoryLeakValidator.getInfoMessages().size() == 1);
	}

	Map<Integer, File> buildGCLogMap(final List<String> filePaths) throws Exception {
		Map<Integer, File> map = new HashMap<>();
		for (int i = 0; i < filePaths.size(); i++) {
			map.put(i, new File(getClass().getClassLoader().getResource(filePaths.get(i)).toURI()));
		}

		return map;
	}
}
