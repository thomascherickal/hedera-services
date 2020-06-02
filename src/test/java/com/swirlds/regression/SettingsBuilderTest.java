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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsBuilderTest {
	private static TestConfig testConfig;
	private static RegressionConfig regConfig;
	private static SettingsBuilder settingsBuilder;

	private static ArrayList<String> SETTINGS_HEADER = new ArrayList<>();
	private static ArrayList<String> SETTINGS_APP = new ArrayList<>();
	private static ArrayList<String> SETTINGS_LOCAL_ADDRESS = new ArrayList<>();

	private static ArrayList<String> SETTINGS_REMOTE_ADDRESS = new ArrayList<>();

	private static int totalNodes;
	private static boolean isLocal;

	//	@BeforeAll -- doesn't work with maven
	public static void init() {
		if(regConfig == null || testConfig == null) {

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URI regressionCfg = null;
		URI testCfg = null;
		try {
			regressionCfg = classloader.getResource("configs/localRegressionCfg.json").toURI();
			RegressionConfig regCfg = RegressionUtilities.importRegressionConfig(regressionCfg);
			testCfg = classloader.getResource(regCfg.getExperiments().get(0)).toURI();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		regConfig = RegressionUtilities.importRegressionConfig(regressionCfg);
		testConfig = RegressionUtilities.importExperimentConfig(testCfg);

		SETTINGS_HEADER.add(
				"#######################################################################################\n"
						+ "# Each line is setting name then value, separated by a comma. There must not be a\n"
						+ "# comma in the value, not even escaped or within quotes. The settings can be in any\n "
						+ "# order, with whitespace, and with comments on the lines. For booleans, a value\n"
						+ "# is considered false if it starts with one of {F, f, N, n} or is exactly 0.\n"
						+ "# All other values are true. \n"
						+ "#######################################################################################\n");
		SETTINGS_APP.add("app, PlatformTestingDemo.jar, FCM1K.json");
		SETTINGS_LOCAL_ADDRESS.add(
				"address, restart0, restart0, 1, 127.0.0.1, 40124, 127.0.0.1, 40124, , localhost, 60051");
		SETTINGS_LOCAL_ADDRESS.add(
				"address, restart1, restart1, 1, 127.0.0.1, 40125, 127.0.0.1, 40125, , localhost, 60052");
		SETTINGS_LOCAL_ADDRESS.add("address, restart2, restart2, 1, 127.0.0.1, 40126, 127.0.0.1, 40126");
		SETTINGS_LOCAL_ADDRESS.add("address, restart3, restart3, 1, 127.0.0.1, 40127, 127.0.0.1, 40127");

		// For future use
		// SETTINGS_REMOTE_ADDRESS.add("address, restart0, restart0, 1, 127.0.0.1, 40124, 127.0.0.1, 40124");
	}

}

	//	@BeforeEach - doesn't work with maven
	public void initializeSettingBuilder() {

		settingsBuilder = new SettingsBuilder(testConfig);
		assertNotNull(settingsBuilder);
	}

	@Test
	@DisplayName("Test config File building")
	public void testExportSettingFile() {
		init();
		initializeSettingBuilder();
		//TODO - add better checking than just if the files was written
		assertTrue(settingsBuilder.exportSettingsFile());
	}

	@Test
	@DisplayName("Test config File building with additional setting")
	public void testExportNodeSpecificSettingFile() {
		init();
		initializeSettingBuilder();
		ArrayList<String> extraSetting = new ArrayList<>();
		extraSetting.add("enableEventStreaming, true");
		//TODO - add better checking than just if the files was written
		assertTrue(settingsBuilder.exportNodeSpecificSettingsFile(extraSetting));
	}

	@Test
	@DisplayName("Test config File building with multiple additional settings")
	public void testExportNodeSpecificSettingFileWithMultipleSettings() {
		init();
		initializeSettingBuilder();
		ArrayList<String> extraSetting = new ArrayList<>();
		extraSetting.add("SomeNewSetting, true");
		extraSetting.add("aNewSettingWeNeed, 73.4");
		//TODO - add better checking than just if the files was written
		assertTrue(settingsBuilder.exportNodeSpecificSettingsFile(extraSetting));
	}

	private void checkLines(ArrayList<String> firstArr, ArrayList<String> secondArr) {
		assertEquals(firstArr.size(), secondArr.size());
		for (int i = 0; i < firstArr.size(); i++) {
			assertEquals(firstArr.get(i), secondArr.get(i));
		}
	}
}
