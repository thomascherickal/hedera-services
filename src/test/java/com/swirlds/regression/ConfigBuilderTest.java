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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigBuilderTest {
	private static TestConfig testConfig;
	private static RegressionConfig regConfig;
	private static ConfigBuilder configBuilder;

	private static ArrayList<String> CONFIG_HEADER = new ArrayList<>();
	private static ArrayList<String> CONFIG_APP = new ArrayList<>();
	private static ArrayList<String> CONFIG_LOCAL_ADDRESS = new ArrayList<>();
	private static ArrayList<String> CONFIG_LOCAL_ADDRESS_STAKE = new ArrayList<>();

	private static ArrayList<String> CONFIG_REMOTE_ADDRESS = new ArrayList<>();

	//	@BeforeAll - doesn't work with maven
	public static void init() {
		try {
			regConfig = loadRegressionConfig("configs/localRegressionCfg.json");
			testConfig = loadExperimentConfig("configs/testRestartCfg.json");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		if (CONFIG_HEADER.size() == 0) {
			CONFIG_HEADER.add(
					"###############################################################################################\n"
							+ "# Swirlds configuration file, for automatically running multiple instances\n"
							+
							"###############################################################################################\n"
							+ "swirld, 123");
		}
		if (CONFIG_APP.size() == 0) {
			CONFIG_APP.add("app, PlatformTestingDemo.jar, FCM1K.json");
		}
		if (CONFIG_LOCAL_ADDRESS.size() == 0) {
			CONFIG_LOCAL_ADDRESS.add("address, restart0, restart0, 1, 127.0.0.1, 40124, 127.0.0.1, 40124");
			CONFIG_LOCAL_ADDRESS.add("address, restart1, restart1, 1, 127.0.0.1, 40125, 127.0.0.1, 40125");
			CONFIG_LOCAL_ADDRESS.add("address, restart2, restart2, 1, 127.0.0.1, 40126, 127.0.0.1, 40126");
			CONFIG_LOCAL_ADDRESS.add("address, restart3, restart3, 1, 127.0.0.1, 40127, 127.0.0.1, 40127");
		}
		if (CONFIG_LOCAL_ADDRESS_STAKE.size() == 0) {
			CONFIG_LOCAL_ADDRESS_STAKE.add("address, restart0, restart0, 500, 127.0.0.1, 40124, 127.0.0.1, 40124");
			CONFIG_LOCAL_ADDRESS_STAKE.add("address, restart1, restart1, 500, 127.0.0.1, 40125, 127.0.0.1, 40125");
			CONFIG_LOCAL_ADDRESS_STAKE.add("address, restart2, restart2, 500, 127.0.0.1, 40126, 127.0.0.1, 40126");
			CONFIG_LOCAL_ADDRESS_STAKE.add("address, restart3, restart3, 500, 127.0.0.1, 40127, 127.0.0.1, 40127");
		}
		// For future use
		// CONFIG_REMOTE_ADDRESS.add("address, restart0, restart0, 1, 127.0.0.1, 40124, 127.0.0.1, 40124");
	}

	private static RegressionConfig loadRegressionConfig(String filePath) throws URISyntaxException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		return RegressionUtilities.importRegressionConfig(
				classloader.getResource(filePath).toURI());
	}

	private static TestConfig loadExperimentConfig(String filePath) throws URISyntaxException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		return RegressionUtilities.importExperimentConfig(
				classloader.getResource(filePath).toURI());
	}

	//	@BeforeEach - doesn't work with maven
	public void initializeConfig() {
		configBuilder = new ConfigBuilder(regConfig, testConfig);
		assertNotNull(configBuilder);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/AwsRegressionCfg.json",
			"configs/AwsRegressionSlack.json"
	})
	@DisplayName("Test config json file parsing")
	public void testConfigParsing(String configFile) throws URISyntaxException {
		System.out.println();
		loadRegressionConfig(configFile);
	}

	@Test
	@DisplayName("Test config File building")
	public void testExportConfigFile() {
		init();
		initializeConfig();
		assertTrue(configBuilder.exportConfigFile());
		configBuilder = null;
	}

	@Test
	@DisplayName("Test Building File Content")
	public void testBuildFileContent() {
		init();
		initializeConfig();
		configBuilder.buildFileContent();
		ArrayList<String> lines = configBuilder.getLines();
		ArrayList<String> wholeFile = new ArrayList<>();
		wholeFile.addAll(CONFIG_HEADER);
		wholeFile.addAll(CONFIG_APP);
		wholeFile.addAll(CONFIG_LOCAL_ADDRESS);
		checkLines(wholeFile, lines);
		configBuilder = null;
	}

	@Test
	@DisplayName("Test building header for config")
	public void testBuildHeader() {
		init();
		initializeConfig();
		configBuilder.buildHeader();
		ArrayList<String> lines = configBuilder.getLines();
		checkLines(CONFIG_HEADER, lines);
		configBuilder = null;
	}

	@Test
	@DisplayName("Test building application line for config")
	public void testBuildApp() {
		init();
		initializeConfig();
		configBuilder.buildAppString();
		ArrayList<String> lines = configBuilder.getLines();
		checkLines(CONFIG_APP, lines);
		configBuilder = null;
	}

	@Test
	@DisplayName("Test building address lines for config")
	public void testBuildLocalAdddress() {
		init();
		initializeConfig();
		configBuilder.buildAddressStrings();
		ArrayList<String> lines = configBuilder.getLines();
		checkLines(CONFIG_LOCAL_ADDRESS, lines);
		configBuilder = null;
	}

	@Test
	@DisplayName("Test building address lines for config")
	public void testBuildLocalAdddressStake() {
		init();
		initializeConfig();
		configBuilder.setStakes(Collections.nCopies(regConfig.getTotalNumberOfNodes(), 500L));
		configBuilder.buildAddressStrings();
		ArrayList<String> lines = configBuilder.getLines();
		checkLines(CONFIG_LOCAL_ADDRESS_STAKE, lines);
		configBuilder = null;
	}

	private void checkLines(ArrayList<String> firstArr, ArrayList<String> secondArr) {
		assertEquals(firstArr.size(), secondArr.size());
		for (int i = 0; i < firstArr.size(); i++) {
			assertEquals(firstArr.get(i), secondArr.get(i));
		}
	}
}
