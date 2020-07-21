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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogParser;
import com.swirlds.regression.logs.services.HAPIClientLogParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swirlds.regression.RegressionUtilities.*;
import static com.swirlds.regression.utils.FileUtils.getInputStream;
import static com.swirlds.regression.validators.RecoverStateValidator.EVENT_MATCH_LOG_NAME;
import static com.swirlds.regression.validators.StreamType.EVENT;
import static com.swirlds.regression.validators.StreamType.RECORD;

public abstract class ValidatorTestUtil {

	public static Map<Integer, String> loadExpectedMapPaths(String directory) {
		Map<Integer, String> expectedMapPaths = new HashMap<>();

		for (int i = 0; i < 4; i++) {
			final String expectedMapPath = String.format("%s/node%04d/" + LifecycleValidator.EXPECTED_MAP_ZIP,
					directory, i);
			if (new File(expectedMapPath).exists()) {
				expectedMapPaths.put(i, expectedMapPath);
			} else {
				throw new RuntimeException(" expectedMap in node " + i + " doesn't exist");
			}
		}

		if (expectedMapPaths.size() == 0) {
			throw new RuntimeException("Cannot find expectedMap files in: " + directory);
		}
		return expectedMapPaths;
	}

	public static List<NodeData> loadNodeData(String directory, String csvName, int logVersion) {
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0; ; i++) {

			String logFileName = String.format("%s/node%04d/swirlds.log", directory, i);
			InputStream logInput =
					ValidatorTestUtil.class.getClassLoader().getResourceAsStream(logFileName);
			if (logInput == null) {
				break;
			}

			int csvVersion = 1;
			String csvFileName = String.format("%s/node%04d/%s%d.csv", directory, i, csvName, i);
			InputStream csvInput =
					ValidatorTestUtil.class.getClassLoader().getResourceAsStream(csvFileName);

			nodeData.add(
					new NodeData(
							LogReader.createReader(PlatformLogParser.createParser(logVersion), logInput),
							CsvReader.createReader(csvVersion, csvInput)
					));
		}
		if (nodeData.size() == 0) {
			throw new RuntimeException("Cannot find log files in: " + directory);
		}
		return nodeData;
	}

	public static List<StreamingServerData> loadStreamingServerData(final String directory,
			final StreamType streamType) throws RuntimeException {
		List<StreamingServerData> data = new ArrayList<>();
		for (int i = 0; ; i++) {
			final String finalShaFileName = String.format(
					"%s/node%04d/" + StreamingServerValidator.buildFinalHashFileName(streamType.getExtension()),
					directory, i);
			final String shaListFileName = String.format(
					"%s/node%04d/" + StreamingServerValidator.buildShaListFileName(streamType.getExtension()),
					directory, i);
			final String sigListFileName =
					String.format(
							"%s/node%04d/" + StreamingServerValidator.buildSigListFileName(streamType.getExtension()),
							directory, i);

			final InputStream shaInput = ValidatorTestUtil.class.getClassLoader().getResourceAsStream(finalShaFileName);
			if (shaInput == null) {
				break;
			}

			final InputStream shaListInput = ValidatorTestUtil.class.getClassLoader().getResourceAsStream(
					shaListFileName);
			final InputStream sigListInput = ValidatorTestUtil.class.getClassLoader().getResourceAsStream(
					sigListFileName);

			if (streamType == EVENT) {
				InputStream recoverEventLogStream = ValidatorTestUtil.class.getClassLoader().getResourceAsStream(
						String.format("%s/node%04d/", directory, i) + EVENT_MATCH_LOG_NAME);
				if (recoverEventLogStream != null) {
					data.add(new StreamingServerData(sigListInput, shaInput, shaListInput,
							ValidatorTestUtil.class.getClassLoader().getResourceAsStream(
									String.format("%s/node%04d/", directory, i) + EVENT_MATCH_LOG_NAME), EVENT));
				} else {
					data.add(new StreamingServerData(sigListInput, shaInput, shaListInput, EVENT));
				}
			} else if (streamType == RECORD) {
				data.add(new StreamingServerData(sigListInput, shaInput, shaListInput, RECORD));
			}
		}
		if (data.size() == 0) {
			throw new RuntimeException("Cannot find log files in: " + directory);
		}
		return data;
	}

	/**
	 * load TestConfig from .json file
	 * @param jsonPath
	 * @return
	 * @throws IOException
	 */
	public static TestConfig loadTestConfig(final String jsonPath) throws IOException {
		Path testConfigFileLocation = Paths.get(jsonPath);
		byte[] jsonData = Files.readAllBytes(testConfigFileLocation);
		ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		return objectMapper.readValue(jsonData, TestConfig.class);
	}

	public static List<HapiClientData> loadTestClientNodeData(String directory, int numberOfTestClients) {
		List<HapiClientData> nodeData = new ArrayList<>();
		for (int i = 0; i< numberOfTestClients ; i++) {
			String hapiClientLogFileName = String.format("%s/node%04d-TestClient/"+
					HAPI_CLIENT_COMBINED_FILE_NAME, directory, i);
			InputStream logInput = getInputStream(hapiClientLogFileName);

			LogReader logReader = null;
			if (logInput != null) {
				logReader = LogReader.createReader(new HAPIClientLogParser(), logInput);
			}
			nodeData.add(new HapiClientData(logReader));
		}
		if (nodeData.size() == 0) {
			throw new RuntimeException("Cannot find log files in: " + directory);
		}
		return nodeData;
	}

	public static List<NodeData> loadHederaNodeHGCAAData(String directory, int numberOfTestClientNodes) {
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0; i< numberOfTestClientNodes ; i++) {
			String hgcaaLogFileName = String.format("%s/node%04d/" +
					HGCAA_LOG_FILENAME, directory, i);
			String queryLogFileName = String.format("%s/node%04d/" +
					QUERY_LOG_FILENAME, directory, i);
			InputStream logInput = getInputStream(hgcaaLogFileName);
			InputStream queryInput = getInputStream(queryLogFileName);
			SequenceInputStream combinedLogInput = new SequenceInputStream(logInput, queryInput);

			LogReader logReader = LogReader.createReader(PlatformLogParser.createParser(1), combinedLogInput);

			nodeData.add(new NodeData(logReader));
			if (nodeData.size() == 0) {
				throw new RuntimeException("Cannot find hgcaa log file : " + hgcaaLogFileName
						+ "Cannot find queries log file : " + queryLogFileName);
			}
		}
		return nodeData;
	}
}
