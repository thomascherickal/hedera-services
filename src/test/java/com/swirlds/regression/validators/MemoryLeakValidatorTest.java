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

import com.swirlds.regression.utils.FileUtils;
import com.swirlds.test.framework.TestComponentTags;
import com.swirlds.test.framework.TestTypeTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swirlds.regression.RegressionUtilities.GC_LOG_GZ_FILE;
import static com.swirlds.regression.RegressionUtilities.GC_LOG_ZIP_FILE;
import static com.swirlds.regression.validators.MemoryLeakValidator.FAULT_FIELD_MSG;
import static com.swirlds.regression.validators.MemoryLeakValidator.GCEASY_URL;
import static com.swirlds.regression.validators.MemoryLeakValidator.GC_API_KEY_PATH;
import static com.swirlds.regression.validators.MemoryLeakValidator.PROBLEM_FIELD_MSG;
import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemoryLeakValidatorTest {

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Find GC logs in a given folder")
	public void getGCLogsTest() {
		String folder = getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/zipTest").getPath();
		File[] files = MemoryLeakValidator.getGCLogs(folder);;
		assertEquals(3, files.length);
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Zip GC logs in a given folder into one file")
	public void zipTest() throws IOException {
		String folder = getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/zipTest/").getPath();
		File[] files = MemoryLeakValidator.getGCLogs(folder);
		String zipPath = folder + GC_LOG_ZIP_FILE;
		File zipFile = new File(zipPath);
		zipFile.deleteOnExit();
		assertFalse(zipFile.exists());
		FileUtils.zip(files, zipFile);
		assertTrue(zipFile.exists());
		assertTrue(zipFile.canRead());
		zipFile.delete();
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("compress GC logs in a given folder into one .tar.gz file")
	public void tarGZTest() throws IOException {
		String folder = getClass().getClassLoader().getResource("logs/MemoryLeak/singleFile/zipTest/").getPath();
		File[] files = MemoryLeakValidator.getGCLogs(folder);
		File tarFile = new File(folder + GC_LOG_GZ_FILE);
		tarFile.deleteOnExit();
		assertFalse(tarFile.exists());
		FileUtils.generateTarGZFile(tarFile, Arrays.asList(files));
		assertTrue(tarFile.exists());
		assertTrue(tarFile.canRead());
		tarFile.delete();
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("NegativeTest: Check GC Events in a GC log file")
	public void hasGCEvents_Negative_Test() {
		String file = getClassLoader().getResource("logs/MemoryLeak/singleFile/gc-NoGCEvent.log").getPath();
		assertFalse(MemoryLeakValidator.hasGCEvents(new File(file)));
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("PositiveTest: Check GC Events in a GC log file")
	public void hasGCEvents_Positive_Test() {
		String file = getClassLoader().getResource("logs/MemoryLeak/singleFile/gc-GCEvent.log").getPath();
		assertTrue(MemoryLeakValidator.hasGCEvents(new File(file)));
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Validate a GC log file which has problem in the report")
	public void checkGCFile_HasProblemTest() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.getErrorMessages().stream()
				.anyMatch((str) -> (str.contains(PROBLEM_FIELD_MSG))));
		assertFalse(memoryLeakValidator.isValid());
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Validate a GC log zip file which has not any problem in the report")
	public void checkGCFile_SUCCESS_Test() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gcLog.zip";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.isValid());
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Validate a GC log .gz file which has not any problem in the report")
	public void checkGCFile_GZ_Test() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gc-GCEvent.tar.gz";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.isValid());
	}

	public void testMethod(int n) {
		int m = 6;
		assert n < m;
		System.out.println("n");
	}

	@Test
	public void testAssert() {
		testMethod(1);
		testMethod(10);
	}

	/**
	 * if not provide GC_API_KEY, the response would contain: "fault":{"reason":"apiKey is missing"}
	 */
	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Validate a GC log file when not providing a GC_API_KEY")
	public void checkGCFile_Fault_Test() throws Exception {
		String file = "logs/MemoryLeak/singleFile/gc-MemoryLeak-95mins.log.zip";
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(buildGCLogMap(Arrays.asList(file)),
				new URL(GCEASY_URL));
		memoryLeakValidator.validate();
		assertTrue(memoryLeakValidator.getWarningMessages().stream()
				.anyMatch((str) -> (str.contains(FAULT_FIELD_MSG))));
		assertTrue(memoryLeakValidator.isValid());
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Validate 4 GC log files which don't have `problem` in response")
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
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Show Response Code")
	public void showResponseTest() {
		MemoryLeakValidator memoryLeakValidator = new MemoryLeakValidator(new HashMap<>());
		memoryLeakValidator.showResponseCode(HttpURLConnection.HTTP_OK);
		assertTrue(memoryLeakValidator.getWarningMessages().isEmpty());
		assertEquals(1, memoryLeakValidator.getInfoMessages().size());
	}

	/**
	 * Build a map whose keys are nodeIds, and values are GC log files for testing
	 * @param filePaths
	 * @return
	 * @throws Exception
	 */
	Map<Integer, File> buildGCLogMap(final List<String> filePaths) throws Exception {
		Map<Integer, File> map = new HashMap<>();
		for (int i = 0; i < filePaths.size(); i++) {
			map.put(i, new File(getClassLoader().getResource(filePaths.get(i)).toURI()));
		}
		return map;
	}
}
