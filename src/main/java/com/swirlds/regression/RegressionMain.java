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

import com.swirlds.regression.experiment.ExperimentSummary;
import com.swirlds.regression.experiment.ExperimentSummaryStorage;
import com.swirlds.regression.jsonConfigs.CloudConfig;
import com.swirlds.regression.jsonConfigs.RegionList;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.slack.SlackNotifier;
import com.swirlds.regression.slack.SlackSummaryMsg;
import com.swirlds.regression.slack.SlackTestMsg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.CLOUD_WAIT_MILLIS;
import static com.swirlds.regression.RegressionUtilities.WAIT_NODES_READY_TIMES;
import static java.lang.Thread.sleep;


public class RegressionMain {
	private static final Logger log = LogManager.getLogger(RegressionMain.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private RegressionConfig regConfig;
	private LocalDateTime date = LocalDateTime.now();
	private String startDate;
	private String startTime;
	private GitInfo git;

	private String baseFilename, logFilename, errFilename, logDir;

	public RegressionMain(String regressionFileLocation) {
		log.info(MARKER, "Starting Regression...");
		System.setProperty("user.timezone", "GMT");
		this.regConfig = RegressionUtilities.importRegressionConfig(regressionFileLocation);
		log.trace(MARKER, "parse Reg Config");


		log.trace(MARKER, "parse Node Config");
		git = new GitInfo();
		git.gitVersionInfo();

		getTestTime();
		setFileNames();
	}

	void RunRegression() {
		if (regConfig.getCloud() != null) {
			//TODO Unit test for null cloud
			if (isIllegalUseOfNightlyRunServers(regConfig.getCloud())) {
				reportErrorToSlack(new Throwable(
								"The servers you requested can only be used in the nightly regression runs. Please " +
										"fix your config file to start up new servers or use different servers than " +
										"these."),
						null);
				return;
			}
			RunCloudExperiment();
		} else {
			/* run local experiments */
			runExperiments(null);
		}
		// shutdown the ThreadPool if there was one created fo regression
		ThreadPool.closeThreadPool();
	}

	private void RunCloudExperiment() {
		final CloudService cloud = setUpCloudService();
			if (cloud == null) {
				reportErrorToSlack(new Throwable("Cloud instances failed to start."), null);
				return;
			}
			//TODO Unit test for system.exit after cloud service set up
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					log.error(ERROR, "Shutdown hook invoked. Destroying cloud instances");
				cloud.destroyInstances();
					log.info(MARKER, "cloud instances destroyed");
				}
			}));

		try {
			runExperiments(cloud);
		} finally {
			// always destroy instances if anything goes wrong
			cloud.destroyInstances();
		}
	}

	boolean isIllegalUseOfNightlyRunServers(CloudConfig cloud) {
		if (!isRunningFromNightlyKickOffServer() && isRequestingUseOfNightlyServer(cloud)) {
			return (true);
		}
		return false;
	}

	boolean isRequestingUseOfNightlyServer(CloudConfig cloudCfg) {
		for (RegionList region : cloudCfg.getRegionList()) {
			if (region.getInstanceList() == null || region.getInstanceList().length == 0) continue;
			for (String serverIP : region.getInstanceList()) {
				for (int i = 0; i < RegressionUtilities.NIGHTLY_REGRESSION_SERVER_LIST.length; i++) {
					if (serverIP.equals(RegressionUtilities.NIGHTLY_REGRESSION_SERVER_LIST[i])) {
						return true;
					}
				}
			}
		}
		return false;
	}

	boolean isRunningFromNightlyKickOffServer() {
		ProcessBuilder processBuilder;
		// if machine running regression is windows it can't be the nightly regression kickoff server
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			return false;
		} else {
			processBuilder = new ProcessBuilder("/sbin/ifconfig");
		}

		// processbuilder has trouble running ifconfig on ATF server without a specific run directory
		String currentDir = System.getProperty("user.dir");
		File parentDir = new File(currentDir).getParentFile();
		processBuilder.directory(parentDir);

		processBuilder.redirectErrorStream(true);
		String procOutput = "";

		Process process = null;

		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains(RegressionUtilities.NIGHTLY_REGRESSION_KICKOFF_SERVER)) {
					return true;
				}
				log.info(MARKER, line);
			}
			process.waitFor();
		} catch (InterruptedException | IOException e) {
			log.error(MARKER,
					"Could not get IP address of machine running regression, returning false in case this isn't the " +
							"kick off server.",
					e);
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return false;
	}

	void runExperiments(CloudService cloud) {
		ZonedDateTime regressionTestStart = ZonedDateTime.now(ZoneOffset.ofHours(0));
		SlackSummaryMsg summary = new SlackSummaryMsg(regConfig.getSlack(), regConfig, git,
				RegressionUtilities.getResultsFolder(regressionTestStart,regConfig.getName()));
		Experiment currentTest = null;
		SlackNotifier slacker = SlackNotifier.createSlackNotifier(regConfig.getSlack().getToken());
		for (int i = 0; i < regConfig.getExperiments().size(); i++) {
			try {
				currentTest = new Experiment(regConfig, regConfig.getExperiments().get(i));
				currentTest.setUseThreadPool(regConfig.isUseThreadPool());
				currentTest.resetNodes(); // make sure any preexisting instances are clean.
				/* all experiments in the set should have the same time to save in the same results folder */
				currentTest.setExperimentTime(regressionTestStart);
				if (cloud == null) {
					currentTest.runLocalExperiment(git);
				} else {
					currentTest.runRemoteExperiment(cloud, git);
				}

				// save and load historical data
				List<ExperimentSummary> historical = null;
				try {
					ExperimentSummaryStorage.storeSummary(currentTest,
							Date.from(currentTest.getExperimentTime().toInstant()));
					historical = ExperimentSummaryStorage.readSummaries(
							currentTest.getName(), 10);
					ExperimentSummaryStorage.deleteOldSummaries(
							currentTest.getName(), 10);
				} catch (Exception e) {
					log.error(ERROR, "Exception while storing/reading summaries:", e);
				}

				summary.addExperiment(currentTest, historical);
				sleep(CLOUD_WAIT_MILLIS); // add time between tests to allow for connections to reset, memory to free up
			} catch (Throwable t) {
				log.error(ERROR, "Exception while running experiment:", t);
				reportErrorToSlack(t, currentTest);
				summary.registerException(t);
			}
		}
		String summaryChannel = regConfig.getSlack().getSummaryChannel();
		if (summaryChannel == null || summaryChannel.isEmpty()) {
			summaryChannel = regConfig.getSlack().getChannel();
		}
		slacker.messageChannel(summary, summaryChannel);
	}

	void reportErrorToSlack(Throwable t, Experiment test) {
		SlackNotifier slacker = SlackNotifier.createSlackNotifier(regConfig.getSlack().getToken());
		/* pass null if test is null, otherwise pass whatever is in test */
		TestConfig tc = null;
		if (test != null) {
			tc = test.getTestConfig();
		}
		SlackTestMsg msg = new SlackTestMsg(null, regConfig, tc, null, git);
		msg.addExceptions(t);
		slacker.messageChannel(msg, regConfig.getSlack().getChannel());
	}

	void mvnBuild() throws Throwable {
		String throwMessage = "MVN FAILURE:\r\n";
		boolean wasError = false;
		String currentDir = System.getProperty("user.dir");
		File parentDir = new File(currentDir).getParentFile();
		ProcessBuilder processBuilder;
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			processBuilder = new ProcessBuilder("mvn.cmd", "-DskipTests", "install", "clean");
		} else {
			processBuilder = new ProcessBuilder("mvn", "-DskipTests", "install");
		}

		processBuilder.directory(parentDir);

		processBuilder.redirectErrorStream(true);
		String procOutput = "";
		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.contains(RegressionUtilities.MVN_ERROR_FLAG)) {
				wasError = true;
				log.error(ERROR, "Mvn Fail due to error: {}", line);
				throwMessage += line + "\r\n";

			}
			log.info(MARKER, line);
		}
		if (wasError) {
			throw new Throwable(throwMessage);
		}
		process.waitFor();
		process.destroy();
	}

	boolean stashAndPull() {

		String stashResults = git.stash();
		log.info(MARKER, "'{}'", stashResults.trim());
		String pullResults = git.updateGitBranch();
		log.info(MARKER, pullResults);
		if (stashResults.trim().equals("No local changes to save")) {
			log.info(MARKER, "nothing to stash");
			return false;
		}
		log.info(MARKER, "stashed changes");
		return true;
	}

	CloudService setUpCloudService() {
		CloudService service = new CloudService(regConfig.getCloud());

		service.startService(regConfig.getName());
		if (!service.isInstanceReady()) {
			try {
				log.info(MARKER, "sleeping for 30 seconds to allow instances to boot up");
				sleep(CLOUD_WAIT_MILLIS);
			} catch (InterruptedException e) {
				log.error(ERROR, "regression could not sleep.", e);
			}
		}
		//TODO: made retry constant in RegressionUtlities and use that
		int counter = 0;
		int wait_retry_times = WAIT_NODES_READY_TIMES + regConfig.getTotalNumberOfRegions();
		while (!service.isInstanceReady() && counter < wait_retry_times) { // old value 10 is too short for multiregion network
			log.info(MARKER, "instances still not ready...");
			try {
				sleep(10000);
			} catch (InterruptedException e) {
				log.error(ERROR, "regression could not sleep.", e);
			}
			counter++;
		}

		/* instance not ready after an extended time period, something went wrong */
		if (counter >= wait_retry_times) {
			log.error(ERROR, "Could not setup cloud service due to instances not ready after waiting.");
			return null;
		}


/*		try {
			sleep(30000);
		} catch (InterruptedException e) {
			log.error(ERROR, "regression could not sleep.", e);
		}*/

		return service;
	}


	void setFileNames() {
		if (startDate.equals("") || startTime.equals("")) {
			getTestTime();
		}
		baseFilename = "test " + startTime;
		logFilename = baseFilename + ".log";
		errFilename = baseFilename + ".err";
		logDir = this.regConfig.getLog().getURI() + startTime;

		log.info(MARKER, "base: {}\tlogFile:{}\terrFile:{}\tlogDir:{}", baseFilename, logFilename, errFilename, logDir);

	}

	void getTestTime() {
		DateTimeFormatter baseDate = DateTimeFormatter.ISO_LOCAL_DATE;
		DateTimeFormatter baseDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
		startDate = date.format(baseDate);
		startTime = date.format(baseDateTime);
	}


	public static void main(String[] args) {
		String regressionFile;
		String hederaServicesPath;
		RegressionMain rm;
		if (args.length > 0) {
			regressionFile = args[0];
			if (args.length == 2) {
				hederaServicesPath = args[1];
				RegressionUtilities.setHederaServicesRepoPath(hederaServicesPath);
			}
		} else {
			log.info(MARKER, "regression file not found, using default regression file: {}",
					RegressionUtilities.REGRESSION_CONFIG);
			regressionFile = RegressionUtilities.REGRESSION_CONFIG;
		}
		rm = new RegressionMain(regressionFile);
		rm.RunRegression();
	}

	public RegressionConfig getRegConfig() {
		return regConfig;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getBaseFilename() {
		return baseFilename;
	}

	public String getLogFilename() {
		return logFilename;
	}

	public String getErrFilename() {
		return errFilename;
	}

	public String getLogDir() {
		return logDir;
	}
}
