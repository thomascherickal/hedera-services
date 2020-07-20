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

import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogParser;
import com.swirlds.regression.logs.services.HAPIClientLogParser;
import com.swirlds.regression.validators.HapiClientData;
import com.swirlds.regression.validators.NodeData;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.swirlds.regression.RegressionUtilities.HGCAA_LOG_FILENAME;
import static com.swirlds.regression.RegressionUtilities.OUTPUT_LOG_FILENAME;
import static com.swirlds.regression.RegressionUtilities.PRIVATE_IP_ADDRESS_FILE;
import static com.swirlds.regression.RegressionUtilities.PUBLIC_IP_ADDRESS_FILE;
import static com.swirlds.regression.RegressionUtilities.QUERY_LOG_FILENAME;
import static com.swirlds.regression.utils.FileUtils.getInputStream;

/**
 * ExperimentServicesHelper is helper class for services regression related functionality
 */
public class ExperimentServicesHelper {
	private static final Logger log = LogManager.getLogger(ExperimentServicesHelper.class);
	private static final Marker MARKER = MarkerManager.getMarker("SERVICES_REGRESSION");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	/**
	 * Services regression related constants
	 */
	public static final String HEDERA_NODE_DIR = "/hedera-node/";
	public static final String HEDERA_TEST_CLIENT_DIR = "/test-clients/";
	public static final String SUITE_RUNNER_JAR = "SuiteRunner.jar";
	public static final long STARTING_CRYPTO_ACCOUNT = 3L;
	private static String publicIPStringForServices = null;
	private static String testSuites;
	private static String hederaServicesRepoPath;
	private static String ciPropertiesMap;

	private ArrayList<SSHService> testClientNodes;
	private ArrayList<SSHService> sshNodes;
	private Experiment experiment;
	private static TestConfig testConfig;

	public ExperimentServicesHelper(Experiment experiment) {
		this.experiment = experiment;
		this.testClientNodes = experiment.getTestClientNodes();
		this.sshNodes = experiment.getSSHNodes();
		this.testConfig = experiment.getTestConfig();
	}

	public ExperimentServicesHelper() {
	}

	public static ArrayList<String> getServicesFiles(boolean isTestClient, File keyFile) {
		if (isTestClient) {
			return getRsyncTestClientFiles(keyFile);
		}
		return getServicesRsyncFiles(keyFile);
	}

	/**
	 * Start Browser and HederaNode.jar. When they start running , start SuiteRunner.jar to run Services regression
	 */
	public void startServicesRegression() {
		// start Browser and HGCApp
		startHGCApp();
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Once the HGCApp jar starts running , run test clients
		startSuiteRunner();
	}

	/**
	 * Get list of files to send based on if it is a test client node or server node when running services-regression
	 *
	 * @param isTestClientNode
	 * @param pemFile
	 * @return
	 */
	public Collection<File> getListOfFilesToSend(boolean isTestClientNode, File pemFile) {
		//If it is services-regression based on the type of node (test-client node or server node)
		// upload necessary files
		if (isTestClientNode) {
			return getServicesClientFilesToUpload(pemFile);
		} else {
			return getServicesFilesToUpload(pemFile);
		}
	}

	/**
	 * Start SuiteRunner JAR to run services-regression
	 */
	private void startSuiteRunner() {
		experiment.threadPoolService(testClientNodes.stream().<Runnable>map(node -> () -> {
			node.execTestClientWithProcessID(experiment.getJVMOptionsString());
			log.info(MARKER, "node:{} SuiteRunner.jar started.", node.getIpAddress());
		}).collect(Collectors.toList()));
	}

	/**
	 * Start Browser and corresponding HederaNode.jar, if services regression is enabled
	 */
	private void startHGCApp() {
		experiment.threadPoolService(sshNodes.stream().<Runnable>map(node -> () -> {
			// Sometimes the environment variable in application properties is set to DEV.
			// Modify it automatically to PROD when running regression on AWS
			node.modifyEnvToProd();
			node.execHGCAppWithProcessID(experiment.getJVMOptionsString());
			log.info(MARKER, "node:{} Browser started.", node.getIpAddress());
		}).collect(Collectors.toList()));
	}

	/**
	 * Send suite runner jar and other needed files to test client nodes
	 *
	 * @param addedFiles
	 */
	public void sendTarToTestClientNodes(ArrayList<File> addedFiles) {
		final SSHService firstTestClientNode = testClientNodes.get(0);
		log.info(MARKER, "TestClientNode {}, {}", testClientNodes.size(), firstTestClientNode.toString());
		experiment.calculateNodeMemoryProfile(firstTestClientNode);
		Collection<File> filesToSend = getListOfFilesToSend(true, new File(experiment.getPEMFile()));
		experiment.sendTarToNode(firstTestClientNode, filesToSend);

		// Get list of address of other N-1 nodes test client nodes
		List<String> testClientIpAddresses = IntStream.range(1, testClientNodes.size()).mapToObj(
				i -> testClientNodes.get(i).getIpAddress()).collect(Collectors.toList());
		firstTestClientNode.rsyncTo(addedFiles,
				new File(testConfig.getLog4j2File()), testClientIpAddresses, true);
		log.info(MARKER, "upload to test client nodes has finished");
	}

