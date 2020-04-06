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
import com.swirlds.regression.jsonConfigs.RegionList;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.SWIRLDS_NAME;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ConfigBuilder {
	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private static final String CONFIG_HEADER =
			"###############################################################################################\n"
					+ "# Swirlds configuration file, for automatically running multiple instances\n"
					+
					"###############################################################################################\n"
					+ "swirld, " + SWIRLDS_NAME;
	private static final String LOCALHOST_DEFAULT_IP = "127.0.0.1";
	private static final String SEPERATOR = ", ";
	private static final String CONFIG_PLACEHOLDER = "1";
	private static final String STREAM_SERVER_LOCATION = "localhost";

	private Path outFile = Paths.get(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.CONFIG_FILE);
	private ArrayList<String> lines = new ArrayList<>();
	private List<String> publicIPList;
	private List<String> privateIPList;
	private int startingPort = 40124;
	private int eventFilesWriters = 0;
	private boolean isLocal;
	private int totalNodes = 0;
	private AppConfig app;
	private String nodeNames = "aaaa";
	private List<Long> stakes;

	public ConfigBuilder(RegressionConfig regConfig, TestConfig expConf) {
		isLocal = (regConfig.getLocal() != null);
		if (isLocal) {
			totalNodes = regConfig.getLocal().getNumberOfNodes();
			buildLocalIPLists();
		} else {
			if (regConfig.getCloud() == null) {
				log.error(ERROR, "regression must have 'local' or 'cloud' config");
				System.exit(-1);
			}
			for (RegionList reg : regConfig.getCloud().getRegionList()) {
				totalNodes += reg.getNumberOfNodes();
			}
		}

		this.app = expConf.getApp();
		this.nodeNames = expConf.getName();
		eventFilesWriters = regConfig.getEventFilesWriters();
		// default stakes are 1 for each node
		stakes = Collections.nCopies(totalNodes, 1L);
	}

	private void buildLocalIPLists() {
		publicIPList = new ArrayList<>();
		privateIPList = new ArrayList<>();
		for (int i = 0; i < totalNodes; i++) {
			publicIPList.add(LOCALHOST_DEFAULT_IP);
			privateIPList.add(LOCALHOST_DEFAULT_IP);
		}
	}

	void buildHeader() {
		lines.add(CONFIG_HEADER);
	}

	void buildAppString() {
		StringBuilder appString = new StringBuilder();
		appString.append("app");
		appString.append(SEPERATOR);
		appString.append(app.getJar());
		for (String param : app.getParameterList()) {
			appString.append(SEPERATOR);
			appString.append(param);
		}
		lines.add(appString.toString());
	}

	void buildAddressStrings() {
		for (int i = 0; i < totalNodes; i++) {
			StringBuilder addressString = new StringBuilder();
			addressString.append("address");
			addressString.append(SEPERATOR);
			addressString.append(nodeNames + Integer.toString(i));
			addressString.append(SEPERATOR);
			addressString.append(nodeNames + Integer.toString(i));
			addressString.append(SEPERATOR);
			addressString.append(stakes.get(i));
			addressString.append(SEPERATOR);
			addressString.append(privateIPList.get(i));
			addressString.append(SEPERATOR);
			addressString.append(startingPort + i);
			addressString.append(SEPERATOR);
			addressString.append(publicIPList.get(i));
			addressString.append(SEPERATOR);
			addressString.append(startingPort + i);
			lines.add(addressString.toString());
		}
	}

	void buildFileContent() {
		buildHeader();
		buildAppString();
		buildAddressStrings();
	}

	boolean exportConfigFile() {
		if (this.publicIPList == null || this.privateIPList == null || this.app == null) {
			log.error(MARKER, "A vital variable is null: publicIPList: {}, privateIPList: {}, app: {}",
					(this.publicIPList == null) ? "NULL" : "good", (this.privateIPList == null) ? "NULL" : "good",
					(this.app == null) ? "NULL" : "good");
			return false;
		}

		lines.clear();
		buildFileContent();

		try {
			Files.createDirectories(outFile.getParent());
			if (!Files.exists(outFile)) {
				Files.createFile(outFile);
			}
			Files.write(outFile, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			log.error(ERROR, "Unable to write to config.txt", e);
			return false;
		}
		return true;
	}

	void addIPAddresses(List<String> publicIPs, List<String> privateIPs) {
		publicIPList = publicIPs;
		privateIPList = privateIPs;
	}

	private void moveOldConfigToTempConfig(){
		Path oldConfig = Paths.get("config.txt");
		Path tempConfig = Paths.get("config.bak");
		try {
			Files.move(oldConfig, tempConfig, REPLACE_EXISTING);
		} catch (IOException e){
			log.error(ERROR, "config could not be backed up", e);
		}

	}

	private void moveTempConfigBackToOldConfig(){
		Path oldConfig = Paths.get("config.txt");
		Path tempConfig = Paths.get("config.bak");
		try {
			Files.move(tempConfig, oldConfig, REPLACE_EXISTING);
		} catch (IOException e){
			log.error(ERROR, "config could not be moved back", e);
		}

	}

	private void moveNewConfigToOldConfig(){
		Path oldConfig = Paths.get("config.txt");
		Path newConfig = outFile;
		try {
			Files.move(newConfig, oldConfig, REPLACE_EXISTING);
		} catch (IOException e){
			log.error(ERROR, "new config could not be written to old config", e);
		}
	}

	public void setStartingPort(int startingPort) {
		this.startingPort = startingPort;
	}

	public void setEventFilesWriters(int eventFilesWriters) {
		this.eventFilesWriters = eventFilesWriters;
	}

	public void setLocal(boolean local) {
		isLocal = local;
	}

	public void setTotalNodes(int totalNodes) {
		this.totalNodes = totalNodes;
	}

	public void setApp(AppConfig app) {
		if (app == null) {
			log.error(MARKER, "AppConfig passed in was null");
		}
		this.app = app;
	}

	public void setNodeNames(String nodeNames) {
		this.nodeNames = nodeNames;
	}

	public void setPublicIPList(ArrayList<String> publicIPList) {
		this.publicIPList = publicIPList;
	}

	public void setPrivateIPList(ArrayList<String> privateIPList) {
		this.privateIPList = privateIPList;
	}

	public void setStakes(List<Long> stakes) {
		if (stakes == null) {
			throw new NullPointerException("Argument stakes must not be null!");
		}
		if (stakes.size() != totalNodes) {
			throw new IllegalArgumentException("The list of stakes must be equal to the number of nodes!");
		}
		this.stakes = stakes;
	}

	public ArrayList<String> getLines() {
		return lines;
	}

	public void backupConfigAndMoveTestConfig() {
		moveOldConfigToTempConfig();
		moveNewConfigToOldConfig();
	}

	public void returnOriginalConfig() {
		moveTempConfigBackToOldConfig();
	}
}
