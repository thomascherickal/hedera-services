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

import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
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
import java.util.LinkedList;
import java.util.List;

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
	static final String KEY_FILE_LOCATION = ".\\keys\\my-key.pem";

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
		String expectedResult = "./" + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION + RegressionUtilities.TAR_NAME;
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
		String expectedResult = "./" + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION + RegressionUtilities.TAR_NAME;
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

		ssh.executeCmd("chmod 400 ~/"+RegressionUtilities.REMOTE_EXPERIMENT_LOCATION +"/my-key.pem");
		ssh.executeCmd("ssh -i ~/"+RegressionUtilities.REMOTE_EXPERIMENT_LOCATION+"/my-key.pem ubuntu@13.56.18.18 'mkdir remoteExperiment'" );
		startTime = System.nanoTime();
		ssh.rsyncTo(null, "13.56.18.18", null);
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
		long expectedMemory = 161169;
		long nodeMemory = ssh.checkTotalMemoryOnNode();

		assertTrue(expectedMemory == nodeMemory);
	}

}