	/**
	 * Download necessary files that need to be validated from test client nodes
	 * Downloaded files will be placed in folder appended with "TestClient"
	 */
	void scpFromTestClientNode() {
		experiment.threadPoolService(IntStream.range(0, testClientNodes.size())
				.<Runnable>mapToObj(i -> () -> {
					SSHService node = testClientNodes.get(i);
					boolean success = false;
					while (!success)
						success =
								node.scpFromTestClient(getExperimentResultsFolderForTestClientNode(i));
				}).collect(Collectors.toList()));
	}

	/**
	 * Read all the test suites that are mentioned in testConfig
	 *
	 * @param testConfig
	 * @return
	 */
	public static void setTestSuites(TestConfig testConfig) {
		testSuites = StringUtils.join(testConfig.getTestSuites(), " ");
		if (testConfig.isPerformanceRun()) {
			String[] propertiesMap = testSuites.split("\\s+");
			testSuites = propertiesMap[0];
			ciPropertiesMap = propertiesMap[1];
		}
	}

	/**
	 * Returns testSuites that should be run for Services Regression
	 *
	 * @return
	 */
	public static String getTestSuites() {
		return testSuites;
	}

	public static String getPublicIPStringForServices() {
		return publicIPStringForServices;
	}

	public static void setPublicIPStringForServices(String ipAddress) {
		publicIPStringForServices = ipAddress;
	}

	/**
	 * When running services regression, get list of files that should be uploaded on test client
	 * nodes to run necessary test suites
	 *
	 * @param keyFile
	 * @return
	 */
	private Collection<File> getServicesClientFilesToUpload(File keyFile) {
		Collection<File> returnIterator = new ArrayList<>();
		String hederaTestClientDir = getHederaServicesRepoPath() + HEDERA_TEST_CLIENT_DIR;
		returnIterator.add(new File(hederaTestClientDir + "target/" + SUITE_RUNNER_JAR));
		returnIterator.add(new File(hederaTestClientDir + "src/main/resource/"));
		returnIterator.add(new File(hederaTestClientDir + "system-files/"));
		returnIterator.add(new File(hederaTestClientDir + "remote-system-files/"));
		returnIterator.add(new File(hederaTestClientDir + "testfiles/"));

		returnIterator.add(keyFile);
		return returnIterator;
	}

	/**
	 * To run services regression , get list of files that should be uploaded to one of the server nodes
	 *
	 * @param keyFile
	 * @return
	 */
	private Collection<File> getServicesFilesToUpload(File keyFile) {
		Collection<File> returnIterator = new ArrayList<>();
		String hederaNodeDir = hederaServicesRepoPath + HEDERA_NODE_DIR;
		returnIterator.add(new File(hederaNodeDir + "data/apps/"));
		returnIterator.add(new File(hederaNodeDir + "data/backup/"));
		returnIterator.add(new File(hederaNodeDir + "data/keys/"));
		returnIterator.add(new File(hederaNodeDir + "data/lib/"));
		returnIterator.add(new File(hederaNodeDir + "data/repos/"));
		returnIterator.add(new File(hederaNodeDir + "data/config/"));
		returnIterator.add(new File(hederaNodeDir + "data/onboard/"));
		returnIterator.add(new File("hedera.crt"));
		returnIterator.add(new File("hedera.key"));
		returnIterator.add(new File("log4j2-services-regression.xml"));
		returnIterator.add(new File(PRIVATE_IP_ADDRESS_FILE));
		returnIterator.add(new File(PUBLIC_IP_ADDRESS_FILE));
		returnIterator.add(keyFile);
		returnIterator.add(new File(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.CONFIG_FILE));
		returnIterator.add(new File(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.SETTINGS_FILE));
		return returnIterator;
	}

	/**
	 * when running services regression, get list of files from the Tar placed on one of the server nodes,
	 * that should be rsynced to other server nodes
	 *
	 * @param keyFile
	 * @return
	 */
	public static ArrayList<String> getServicesRsyncFiles(File keyFile) {
		ArrayList<String> returnIterator = new ArrayList<>();
		returnIterator.add("data/");
		returnIterator.add("data/apps/");
		returnIterator.add("data/apps/**");
		returnIterator.add("data/backup/");
		returnIterator.add("data/backup/**");
		returnIterator.add("data/keys/");
		returnIterator.add("data/keys/**");
		returnIterator.add("data/lib/");
		returnIterator.add("data/lib/**");
		returnIterator.add("data/repos/");
		returnIterator.add("data/repos/**");
		returnIterator.add("data/config/");
		returnIterator.add("data/config/**");
		returnIterator.add("data/onboard");
		returnIterator.add("data/onboard/**");
		returnIterator.add("privateAddresses.txt");
		returnIterator.add("publicAddresses.txt");
		// TODO add functionality to generate these two files every-time test is run
		//  For now they are in the base directory of regression
		returnIterator.add("hedera.crt");
		returnIterator.add("hedera.key");
		returnIterator.add(keyFile.getName());
		returnIterator.add(RegressionUtilities.CONFIG_FILE);
		returnIterator.add(RegressionUtilities.SETTINGS_FILE);
		returnIterator.add("log4j2-services-regression.xml");
		return returnIterator;
	}

