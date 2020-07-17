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

package com.swirlds.regression;

import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.validators.StreamType;
import com.swirlds.regression.logs.services.HAPIClientLogParser;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.validators.NodeData;
import com.swirlds.regression.validators.StreamingServerData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swirlds.regression.RegressionUtilities.HGCAA_LOG_FILENAME;
import static com.swirlds.regression.RegressionUtilities.QUERY_LOG_FILENAME;
import static com.swirlds.regression.RegressionUtilities.OUTPUT_LOG_FILENAME;
import static com.swirlds.regression.RegressionUtilities.RESULTS_FOLDER;
import static com.swirlds.regression.RegressionUtilities.getResultsFolder;
import static com.swirlds.regression.utils.FileUtils.getInputStream;
import static com.swirlds.regression.validators.LifecycleValidator.EXPECTED_MAP_ZIP;
import static com.swirlds.regression.validators.RecoverStateValidator.EVENT_MATCH_LOG_NAME;
import static com.swirlds.regression.validators.StreamingServerValidator.buildFinalHashFileName;
import static com.swirlds.regression.validators.StreamingServerValidator.buildShaListFileName;
import static com.swirlds.regression.validators.StreamingServerValidator.buildSigListFileName;

/**
 * ExperimentFileHelper is in responsible for processing local files/Paths related to an Experiment,
 * such as loading data from files
 */
public class ExperimentLocalFileHelper {
	private static final Logger log = LogManager.getLogger(ExperimentLocalFileHelper.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private RegressionConfig regConfig;
	private TestConfig testConfig;
	private ZonedDateTime experimentTime;

	public ExperimentLocalFileHelper(final RegressionConfig regConfig,
			final TestConfig testConfig) {
		this.regConfig = regConfig;
		this.testConfig = testConfig;
	}

	void setExperimentTime(final ZonedDateTime experimentTime) {
		this.experimentTime = experimentTime;
	}

	String getExperimentResultsFolderForNode(final int nodeNumber) {
		return getExperimentFolder() + "node000" + nodeNumber + "/";
	}

	/**
	 * Experiment folder name for the logs downloaded from test client nodes
	 *
	 * @param nodeNumber
	 * @return
	 */
	String getExperimentResultsFolderForTestClientNode(final int nodeNumber) {
		return getExperimentFolder() + "node000" + nodeNumber + "-TestClient/";
	}

	/**
	 * Experiment folder name for the logs downloaded from hedera nodes
	 *
	 * @param nodeNumber
	 * @return
	 */
	String getExperimentResultsFolderForHederaNode(final int nodeNumber) {
		return getExperimentFolder() + "node000" + nodeNumber + "/";
	}

	String getExperimentFolder() {
		String folderName = regConfig.getName() + "/" + testConfig.getName();
		return RESULTS_FOLDER + "/" + getResultsFolder(experimentTime,
				folderName) + "/";
	}

	/**
	 * Load ExpectedMap file path for all nodes into a Map
	 */
	Map<Integer, String> loadExpectedMapPaths() {
		final Map<Integer, String> expectedMapPaths = new HashMap<>();
		for (int i = 0; i < regConfig.getTotalNumberOfNodes(); i++) {
			final String expectedMapPath = getExperimentResultsFolderForNode(i) + EXPECTED_MAP_ZIP;
			if (!new File(expectedMapPath).exists()) {
				log.error(MARKER, "ExpectedMap doesn't exist for validation in Node {}", i);
				return null;
			}
			expectedMapPaths.put(i, expectedMapPath);
		}
		return expectedMapPaths;
	}

	List<StreamingServerData> loadStreamingServerData(final StreamType streamType) {
		final List<StreamingServerData> nodeData = new ArrayList<>();
		for (int i = 0; i < regConfig.getEventFilesWriters(); i++) {
			final String experimentResultsFolder = getExperimentResultsFolderForNode(i);
			final String extension = streamType.getExtension();

			final String shaFileName = experimentResultsFolder +
					buildFinalHashFileName(extension);
			final String shaEventFileName = experimentResultsFolder + buildShaListFileName(extension);
			final String eventSigFileName = experimentResultsFolder + buildSigListFileName(extension);
			final String recoverEventMatchFileName = experimentResultsFolder + EVENT_MATCH_LOG_NAME;

			InputStream recoverEventLogStream = getInputStream(recoverEventMatchFileName);
			nodeData.add(new StreamingServerData(getInputStream(eventSigFileName), getInputStream(shaFileName),
					getInputStream(shaEventFileName), recoverEventLogStream, streamType));
		}
		return nodeData;
	}

	/**
	 * @return an array of result folder path strings
	 */
	String[] getResultFolders() {
		final String[] results = new String[regConfig.getTotalNumberOfNodes()];
		for (int nodeIndex = 0; nodeIndex < results.length; nodeIndex++) {
			final String resultFolder = getExperimentResultsFolderForNode(nodeIndex);
			if (!new File(resultFolder).exists()) {
				log.error(MARKER, "Result doesn't exist for validation in Node {}", nodeIndex);
				return null;
			}
			results[nodeIndex] = resultFolder;
		}
		return results;
	}

	List<NodeData> loadTestClientNodeData(ArrayList<SSHService> testClientNodes) {
		int numberOfTestClientNodes = getNumberOfTestClientNodes(testClientNodes);
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0; i < numberOfTestClientNodes; i++) {
			String outputLogFileName = getExperimentResultsFolderForTestClientNode(i)
					+ OUTPUT_LOG_FILENAME;

			InputStream logInput = getInputStream(outputLogFileName);

			LogReader logReader = null;
			if (logInput != null) {
				logReader = LogReader.createReader(new HAPIClientLogParser(), logInput);
			}

			nodeData.add(new NodeData(logReader));
		}
		return nodeData;
	}

	List<NodeData> loadHederaNodeHGCAAData(ArrayList<SSHService> nodes) {
		int numberOfTestClientNodes = getNumberOfTestClientNodes(nodes);
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0; i< numberOfTestClientNodes ; i++) {
			String hgcaaLogFileName = getExperimentResultsFolderForHederaNode(i)+
					HGCAA_LOG_FILENAME;
			String queryLogFileName = getExperimentResultsFolderForHederaNode(i) +
					QUERY_LOG_FILENAME;
			InputStream logInput = getInputStream(hgcaaLogFileName);
			InputStream queryInput = getInputStream(queryLogFileName);
			SequenceInputStream combinedLogInput = new SequenceInputStream(logInput, queryInput);

			LogReader logReader = LogReader.createReader(new HAPIClientLogParser(), combinedLogInput);

			nodeData.add(new NodeData(logReader));
			if (nodeData.size() == 0) {
				throw new RuntimeException("Cannot find hgcaa log file : " + hgcaaLogFileName
						+ "Cannot find queries log file : " + queryLogFileName);
			}
		}
		return nodeData;
	}

	int getNumberOfTestClientNodes(ArrayList<SSHService> testClientNodes){
		int numberOfTestClientNodes;
		if (regConfig.getLocal() != null) {
			numberOfTestClientNodes = regConfig.getLocal().getNumberOfNodes();
		} else if (testClientNodes == null || testClientNodes.isEmpty()) {
			return 0;
		} else {
			numberOfTestClientNodes = testClientNodes.size();
		}
		return numberOfTestClientNodes;
	}
}
