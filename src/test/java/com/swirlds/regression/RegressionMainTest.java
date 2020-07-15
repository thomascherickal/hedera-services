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

import com.swirlds.regression.jsonConfigs.CloudConfig;
import com.swirlds.regression.jsonConfigs.CloudConfigTest;
import com.swirlds.regression.jsonConfigs.DBConfig;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegressionMainTest {
	static RegressionMain rm;

	// Helper Functions

	CloudConfig LoadCloudConfig(String fileLocation) {
		try {
			return CloudConfigTest.loadCloudConfig(fileLocation);
		} catch (URISyntaxException | IOException e) {
			System.out.println("Could not open config file: " + fileLocation);
		}
		return null;
	}

	//	@BeforeAll -- doesn't work in maven
	public static void init() {

		if (rm != null) {
			return;
		}

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String regressionCfg = null;
		URI testCfg = null;
		try {

			URI uri = classloader.getResource("configs/localRegressionCfg.json").toURI();
			regressionCfg = Paths.get(uri).toString();

			rm = new RegressionMain(regressionCfg);
		} catch (URISyntaxException e) {
			System.out.println("Could not load regression config");
		}
	}

	@Test
	@DisplayName("is Running from kick off server")
	public void isRunningFromNightlyKickOffServer() {
		init();
		assertFalse(rm.isRunningFromNightlyKickOffServer());
	}

	@Test
	@DisplayName("Run test to instantiate test client nodes and server nodes.")
	//To run this add -Daws.accessKeyId=XXX -Daws.secretKey=XXX in "VMOptions".
	// Also the key name in localServicesRegressionCfg should match the key name in keys folder
	public void isRunningExperiments() {
		try {
			URI uri = Thread.currentThread().getContextClassLoader().
					getResource("configs/localServicesRegressionCfg.json").toURI();
			rm = new RegressionMain(Paths.get(uri).toString());
		} catch (URISyntaxException e) {
			System.out.println("Could not load services regression config");
		}

		final CloudService cloud = rm.setUpCloudService();
		rm.runExperiments(cloud);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//BaseCloud.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//BaseCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//MultiRegionCloud.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//MultiRegionCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//MultiRegionCloudMixed.json"
	})
	@DisplayName("isRequestingUseOfNightlyServer no instances")
	public void testIsRequestingUseOfNightlyServerReturnFalse(String configFileLocation) {
		init();
		CloudConfig cloudCfg = LoadCloudConfig(configFileLocation);
		assertFalse(rm.isRequestingUseOfNightlyServer(cloudCfg));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnTrue//BaseCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnTrue//MultiRegionCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnTrue//MultiRegionCloudMixed.json"
	})
	@DisplayName("isRequestingUseOfNightlyServer no instances")
	public void testIsRequestingUseOfNightlyServerReturnTrue(String configFileLocation) {
		init();
		CloudConfig cloudCfg = LoadCloudConfig(configFileLocation);
		assertTrue(rm.isRequestingUseOfNightlyServer(cloudCfg));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//BaseCloud.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//BaseCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//MultiRegionCloud.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//MultiRegionCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnFalse//MultiRegionCloudMixed.json"
	})
	@DisplayName("isRequestingUseOfNightlyServer no instances")
	public void testIsIllegalUseOfNightlyRunServersFalse(String configFileLocation) {
		init();
		CloudConfig cloudCfg = LoadCloudConfig(configFileLocation);
		assertFalse(rm.isIllegalUseOfNightlyRunServers(cloudCfg));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnTrue//BaseCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnTrue//MultiRegionCloudWithIP.json",
			"configs/RegressionMainTest/IsRequestingUseOfNightlyServerTest/returnTrue//MultiRegionCloudMixed.json"
	})
	@DisplayName("isRequestingUseOfNightlyServer no instances")
	public void testIsIllegalUseOfNightlyRunServersTrue(String configFileLocation) {
		init();
		CloudConfig cloudCfg = LoadCloudConfig(configFileLocation);
		assertTrue(rm.isIllegalUseOfNightlyRunServers(cloudCfg));
	}

	@Test
	@DisplayName("Import Regression config JSON Test")
	public void testImportRegressionConfig() {
		init();
		RegressionConfig rc = rm.getRegConfig();
		DBConfig db = rc.getDb();
		assertEquals("663-restart-validation", rc.getName());
		assertEquals("testing/results", rc.getResult().getURI());
		assertEquals("testing/logs/", rc.getLog().getURI());
		assertEquals("postgresql", db.getType());
		assertEquals("dbregression", db.getLogin());
		assertEquals("localhost", db.getAddress());
		assertEquals(5432, db.getPort());
		assertEquals("stats", db.getDatabase());
//		assertEquals("aFakeToken", rc.getSlack().getToken());
//		assertEquals("regression", rc.getSlack().getChannel());
	}

	@Test
	@DisplayName("Import Node config JSON Test")
	public void testImportNodeConfig() {
	}

	@Test
	@DisplayName("GMT Date Test")
	public void testStartDate() {
		init();
		LocalDateTime baseDate = rm.getDate();
		DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
		System.setProperty("user.timezone", "GMT");
		assertEquals(baseDate.format(dateFormat), rm.getStartDate());
	}

	@Test
	@DisplayName("File Names Test")
	public void testStartDateTime() {
		init();
		LocalDateTime baseDate = rm.getDate();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
		System.setProperty("user.timezone", "GMT");
		assertEquals(baseDate.format(dateFormat), rm.getStartTime());
	}

	@Test
	public void testFileNames() {
		init();
		String startTime = rm.getStartTime();
		String baseFilename = "test " + startTime;
		String logFilename = baseFilename + ".log";
		String errFilename = baseFilename + ".err";
		String logDir = rm.getRegConfig().getLog().getURI() + startTime;

		assertEquals(baseFilename, rm.getBaseFilename());
		assertEquals(logFilename, rm.getLogFilename());
		assertEquals(errFilename, rm.getErrFilename());
		assertEquals(logDir, rm.getLogDir());
	}
}