	/**
	 * When running services regression, get list of files in the Tar that should be rsynced between test client
	 * nodes to run test, after Tar is placed on one test client node
	 *
	 * @param keyFile
	 * @return
	 */
	public static ArrayList<String> getRsyncTestClientFiles(File keyFile) {
		ArrayList<String> returnIterator = new ArrayList<>();
		returnIterator.add(SUITE_RUNNER_JAR);
		returnIterator.add("resource/");
		returnIterator.add("resource/**");
		returnIterator.add("system-files/");
		returnIterator.add("system-files/**");
		returnIterator.add("remote-system-files/");
		returnIterator.add("remote-system-files/**");
		returnIterator.add("testfiles/");
		returnIterator.add("testfiles/**");
		returnIterator.add(keyFile.getName());
		return returnIterator;
	}

	/**
	 * Get list of files that should be downloaded from test client nodes when running services regression
	 *
	 * @return
	 */
	protected static ArrayList<String> getServicesFilesToDownload() {
		ArrayList<String> returnIterator = new ArrayList<>();
		returnIterator.add("*.log");
		return returnIterator;
	}

	public static String getHederaServicesRepoPath() {
		return hederaServicesRepoPath;
	}

	public static void setHederaServicesRepoPath(String hederaServicesRepoPath) {
		ExperimentServicesHelper.hederaServicesRepoPath = hederaServicesRepoPath;
	}

	public static String getCiPropertiesMap() {
		if (testConfig.isPerformanceRun()) {
			return "CI_PROPERTIES_MAP=" + ciPropertiesMap;
		}
		return "";
	}

	public void setCiPropertiesMap(String ciPropertiesMap) {
		ExperimentServicesHelper.ciPropertiesMap = ciPropertiesMap;
	}

	List<HapiClientData> loadTestClientNodeData(ArrayList<SSHService> testClientNodes) {
		int numberOfTestClientNodes = getNumberOfTestClientNodes(testClientNodes);
		List<HapiClientData> testClientData = new ArrayList<>();
		for (int i = 0; i < numberOfTestClientNodes; i++) {
			String outputLogFileName = getExperimentResultsFolderForTestClientNode(i)
					+ OUTPUT_LOG_FILENAME;

			InputStream logInput = getInputStream(outputLogFileName);

			LogReader logReader = null;
			if (logInput != null) {
				logReader = LogReader.createReader(new HAPIClientLogParser(), logInput);
			}

			testClientData.add(new HapiClientData(logReader));
		}
		return testClientData;
	}

	List<NodeData> loadHederaNodeHGCAAData(ArrayList<SSHService> nodes) {
		List<NodeData> servicesNodesData = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			String hgcaaLogFileName = getExperimentResultsFolderForHederaNode(i) +
					HGCAA_LOG_FILENAME;
			String queryLogFileName = getExperimentResultsFolderForHederaNode(i) +
					QUERY_LOG_FILENAME;
			InputStream logInput = getInputStream(hgcaaLogFileName);
			InputStream queryInput = getInputStream(queryLogFileName);
			SequenceInputStream combinedLogInput = new SequenceInputStream(logInput, queryInput);

			LogReader logReader = LogReader.createReader(PlatformLogParser.createParser(1), combinedLogInput);

			servicesNodesData.add(new NodeData(logReader));
		}
		return servicesNodesData;
	}

	int getNumberOfTestClientNodes(ArrayList<SSHService> testClientNodes) {
		int numberOfTestClientNodes;
		if (experiment.getRegConfig().getLocal() != null) {
			numberOfTestClientNodes = experiment.getRegConfig().getLocal().getNumberOfNodes();
		} else if (testClientNodes == null || testClientNodes.isEmpty()) {
			return 0;
		} else {
			numberOfTestClientNodes = testClientNodes.size();
		}
		return numberOfTestClientNodes;
	}

	/**
	 * Experiment folder name for the logs downloaded from test client nodes
	 *
	 * @param nodeNumber
	 * @return
	 */
	String getExperimentResultsFolderForTestClientNode(final int nodeNumber) {
		return experiment.getExperimentLocalFileHelper().getExperimentFolder() + "node000" + nodeNumber +
				"-TestClient/";
	}

	/**
	 * Experiment folder name for the logs downloaded from hedera nodes
	 *
	 * @param nodeNumber
	 * @return
	 */
	String getExperimentResultsFolderForHederaNode(final int nodeNumber) {
		return experiment.getExperimentLocalFileHelper().getExperimentFolder() + "node000" + nodeNumber + "/";
	}
}
