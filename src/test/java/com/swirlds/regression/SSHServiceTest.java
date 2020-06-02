/*
 * (c) 2016-2019 Swirlds, Inc.
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

import com.swirlds.regression.jsonConfigs.AppConfig;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.validators.ValidatorTestUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.swirlds.regression.RegressionUtilities.CONFIG_FILE;
import static com.swirlds.regression.RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
import static com.swirlds.regression.RegressionUtilities.WRITE_FILE_DIRECTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SSHServiceTest {
	private static final Logger log = LogManager.getLogger(SSHServiceTest.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	static final String USER = "ubuntu";
	static final String IPADDRESS = "18.222.31.205";
	static final String KEY_FILE_LOCATION = "./keys/my-key.pem";

	static final String BAD_USER = "ec2USER";
	static final String BAD_IPADDRESS = "3.16.38.224";
	static final String BAD_KEY_FILE_LOCATION = "sdk\\testing\\my-key";

	private SSHService Connect(String user, String ip, String keyFileLocation){
		File keyFile = new File(keyFileLocation);
		SSHService ssh = null;
		try {
			ssh = new SSHService(user, ip, keyFile);
			ssh.buildSession();
		} catch (SocketException e){
			System.out.println("ssh to node failed!");
			e.printStackTrace();
		}
		return ssh;
	}

	/************************************************************
	 * Connection Tests                                         *
	 ***********************************************************/
	@Test
	@DisplayName("test connection")
	void TestConnection(){
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		assertTrue(ssh.isConnected());
	}

//	@Test
	@DisplayName("Test Connection with Bad IP")
	void TestConnectionWithBadIP(){
		SSHService ssh = Connect(USER, BAD_IPADDRESS, KEY_FILE_LOCATION);
		assertFalse(ssh.isConnected());
	}

//	@Test
	@DisplayName("Test Connection with Bad KeyFile")
	void TestConnectionWithBapKeyFile(){
		boolean threwFileNotFound = false;
		SSHService ssh = Connect(USER, IPADDRESS, BAD_KEY_FILE_LOCATION);
		assertFalse(ssh.isConnected());
	}

//	@Test
	@DisplayName("Test Connection with Bad User")
	void TestConnectionWithBadUSER(){
		SSHService ssh = Connect(BAD_USER, IPADDRESS, KEY_FILE_LOCATION);
		assertFalse(ssh.isConnected());
	}

