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

import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartValidatorTest {

	@Test
	void validateReconnectLogs() throws IOException {
		List<NodeData> nodeData = loadNodeData("logs/RestartBlob/Success");
		NodeValidator validator = new RestartValidator(nodeData, null);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}
		assertTrue(validator.getErrorMessages().isEmpty());
		assertEquals(true, validator.isValid());
	}

	/**
	 * In the logs, each node freezes once, doesn't match expected 3
	 * @throws IOException
	 */
	@Test
	void vadidateDynamicRestartLogsNegative() throws IOException {
		TestConfig testConfig = ValidatorTestUtil.loadTestConfig("configs/testFCMFreezeBlobCfg.json");
		assertNotNull(testConfig.getFreezeConfig());
		List<NodeData> nodeData = loadNodeData("logs/DynamicRestartBlob/FreezeFreqNotMatch");
		NodeValidator validator = new RestartValidator(nodeData, testConfig);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}

		assertEquals(4, validator.getErrorMessages().size());
		for (String msg : validator.getErrorMessages()) {
			assertTrue(msg.contains("froze 1 times, didn't match expected frequency of freeze: 3"));
		}
		assertEquals(false, validator.isValid());
	}

	/**
	 * the test passed successfully
	 * @throws IOException
	 */
	@Test
	void vadidateDynamicRestartLogsPositive() throws IOException {
		TestConfig testConfig = ValidatorTestUtil.loadTestConfig("configs/testFCMFreezeBlobCfg.json");
		assertNotNull(testConfig.getFreezeConfig());
		List<NodeData> nodeData = loadNodeData("logs/DynamicRestartBlob/Success");
		NodeValidator validator = new RestartValidator(nodeData, testConfig);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}

		assertTrue(validator.getErrorMessages().isEmpty());
		assertEquals(true, validator.isValid());
	}

	/**
	 * the test should pass successfully
	 * there is one line in PlatformTesting2.csv contains only one `,` character, CsvParserV1 should treat it as an empty line
	 * @throws IOException
	 */
	@Test
	void csvParserTest() throws IOException {
		TestConfig testConfig = ValidatorTestUtil.loadTestConfig("configs/testFCMFreezeBlobCfg.json");
		assertNotNull(testConfig.getFreezeConfig());
		List<NodeData> nodeData = loadNodeData("logs/DynamicRestartBlob/csvParserTest");
		NodeValidator validator = new RestartValidator(nodeData, testConfig);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}

		assertTrue(validator.getErrorMessages().isEmpty());
		assertEquals(true, validator.isValid());
	}

	public static List<NodeData> loadNodeData(String directory) {
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0;i < 4 ; i++) {
			String logFileName = String.format("%s/node%04d/swirlds.log", directory, i);
			InputStream logInput =
					RestartValidatorTest.class.getClassLoader().getResourceAsStream(logFileName);

			String csvFileName = "PlatformTesting" + i + ".csv";
			String csvFilePath = String.format("%s/node%04d/%s", directory, i, csvFileName);
			InputStream csvInput = RestartValidatorTest.class.getClassLoader().getResourceAsStream(csvFilePath);

			LogReader logReader = null;
			if (logInput != null) {
				logReader = LogReader.createReader(PlatformLogParser.createParser(1), logInput);
			}
			CsvReader csvReader = null;
			if (csvInput != null) {
				csvReader = CsvReader.createReader(1, csvInput);
			}
			nodeData.add(new NodeData(logReader, csvReader));
			System.out.println("loaded for node" + i);
		}
		return nodeData;
	}
}