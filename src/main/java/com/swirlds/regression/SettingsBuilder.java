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

import com.swirlds.regression.jsonConfigs.SettingsConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static com.swirlds.regression.Experiment.EXPERIMENT_FREEZE_SECOND_AFTER_STARTUP;
import static com.swirlds.regression.ExperimentServicesHelper.HEDERA_NODE_DIR;
import static com.swirlds.regression.ExperimentServicesHelper.getHederaServicesRepoPath;

public class SettingsBuilder {
	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private static final String SETTINGS_HEADER =
			"#######################################################################################\n"
					+ "# Each line is setting name then value, separated by a comma. There must not be a\n"
					+ "# comma in the value, not even escaped or within quotes. The settings can be in any\n "
					+ "# order, with whitespace, and with comments on the lines. For booleans, a value\n"
					+ "# is considered false if it starts with one of {F, f, N, n} or is exactly 0.\n"
					+ "# All other values are true. \n"
					+ "#######################################################################################\n";
	private static final String SEPERATOR = ", ";

	private HashMap<String, String> settingsMap = new HashMap<>();
	private TestConfig testConfig;
	private String csvFilename = null;


	public SettingsBuilder(TestConfig expConf) {
		this.testConfig = expConf;

		// If it is services-regression read settings.txt file from hedera-services repo
		if (testConfig.isServicesRegression()) {
			String hederaNodeDir = getHederaServicesRepoPath() + HEDERA_NODE_DIR;
			readSettings(hederaNodeDir + RegressionUtilities.SETTINGS_FILE);
		} else {
			readSettings(RegressionUtilities.DEFAULT_SETTINGS_DIR + RegressionUtilities.SETTINGS_FILE);  // read default
		}
		// setting
		readConfigSettings();
		exportSettingsFile();

		// read generated setting
		readSettings(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.SETTINGS_FILE);
	}

	private void readSettings(String filePath) {
		String line = null;
		File defaultSettingsFile = new File(filePath);
		if (!defaultSettingsFile.exists()) {
			return;
		}
		try {
			BufferedReader defaultReader = new BufferedReader(new FileReader(defaultSettingsFile));
			log.info(MARKER, "Dump setting file {}", () -> filePath);
			while ((line = defaultReader.readLine()) != null) {
				if (!line.startsWith("#")) {
					String[] tempLineArray = line.split(SEPERATOR);
					if (tempLineArray.length == 2) {
						settingsMap.put(tempLineArray[0], tempLineArray[1]);
						log.info(MARKER, "Name:{} \t\t: value:{}", tempLineArray[0], tempLineArray[1]);
					}
				}
			}
		} catch (IOException e) {
			log.error(ERROR, "Could not open default settings file.", e);
		}
	}

	private void readConfigSettings() {
		for (SettingsConfig set : testConfig.getSettings()) {
			settingsMap.put(set.getName(), set.getValue());
			log.trace(MARKER, "Name: {}\t\t: value:{}", set.getName(), set.getValue());
		}
	}

	public String getSettingValue(String key) {
		return settingsMap.get(key);
	}

	public void addSetting(String name, String value) {
		settingsMap.put(name, value);
	}

	private ArrayList<String> convertSettingsToStringList() {
		ArrayList<String> returnSettings = new ArrayList<String>();
		settingsMap.forEach((name, value) -> returnSettings.add(name + SEPERATOR + value));
		return returnSettings;
	}

	public void setFreezeTime(int startHour, int startMin, int endHour, int endMin) {
		settingsMap.put("freezeSettings.active", "true");
		settingsMap.put("freezeSettings.startHour", Integer.toString(startHour));
		settingsMap.put("freezeSettings.startMin", Integer.toString(startMin));
		settingsMap.put("freezeSettings.endHour", Integer.toString(endHour));
		settingsMap.put("freezeSettings.endMin", Integer.toString(endMin));
		settingsMap.put("freezeSecondsAfterStartup", Integer.toString(EXPERIMENT_FREEZE_SECOND_AFTER_STARTUP));
	}

	public void disableFreeze() {
		settingsMap.put("freezeSettings.active", "false");
	}

	/**
	 * Exports specific settings for specific node(s). This was designed specificlly for streaming, since some nodes
	 * have
	 * streaming turned on and some don't. This is easily expandable for other features that may need this in the
	 * future.
	 *
	 * @param addedSetting
	 * 		- List of settings and their values to be added only on the export of the setting files.
	 * @return - true if file was created; false otherwise
	 */
	public boolean exportNodeSpecificSettingsFile(ArrayList<String> addedSetting) {
		ArrayList<String> lines = buildFileContent(addedSetting);
		return exportFile(lines);
	}

	/**
	 * build standard settings files for nodes based on setting in the experiment config
	 *
	 * @return true if file was created; false otherwise
	 */
	public boolean exportSettingsFile() {
		ArrayList<String> lines = buildFileContent();
		return exportFile(lines);
	}

	/**
	 * Writes the settings file to set location for upload to nodes based on list of lines passed in
	 *
	 * @param lines
	 * 		- List of lines in the settings files to be exports
	 * @return - true if file was created, false otherwise
	 */
	private boolean exportFile(ArrayList<String> lines) {
		Path outFile = Paths.get(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.SETTINGS_FILE);
		try {
			Files.createDirectories(outFile.getParent());
			if (!Files.exists(outFile)) {
				Files.createFile(outFile);
			}
			Files.write(outFile, lines, RegressionUtilities.STANDARD_CHARSET);
		} catch (IOException e) {
			log.error(ERROR, "Unable to write to settings.txt", e);
			return false;
		}
		return true;
	}

	String buildHeader() {
		return SETTINGS_HEADER;
	}

	/**
	 * Standard function to build the list of settings for the settings file. Used if no additional temporary settings
	 * are needed
	 */
	ArrayList<String> buildFileContent() {
		return buildFileContent(null);
	}

	/**
	 * Function to build the list of settings for the settings file, with temporary settings for a specific node
	 *
	 * @param additionalSetting
	 * 		- list of additional settings needed for a specific node
	 * @return - list of lists to be exported to the settings file.
	 */
	ArrayList<String> buildFileContent(ArrayList<String> additionalSetting) {
		ArrayList<String> returnList = new ArrayList<>();
		returnList.add(buildHeader());
		returnList.addAll(convertSettingsToStringList());
		if (additionalSetting != null && additionalSetting.size() != 0) {
			for (String setting : additionalSetting) {
				returnList.add(setting);
			}
		}
		return returnList;
	}
}