//	@Test
	@DisplayName("Test Connection with Bad Everything")
	void TestConnectionWithNothingGood() {
		SSHService ssh = Connect(BAD_USER, BAD_IPADDRESS, BAD_KEY_FILE_LOCATION);
		assertFalse(ssh.isConnected());
	}

	/*****************************************************************
	 * SCP Tests                                                     *
	 ****************************************************************/

	//@Test
	@DisplayName("Test scpTo")
	void testScpTo(){
		ArrayList<String> checkExtensions = new ArrayList<>();
		checkExtensions.add("*.tar.gz");
		String expectedResult = "./" + REMOTE_EXPERIMENT_LOCATION + RegressionUtilities.TAR_NAME;
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		assertNotNull(ssh);
		ssh.scpTo(null);
		ArrayList<String> returnedFiles = (ArrayList)ssh.getListOfFiles(checkExtensions);
		assertEquals(1, returnedFiles.size());
		assertEquals(expectedResult, returnedFiles.get(0));
		ssh.executeCmd("rm -rfv" + expectedResult);
	}

	//@Test
	@DisplayName("Test getListOfFiles with single value")
	void testGetListOfFilesSingle(){
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		assertNotNull(ssh);
		ArrayList<String> tempExt = new ArrayList<>();
		tempExt.add("*.jar");
		Collection<String> returnList = ssh.getListOfFiles(tempExt);
		assertEquals(85, returnList.size());

	}

	//@Test
	@DisplayName("Test getListOfFiles with two values")
	void testGetListOfFilesDouble(){
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		assertNotNull(ssh);
		ArrayList<String> tempExt = new ArrayList<>();
		tempExt.add("*.jar");
		tempExt.add("*.sh");
		Collection<String> returnList = ssh.getListOfFiles(tempExt);
		assertEquals(90, returnList.size());
	}

	void testGetListOfFilesWithNothing() {}


	// @Test
	@DisplayName("Test scpFrom")
	void testSCPFromWithAddedFiles(){
		File keyFile = new File(KEY_FILE_LOCATION);
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		assertNotNull(ssh);
		ArrayList<String> tempExt = new ArrayList<>();
		tempExt.add("*.jar");
		tempExt.add("*.sh");
		ssh.createNewTar(RegressionUtilities.getSDKFilesToUpload(keyFile,new File("log4j2.xml"), null));
		ArrayList<File> tarball = new ArrayList<>();
		tarball.add(new File(RegressionUtilities.TAR_NAME));
		ssh.scpToSpecificFiles(tarball);
		ssh.extractTar();

//		ssh.scpFrom("", 0,ZonedDateTime.now(ZoneOffset.ofHours(0)), tempExt);
	}

	void testSCPFromWithNonExistentFile() {}
	void testSCPFrom() {}

	//@Test
	@DisplayName("Test rsyncTo")
	void testRsyncTo(){

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URI regressionCfg = null;
		URI testCfg = null;
		try {
			regressionCfg = classloader.getResource("configs/localRegressionCfg.json").toURI();
			RegressionConfig regCfg = RegressionUtilities.importRegressionConfig(regressionCfg);
			testCfg = classloader.getResource(regCfg.getExperiments().get(0)).toURI();
		} catch (URISyntaxException e){
			e.printStackTrace();
		}

		SettingsBuilder settings = new SettingsBuilder(RegressionUtilities.importExperimentConfig(testCfg));
		settings.exportSettingsFile();


		ArrayList<String> checkExtensions = new ArrayList<>();
		checkExtensions.add("*.tar.gz");
		String expectedResult = "./" + REMOTE_EXPERIMENT_LOCATION + RegressionUtilities.TAR_NAME;
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		ssh.buildSession();
		Collection<File> filesToSend = RegressionUtilities.getSDKFilesToUpload(new File(KEY_FILE_LOCATION),new File("log4j2.xml"), null);
		File oldTarFile = new File(RegressionUtilities.TAR_NAME);
		if(oldTarFile.exists()) {
			oldTarFile.delete();
		}
		ssh.createNewTar(filesToSend);
		ArrayList<File> tarball = new ArrayList<>();
		tarball.add(new File(RegressionUtilities.TAR_NAME));
		long startTime = System.nanoTime();
		ssh.scpToSpecificFiles(tarball);
		ssh.extractTar();
		long endTime = System.nanoTime();
		log.trace(MARKER, "upload of tar took:{} seconds",(endTime - startTime)/ 1000000000 );

		ssh.executeCmd("chmod 400 ~/"+ REMOTE_EXPERIMENT_LOCATION +"/my-key.pem");
		ssh.executeCmd("ssh -i ~/"+ REMOTE_EXPERIMENT_LOCATION+"/my-key.pem ubuntu@13.56.18.18 'mkdir remoteExperiment'" );
		startTime = System.nanoTime();
		ssh.rsyncTo(null, null, Collections.singletonList("13.56.18.18"));
		endTime = System.nanoTime();
		log.trace(MARKER, "rsync took:{} seconds", (endTime - startTime)/ 1000000000 );
		//ssh.executeCmd("rm -rfv" + expectedResult);
	}

	//@Test
	void copyFromS3() throws URISyntaxException {
		TestConfig testConfig = RegressionUtilities.importExperimentConfig(
				Thread.currentThread().getContextClassLoader().getResource("configs/testSavedStateCfg.json").toURI()
		);

		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);

		ssh.copyS3ToInstance(
				testConfig.getStartSavedState().getLocation(),
				RegressionUtilities.getRemoteSavedStatePath(
						testConfig.getStartSavedState().getMainClass(),
						0,
						testConfig.getStartSavedState().getSwirldName(),
						testConfig.getStartSavedState().getRound()
				)
		);
	}

	//@Test
	void testScp() throws IOException {
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);

		List<String> l = new LinkedList<>();
		l.add("gitLog.log");
		l.add("regression.sh");
		//l.add("smth");
		ssh.scpFilesToRemoteDir(l, "this/that/");
	}

	@Test
	@DisplayName("Get total amount of memory on node")
	void testMemoryTotalOnNode(){
		SSHService ssh = Connect(USER, IPADDRESS, KEY_FILE_LOCATION);
		String expectedMemory = "161169M";
		String nodeMemory = ssh.checkTotalMemoryOnNode();

		assertTrue(expectedMemory.equals(nodeMemory));
	}

	@Test
	@DisplayName("Get count of specified Msg")
	void testCountSpecifiedMsgEach() throws IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		final String logFilePath = classloader.getResource("logs/DynamicRestartBlob/swirlds-00.log").getFile();

		// upload the file to aws node
		final SSHService ssh = Connect(USER, "18.216.159.19", KEY_FILE_LOCATION);
		ssh.scpFilesToRemoteDir(List.of(logFilePath), REMOTE_EXPERIMENT_LOCATION);

		final String fileName = REMOTE_EXPERIMENT_LOCATION + "/swirlds-00.log";
		final String occur6 = "Platform status changed to: MAINTENANCE";
		final String occur8 = "Platform status changed to: ACTIVE";
		final String occur0 = "NONE Exist";
		final String occur12 = "MAINTENANCE";
		List<String> list = List.of(occur6, occur8, occur0, occur12);
		Map<String, Integer> map = ssh.countSpecifiedMsgEach(list, fileName);

		// since 3 of the following strings exist in the log file
		// the map size should be 3
		assertEquals(3, map.size());
		assertTrue(8 == map.getOrDefault(occur8, 0));
		assertTrue(0 == map.getOrDefault(occur0, 0));
		assertTrue(6 == map.getOrDefault(occur6, 0));
		assertTrue(12 == map.getOrDefault(occur12, 0));

		// countSpecifiedMsg should return 6 + 8 + 12
		assertTrue(26 == ssh.countSpecifiedMsg(list, fileName));

		// since none of the following strings exist in the log file
		// the map size should be 0
		List<String> nonExists = List.of("NONE Exist", "0 Exist", "RANDOMSTRING");
		assertTrue(0 == ssh.countSpecifiedMsgEach(nonExists, fileName).size());


		final String nonExistFile = REMOTE_EXPERIMENT_LOCATION + "/nonExist.log";
		assertTrue(-1 == ssh.countSpecifiedMsg(list, nonExistFile));
		assertNull(ssh.countSpecifiedMsgEach(list, nonExistFile));
	}

	@Test
	public void updateConfigTest() throws Exception {
		final SSHService currentNode = Connect(USER, "18.216.159.19", KEY_FILE_LOCATION);
		TestConfig testConfig = ValidatorTestUtil.loadTestConfig("configs/testFCMFreezeBlobCfg.json");
		RegressionConfig regressionConfig = RegressionUtilities.importRegressionConfig("configs/AwsRegressionCfgFreezeBlob.json");
		ConfigBuilder configBuilder = new ConfigBuilder(regressionConfig, testConfig);
		List<String> publicIPs = List.of("3.21.241.155", "52.14.218.64",
				"13.58.198.82", "13.59.213.101");
		List<String> privateIPs = List.of("172.31.16.221", "172.31.20.94",
				"172.31.18.221", "172.31.31.67");
		configBuilder.addIPAddresses(publicIPs, privateIPs);
		configBuilder.exportConfigFile();

		final String freezeAppName = "FCM1KFreezeBlob.json";
		final String postAppName = "FCMBlob1K.json";

		// the configBuilder's app should contain freezeAppName
		assertTrue(configBuilder.getLines().stream().
				anyMatch(s -> s.contains(freezeAppName)));

		// set the non-freeze config
		AppConfig postApp = testConfig.getFreezeConfig().getPostFreezeApp();
		configBuilder.setApp(postApp);
		// build new config file
		configBuilder.exportConfigFile();
		// upload new config file
		ArrayList<File> newUploads = new ArrayList<>();
		newUploads.add(new File(WRITE_FILE_DIRECTORY + CONFIG_FILE));
		currentNode.scpToSpecificFiles(newUploads);

		// the node's new config file should contain postAppName
		assertEquals(1, currentNode.countSpecifiedMsg(
				List.of(postAppName), REMOTE_EXPERIMENT_LOCATION + CONFIG_FILE));
	}
}
