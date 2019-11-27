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

import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.jsonConfigs.FileLocationType;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.SavedState;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.slack.SlackNotifier;
import com.swirlds.regression.slack.SlackTestMsg;
import com.swirlds.regression.testRunners.TestRun;
import com.swirlds.regression.validators.NodeData;
import com.swirlds.regression.validators.ReconnectValidator;
import com.swirlds.regression.validators.StreamingServerData;
import com.swirlds.regression.validators.StreamingServerValidator;
import com.swirlds.regression.validators.Validator;
import com.swirlds.regression.validators.ValidatorFactory;
import com.swirlds.regression.validators.ValidatorType;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.MemoryMappedFileAppender;
import org.apache.logging.log4j.core.appender.RandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.swirlds.regression.RegressionUtilities.POSTGRES_WAIT_MILLIS;
import static com.swirlds.regression.RegressionUtilities.SDK_DIR;
import static com.swirlds.regression.RegressionUtilities.CHECK_BRANCH_CHANNEL;
import static com.swirlds.regression.RegressionUtilities.CHECK_USER_EMAIL_CHANNEL;
import static com.swirlds.regression.RegressionUtilities.CLOUD_WAIT_MILLIS;
import static com.swirlds.regression.RegressionUtilities.CONFIG_FILE;
import static com.swirlds.regression.RegressionUtilities.JAVA_PROC_CHECK_INTERVAL;
import static com.swirlds.regression.RegressionUtilities.JVM_OPTIONS_DEFAULT;
import static com.swirlds.regression.RegressionUtilities.MILLIS;
import static com.swirlds.regression.RegressionUtilities.PRIVATE_IP_ADDRESS_FILE;
import static com.swirlds.regression.RegressionUtilities.PUBLIC_IP_ADDRESS_FILE;
import static com.swirlds.regression.RegressionUtilities.REG_GIT_BRANCH;
import static com.swirlds.regression.RegressionUtilities.REG_GIT_USER_EMAIL;
import static com.swirlds.regression.RegressionUtilities.REG_SLACK_CHANNEL;
import static com.swirlds.regression.RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
import static com.swirlds.regression.RegressionUtilities.RESULTS_FOLDER;
import static com.swirlds.regression.RegressionUtilities.SETTINGS_FILE;
import static com.swirlds.regression.RegressionUtilities.STANDARD_CHARSET;
import static com.swirlds.regression.RegressionUtilities.TAR_NAME;
import static com.swirlds.regression.RegressionUtilities.TOTAL_STAKES;
import static com.swirlds.regression.RegressionUtilities.USE_STAKES_IN_CONFIG;
import static com.swirlds.regression.RegressionUtilities.WRITE_FILE_DIRECTORY;
import static com.swirlds.regression.RegressionUtilities.getResultsFolder;
import static com.swirlds.regression.RegressionUtilities.getSDKFilesToDownload;
import static com.swirlds.regression.RegressionUtilities.getSDKFilesToUpload;
import static com.swirlds.regression.RegressionUtilities.importExperimentConfig;
import static com.swirlds.regression.validators.StreamingServerValidator.EVENT_SIG_FILE_LIST;
import static com.swirlds.regression.validators.StreamingServerValidator.EVENT_LIST_FILE;
import static com.swirlds.regression.validators.StreamingServerValidator.FINAL_EVENT_FILE_HASH;
import static org.apache.commons.io.FileUtils.listFiles;

public class Experiment {

	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	static final int EXPERIMENT_START_DELAY = 2;
	static final int EXPERIMENT_FREEZE_SECOND_AFTER_STARTUP = 10;

	private TestConfig testConfig;
	private RegressionConfig regConfig;
	private ConfigBuilder configFile;
	private SettingsBuilder settingsFile;
	private GitInfo git;
	private SlackNotifier slacker;

	private ZonedDateTime experimentTime = null;
	private boolean isFirstTestFinished = false;
	private CloudService cloud = null;
	private ArrayList<SSHService> sshNodes = new ArrayList<>();

	// Is used for validating reconnection after running startFromX test, should be the max value of roundNumber of savedState the nodes start from;
	// At the end of the test, if the last entry of roundSup of reconnected node is less than this value, the reconnect is considered to be invalid
	private double savedStateStartRoundNumber = 0;

	void setExperimentTime() {
		this.experimentTime = ZonedDateTime.now(ZoneOffset.ofHours(0));
	}

	public void setExperimentTime(ZonedDateTime regressionStart) {
		this.experimentTime = regressionStart;
	}

// The below method was used for testing, and might be useful in the future

/*	public static void main(String[] args) {
		Experiment experiment = new Experiment(
				RegressionUtilities.importRegressionConfig("./configs/AwsRegressionCfgLazar.json"),
				"configs/testFcm1KSavedStateCfg.json");
		experiment.experimentTime = ZonedDateTime.of(
				2019, 10, 17, 9, 27, 0, 0,
				ZoneOffset.ofHours(0));

		experiment.testConfig.setResultFiles(Collections.singletonList("swirlds.log"));
		experiment.sshNodes = new ArrayList<SSHService>(Arrays.asList(new SSHService[] { null, null, null, null }));

		experiment.validateTest();
	}*/

	public Experiment(RegressionConfig regressionConfig, String experimentFileLocation) {
		this(regressionConfig, importExperimentConfig(experimentFileLocation));
	}

	public Experiment(RegressionConfig regressionConfig, TestConfig experiment) {
		this.regConfig = regressionConfig;
		setupTest(experiment);

		if (regConfig.getSlack() != null) {
			slacker = SlackNotifier.createSlackNotifier(regConfig.getSlack().getToken(),
					regConfig.getSlack().getChannel());
		}
	}

	public void runLocalExperiment(GitInfo git) {
		this.git = git;
		if (regConfig.getLocal() != null) {
			startLocalTest();
		} else if (regConfig.getCloud() != null) {
			log.error(MARKER, "cloud based test are still a work in progress");
		} else {
			log.error(MARKER,
					"regression must either startService local or on cloud, please set up regression config to " +
							"reflect" +
							" " +
							"this.");
		}
	}

	public void startAllSwirlds() {
		for (SSHService node : sshNodes) {
			node.execWithProcessID(regConfig.getJvmOptions());
			log.info(MARKER, "node:" + node.getIpAddress() + "swirlds.jar started.");
		}
	}

	public void stopAllSwirlds() {
		for (SSHService currentNode : sshNodes) {
			currentNode.killJavaProcess();
		}
	}

	public void stopLastSwirlds() {
		SSHService nodeToKill = sshNodes.get(sshNodes.size() - 1);
		nodeToKill.killJavaProcess();
	}

	public void startLastSwirlds() {
		sshNodes.get(sshNodes.size() - 1).execWithProcessID(regConfig.getJvmOptions());
	}

	public void sleepThroughExperiment(long testDuration) {
		/*TODO: Test duration shouldn't be needed for PTD now, look into removing it for PTD tests */
		/* Local test shouldn't look for dead nodes, if the test is too short this would just make the test run
		longer */
		if (regConfig.getCloud() != null && JAVA_PROC_CHECK_INTERVAL < testDuration) {
			ArrayList<Boolean> isProcDown = new ArrayList<>();
			ArrayList<Boolean> isTestFinished = new ArrayList<>();
			/* set array lists to appropriate size with default values */
			for (int i = 0; i < sshNodes.size(); i++) {
				isProcDown.add(false);
				isTestFinished.add(false);
			}
			/* testDuration is already in milliseconds */
			/* TODO: endTime should be unnecessary if java Proc check and test success are working for PTD */
			long endTime = System.nanoTime() + (testDuration * 1_000_000);
			log.info(MARKER, "sleeping for {} seconds, or until test finishes ", testDuration / 1_000);
			while (System.nanoTime() < endTime) { // Don't go over set test time
				try {
					log.trace(MARKER, "sleeping for {} seconds ", JAVA_PROC_CHECK_INTERVAL / 1_000);
					Thread.sleep(JAVA_PROC_CHECK_INTERVAL);
				} catch (InterruptedException e) {
					log.error(ERROR, "could not sleep.", e);
				}
				/* Check all processes are still running, then check if test has finished */
				for (int i = 0; i < sshNodes.size(); i++) {
					SSHService node = sshNodes.get(i);
					boolean isProcStillRunning = node.checkProcess();
					/* if this it the first time down, keep running, if down two times in a row, stop */
					if (!isProcStillRunning && isProcDown.get(i)) {
						return;
					} else {
						/* always set to true in case it was false, this makes sure reconnect and restart don't fail out
						after the first reconnect/restart */
						isProcDown.set(i, !isProcStillRunning);
					}
					isTestFinished.set(i, node.isTestFinished());
				}
				/* we don't want to exit if only a few nodes have reported being finished */
				if (checkForAllTrue(isTestFinished)) {
					return;
				}
			}
		} else {
			try {
				//TODO: should this check for test finishing as well for local test > JAVA_PROC_CHECK_INTERVAL?
				log.info(MARKER, "sleeping for {} seconds ", testDuration / 1_000);
				Thread.sleep(testDuration);
			} catch (InterruptedException e) {
				log.error(ERROR, "could not sleep.", e);
			}
		}
	}

	private boolean checkForAllTrue(ArrayList<Boolean> isTestFinished) {
		for (Boolean val : isTestFinished) {
			if (!val) {
				return false;
			}
		}
		return true;
	}

	private void sendTarToNode(SSHService currentNode, ArrayList<File> addedFiles) {
		String pemFile = regConfig.getCloud().getKeyLocation() + ".pem";
		String pemFileName = pemFile.substring(pemFile.lastIndexOf('/') + 1);
		long startTime = System.nanoTime();
		Collection<File> filesToSend = getSDKFilesToUpload(
				new File(pemFile), new File(testConfig.getLog4j2File()), addedFiles);
		File oldTarFile = new File(TAR_NAME);
		if (oldTarFile.exists()) {
			oldTarFile.delete();
		}
		currentNode.createNewTar(filesToSend);
		ArrayList<File> tarball = new ArrayList<>();
		tarball.add(new File(TAR_NAME));
		currentNode.scpToSpecificFiles(tarball);
		currentNode.extractTar();
		currentNode.executeCmd("chmod 400 ~/" + REMOTE_EXPERIMENT_LOCATION + pemFileName);
		long endTime = System.nanoTime();
		log.trace(MARKER, "Took {} seconds to upload tarball", (endTime - startTime) / 1000000000);
	}

	private String getExperimentResultsFolderForNode(int nodeNumber) {
		return getExperimentFolder() + "node000" + nodeNumber + "/";
	}

	private String getExperimentFolder() {
		String folderName = regConfig.getName() + "/" + testConfig.getName();
		return RESULTS_FOLDER + "/" + getResultsFolder(experimentTime,
				folderName) + "/";
	}

	private InputStream getInputStream(String filename) {
		InputStream inputStream = null;
		if (!new File(filename).exists()) {
			return null;
		}
		try {
			inputStream = new FileInputStream(filename);
		} catch (IOException e) {
			log.error(ERROR, "Could not open file {} for validation", filename, e);
		}
		return inputStream;
	}

	private List<NodeData> loadNodeData(String directory) {
		int numberOfNodes;
		if (regConfig.getLocal() != null) {
			numberOfNodes = regConfig.getLocal().getNumberOfNodes();
		} else if (sshNodes == null || sshNodes.isEmpty()) {
			return null;
		} else {
			numberOfNodes = sshNodes.size();
		}
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0; i < numberOfNodes; i++) {
			for (String logFile : testConfig.getResultFiles()) {
				String logFileName = getExperimentResultsFolderForNode(i) + logFile;
				InputStream logInput = getInputStream(logFileName);

				String csvFileName = settingsFile.getSettingValue("csvFileName") + i + ".csv";
				String csvFilePath = getExperimentResultsFolderForNode(i) + csvFileName;
				InputStream csvInput = getInputStream(csvFilePath);
				if (logInput != null && csvInput != null) {
					nodeData.add(new NodeData(LogReader.createReader(1, logInput), CsvReader.createReader(1,
							csvInput)));
				}
			}
		}
		return nodeData;
	}

	private List<StreamingServerData> loadStreamingServerData(String directory) {
		final List<StreamingServerData> nodeData = new ArrayList<>();
		for (int i = 0; i < regConfig.getEventFilesWriters(); i++) {
			final String shaFileName = getExperimentResultsFolderForNode(i) + FINAL_EVENT_FILE_HASH;
			final String shaEventFileName = getExperimentResultsFolderForNode(i) + EVENT_LIST_FILE;
			final String eventSigFileName = getExperimentResultsFolderForNode(i) + EVENT_SIG_FILE_LIST;
			nodeData.add(new StreamingServerData(getInputStream(eventSigFileName), getInputStream(shaFileName),
					getInputStream(shaEventFileName)));
		}
		return nodeData;
	}

	private void validateTest() {
		SlackTestMsg slackMsg = new SlackTestMsg(
				regConfig,
				testConfig,
				getResultsFolder(experimentTime, testConfig.getName()),
				git
		);

		// if posting to the regression channel, check branch and git user
		if (regConfig.getSlack() != null
				&& regConfig.getSlack().getChannel().equals(REG_SLACK_CHANNEL)) {
			if (CHECK_BRANCH_CHANNEL
					&& !git.getGitInfo(true).contains(REG_GIT_BRANCH)) {
				slackMsg.addWarning(String.format(
						"Only nightly tests on '%s' branch should be posted to the '%s' channel",
						REG_GIT_BRANCH,
						REG_SLACK_CHANNEL
				));
			}
			if (CHECK_USER_EMAIL_CHANNEL
					&& !git.getUserEmail().equals(REG_GIT_USER_EMAIL)) {
				slackMsg.addWarning(String.format(
						"The git user with the email '%s' should not be posting to the '%s' channel",
						git.getUserEmail(),
						REG_SLACK_CHANNEL
				));
			}
		}

		// Build a lists of validator
		List<Validator> requiredValidator = new ArrayList<>();
		for (ValidatorType item : testConfig.validators) {
			List<NodeData> nodeData = loadNodeData(testConfig.getName());
			if (nodeData == null || nodeData.size() == 0) {
				slackMsg.addError("No data found for " + item);
				continue;
			} else if (nodeData.size() < regConfig.getTotalNumberOfNodes()) {
				slackMsg.addWarning(String.format(
						"%s - Configured number of nodes is %d, but only found %d nodes",
						item,
						regConfig.getTotalNumberOfNodes(),
						nodeData.size()));
			}
			requiredValidator.add(ValidatorFactory.getValidator(item, nodeData, testConfig));
		}

		// Add stream server validator if event streaming is configured
		if (regConfig.getEventFilesWriters() > 0) {
			StreamingServerValidator ssValidator = new StreamingServerValidator(
					loadStreamingServerData(testConfig.getName()));
			requiredValidator.add(ssValidator);
		}

		for (Validator item : requiredValidator) {
			try {
				if (item instanceof ReconnectValidator) {
					((ReconnectValidator) item).setSavedStateStartRoundNumber(savedStateStartRoundNumber);
				}
				item.validate();
				slackMsg.addValidatorInfo(item);
			} catch (Throwable e) {
				log.error(ERROR, "{} validator failed to validate", item.getClass(), e);
				slackMsg.addValidatorException(item, e);
			}
		}

		sendSlackMessage(slackMsg);
		log.info(MARKER, slackMsg.getPlainText());
	}

	public void sendSettingFileToNodes() {
		if (regConfig.getEventFilesWriters() == 0) {
			sendSettingToNonStreamingNodes();
		} else {
			sendSettingToStreamingNodes();
		}
	}

	private void sendSettingToStreamingNodes() {
		/* since the contents of the file change, but not the location of the file this can be set up outside the for
		loop */
		ArrayList<File> newUploads = new ArrayList<>();
		newUploads.add(new File(WRITE_FILE_DIRECTORY + SETTINGS_FILE));
		for (int i = 0; i < sshNodes.size(); i++) {
			if (i == 0 && i < regConfig.getEventFilesWriters()) {
				/* create settings file with enableEventStreaming enabled for streaming nodes */
				ArrayList<String> enableEventStreamSetting = new ArrayList<>();
				enableEventStreamSetting.add("enableEventStreaming, true");
				settingsFile.exportNodeSpecificSettingsFile(enableEventStreamSetting);
			} else if (i == regConfig.getEventFilesWriters()) {
				/* now that streaming nodes are taken care of  export settings file without enableEventStreaming
				setting */
				settingsFile.exportSettingsFile();
			}
			sshNodes.get(i).scpToSpecificFiles(newUploads);
		}
	}

	private void sendSettingToNonStreamingNodes() {
		settingsFile.exportSettingsFile();
		ArrayList<File> newUploads = new ArrayList<>();
		newUploads.add(new File(WRITE_FILE_DIRECTORY + SETTINGS_FILE));
		for (SSHService currentNode : sshNodes) {
			currentNode.scpToSpecificFiles(newUploads);
		}
	}

	private void setIPsAndStakesInConfig() {
		configFile.setPublicIPList(cloud.getPublicIPList());
		configFile.setPrivateIPList(cloud.getPrivateIPList());
		if (USE_STAKES_IN_CONFIG) {
			configFile.setStakes(
					// each node gets the same stake
					Collections.nCopies(
							regConfig.getTotalNumberOfNodes(),
							TOTAL_STAKES / regConfig.getTotalNumberOfNodes())
			);
		}
		configFile.exportConfigFile();
	}

	public void sendConfigToNodes() {
		// pub and private address aren't set in configFile until this call, since it is never needed/used.
		setIPsAndStakesInConfig();

		ArrayList<File> newUploads = new ArrayList<>();
		newUploads.add(new File(WRITE_FILE_DIRECTORY + CONFIG_FILE));
		for (SSHService currentNode : sshNodes) {
			currentNode.scpToSpecificFiles(newUploads);
		}
	}

	public void sendSlackMessage(SlackTestMsg msg) {
		slacker.messageChannel(msg);
	}

	public void sendSlackMessage(String error) {
		SlackTestMsg msg = new SlackTestMsg(regConfig, testConfig);
		msg.addError(error);
		slacker.messageChannel(msg);
	}

	void runRemoteExperiment(CloudService cld, GitInfo git) throws IOException {
		//TODO: unit test for null cld
		if (cld == null) {
			log.error(ERROR, "Cloud instance was null, cannot run test!");
			sendSlackMessage("Cloud instance was null, cannot start test!");
			return;
		}
		this.cloud = cld;
		this.git = git;
		this.cloud.setInstanceNames(testConfig.getName());
		exportIPAddressFiles();

		setIPsAndStakesInConfig();

		String login = regConfig.getCloud().getLogin();
		File keyfile = new File(regConfig.getCloud().getKeyLocation() + ".pem");
		//TODO multi-thread this perhaps?
		SSHService firstNode = null;
		ArrayList<String> publicIPList = cloud.getPublicIPList();
		int nodeNumber = publicIPList.size();
		for (int i = 0; i < nodeNumber; i++) {
			String pubIP = publicIPList.get(i);
			SSHService currentNode = new SSHService(login, pubIP, keyfile);
			//TODO Unit test for this must change the files name of the pem file to prevent connections
			if (currentNode == null) {
				cloud.destroyInstances();
				log.error(ERROR, "Could not start/ or connect to node, exiting regression test.");
				uploadFilesToSharepoint();
				throw new SocketException("Node: " + pubIP + " returned as null on initialization.");
			}
			currentNode.reset();

			ArrayList<File> addedFiles = buildAdditionalFileList();
			//currentNode.buildSession();

			if (firstNode == null) {
				firstNode = currentNode;
				sendTarToNode(currentNode, addedFiles);
			} else {
				firstNode.rsyncTo(addedFiles, currentNode.getIpAddress(), new File(testConfig.getLog4j2File()));
			}
			sshNodes.add(currentNode);

			// copy a saved state if set in config
			SavedState savedState = testConfig.getSavedStateForNode(i, nodeNumber);
			if (savedState != null) {
				double thisSavedStateRoundNum = savedState.getRound();
				if (thisSavedStateRoundNum > savedStateStartRoundNumber) {
					savedStateStartRoundNumber = thisSavedStateRoundNum;
				}

				String ssPath = RegressionUtilities.getRemoteSavedStatePath(
						savedState.getMainClass(),
						i,
						savedState.getSwirldName(),
						savedState.getRound()
				);
				FileLocationType locationType = savedState.getLocationType();
				switch (locationType) {
					case AWS_S3:
						log.info(MARKER, "Downloading saved state for S3 to node {}", i);
						currentNode.copyS3ToInstance(savedState.getLocation(), ssPath);
						// list files in destination directory for validation
						currentNode.listDirFiles(ssPath);
						break;
					case LOCAL:
						log.info(MARKER, "Uploading local saved state to node {}", i);
						currentNode.scpFilesToRemoteDir(
								RegressionUtilities.getFilesInDir(savedState.getLocation(), true),
								ssPath
						);
						break;
				}
				if (savedState.isRestoreDb()) {
					currentNode.restoreDb(ssPath + RegressionUtilities.DB_BACKUP_FILENAME);
				}
			}
		}
		log.info(MARKER, "upload to nodes has finished");

		log.info(MARKER, "Sleeping for {} seconds to allow PostGres to start", POSTGRES_WAIT_MILLIS / MILLIS);
		//TODO add a proper check for postgres
		try {
			Thread.sleep(POSTGRES_WAIT_MILLIS);
		} catch (InterruptedException ignored) {
		}

		TestRun testRun = testConfig.getRunType().getTestRun();
		testRun.preRun(testConfig, this);
		sendSettingFileToNodes();

		testRun.runTest(testConfig, this);

		//TODO maybe move the kill to the TestRun
		stopAllSwirlds();

		// call badgerize.sh that tars all the database logs

		if (testConfig.getDownloadDbLogFiles()) {
			for (int i = 0; i < nodeNumber; i++) {
				SSHService currentNode = sshNodes.get(i);
				currentNode.badgerize();
			}
		}

		/* make sure that more streaming client than nodes were not requested */
		int eventFileWriters = Math.min(regConfig.getEventFilesWriters(), sshNodes.size());
		for (int i = 0; i < eventFileWriters; i++) {
			SSHService node = sshNodes.get(i);
			node.makeSha1sumOfStreamedEvents(testConfig.getName(), i, testConfig.getDuration());
			log.info(MARKER, "node:" + node.getIpAddress() + " created sha1sum of .evts");
		}
		for (int i = 0; i < sshNodes.size(); i++) {
			SSHService node = sshNodes.get(i);
			node.scpFrom(getExperimentResultsFolderForNode(i), (ArrayList<String>) testConfig.getResultFiles());
		}

		log.info(MARKER, "Downloaded experiment data");

		validateTest();
		log.info(MARKER, "Test validation finished");
		uploadFilesToSharepoint();

		//resetNodes();
		killJavaProcess();
	}

	void resetNodes() {
		for (final SSHService node : sshNodes) {
			node.reset();
			node.close();
		}

		sshNodes.clear();
	}

	void killJavaProcess() {
		for (final SSHService node : sshNodes) {
			node.killJavaProcess();
			node.close();
		}
	}

	private void uploadFilesToSharepoint() {
		if (!regConfig.isUploadToSharePoint()) {
			log.info(MARKER, "Uploading to SharePoint is off, skipping");
			return;
		}
		SharePointManager spm = new SharePointManager();
		spm.login();
		copyLogFileToResultsFolder();
		try (Stream<Path> walkerPath = Files.walk(Paths.get(getExperimentFolder()))) {
			final List<String> foundFiles = walkerPath.filter(Files::isRegularFile).map(x -> x.toString()).collect(
					Collectors.toList());
			for (final String file : foundFiles) {
				final File currentFile = new File(file);
				//TODO get rid of 500000 and replace wit static final, split zip at 50,000 as well
/*				if(currentFile.isFile() && currentFile.getTotalSpace() > 500000){
					Path zippedFile = Paths.get(new File(currentFile.getPath() + ".zip").toURI());
					CompressResults zipper = new CompressResults(zippedFile);
					zipper.bundleFile(Paths.get(currentFile.toURI()));
					currentFile = zippedFile.toFile();
				} */
				final String path = getResultsFolder(experimentTime,
						testConfig.getName()) + file.substring(file.indexOf("results/", 1) + 8);
				path.replace("//", "/");
				log.info(MARKER, "uploadFile({},{})", path, currentFile.getName());
				spm.uploadFile(path, currentFile);
			}
		} catch (IOException e) {
			log.error(ERROR, "unable to walk experiments results", e);
		}

	}

	private void copyLogFileToResultsFolder() {
		//TODO: change regression.log into static const in RegressionUtilites
		String logFile = "regression.log";
		/*get the name of the file logger is writing to */
		org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) log;
		final Collection<Appender> logAppeneders = ((org.apache.logging.log4j.core.Logger) log).getAppenders().values();
		for (final Appender appender : logAppeneders) {
			if (appender instanceof FileAppender) {
				logFile = (((FileAppender) appender).getFileName());
			} else if (appender instanceof RandomAccessFileAppender) {
				logFile = (((RandomAccessFileAppender) appender).getFileName());
			} else if (appender instanceof RollingFileAppender) {
				logFile = (((RollingFileAppender) appender).getFileName());
			} else if (appender instanceof MemoryMappedFileAppender) {
				logFile = (((MemoryMappedFileAppender) appender).getFileName());
			} else if (appender instanceof RollingRandomAccessFileAppender) {
				logFile = (((RollingRandomAccessFileAppender) appender).getFileName());
			}
		}

		final Path logFileSource = Paths.get(logFile);
		/*TODO: change regession.log into static const in RegressionUtilities */
		final Path logFileDestination = Paths.get(getExperimentFolder() + "regression.log");
		if (git != null) {
			if(!new File(git.getGitLog()).exists())
				new File(git.getGitLog()).mkdirs();
			final Path gitLogSource = Paths.get(git.getGitLog());
			final Path gitLogDestination = Paths.get(getExperimentFolder() + git.getGitLog());
			try {
				if(gitLogSource!=null)
					Files.copy(gitLogSource, gitLogDestination);
			} catch (Exception e) {
					log.error(ERROR, "Could not copy regression log", e);
			}
		} else {
			log.error(ERROR, "gitInfo object not initialized");
		}
		try {
			Files.copy(logFileSource, logFileDestination);
		} catch (IOException e) {
			log.error(ERROR, "Could not copy regression log", e);
		}
	}

	private ArrayList<File> buildAdditionalFileList() {
		ArrayList<File> returnList = new ArrayList<>();
		testConfig.getFilesNeeded().forEach((f) -> {
			returnList.add(new File(f));
		});
		return returnList;
	}

	private void exportIPAddressFiles() {
		if (cloud == null) {
			return;
		}
		ArrayList<String> pubIPs = cloud.getPublicIPList();
		ArrayList<String> prvtIPs = cloud.getPrivateIPList();
		Path pubIPFile = Paths.get(PUBLIC_IP_ADDRESS_FILE);
		Path pvtIPFile = Paths.get(PRIVATE_IP_ADDRESS_FILE);
		try {
			Files.write(pubIPFile, pubIPs, STANDARD_CHARSET);
			Files.write(pvtIPFile, prvtIPs, STANDARD_CHARSET);
		} catch (IOException e) {
			log.error(ERROR, "unable to output public and private IP address files.", e);
		}

	}

	public void startLocalTest() {
		try {
			sendSettingFileToNodes();
			int processReturnValue = startBrowser();
			log.info(MARKER, "First Process finish with value: {}", processReturnValue);

			for (int i = 0; i < regConfig.getLocal().getNumberOfNodes(); i++) {
				scpFrom(i, (ArrayList<String>) testConfig.getResultFiles());
				log.info(MARKER, "local files copied to results.");
			}

			validateTest();

			log.info(MARKER, "restart was successful.");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private int startBrowser() throws IOException, InterruptedException {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome +
				File.separator + "bin" +
				File.separator + "java";
		String javaOptions = JVM_OPTIONS_DEFAULT;
		if (!"".equals(regConfig.getJvmOptions())) {
			javaOptions = regConfig.getJvmOptions();
		}

		ProcessBuilder builder = new ProcessBuilder(
				javaBin, javaOptions, "-jar", "swirlds.jar");
		log.info(MARKER, "builder String: {}", builder.command().toString());

		long testDuration = testConfig.getDuration();
		log.info(MARKER, "regular test duration: {}", testDuration);
		//TODO this needs to be handled differently, so commenting out for the time being
		/*
		if (testConfig.isRestart() && !isFirstTestFinished) {
			Duration dur = getRestartDuration();
			testDuration = dur.getSeconds();
			log.info(MARKER, "restart test duration: {}", testDuration);
		}
		 */
		Process process = builder.inheritIO().start();
		if (!process.waitFor(testDuration, TimeUnit.SECONDS)) {
			process.destroyForcibly();
		}
		isFirstTestFinished = true;
		return process.exitValue();

	}

	void scpFrom(int node, ArrayList<String> downloadExtensions) {
		String topLevelFolders = getExperimentResultsFolderForNode(node);
		try {
			CopyOption[] options = new CopyOption[] {
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES
			};
			log.info(MARKER, "top level folder: {}", topLevelFolders);

			Collection<File> foundFiles = getListOfFiles(getSDKFilesToDownload(downloadExtensions),
					node);
			log.info(MARKER, "total files found:{}", foundFiles.size());
			for (File file : foundFiles) {
				Path fromFile = Paths.get(file.toURI());
				String currentLine = file.getName();
				/* remove everything before remoteExperiments and "remoteExpeirments" add in experiment folder and node
				. */
				currentLine = topLevelFolders + currentLine;

				File fileToSplit = new File(currentLine);
				Path toFile = Paths.get(fileToSplit.toURI());
				if (!fileToSplit.exists()) {

					/* has to be getParentFile().mkdirs() because if it is not JAVA will make a directory with the name
					of the file like remoteExperiments/swirlds.jar. This will cause scp to take the input as a filepath
					and not the file itself leaving the directory structure like remoteExperiments/swirlds.jar/swirlds
					.jar
					 */
					fileToSplit.getParentFile().mkdirs();

				}
				Files.copy(fromFile, toFile, options);
				log.info(MARKER, "downloading {} from node {} putting it in {}", fromFile.toString(), node,
						toFile.toString());
			}
		} catch (IOException e) {
			log.error(ERROR, "Could not download files", e);
		}
	}

	Collection<File> getListOfFiles(ArrayList<String> extension, int node) {
		final ArrayList<String> ext = ChangeExtensionToBeNodeSpecific(extension, node);
		final IOFileFilter filter;
		if (ext == null) {
			filter = TrueFileFilter.INSTANCE;
		} else {
			filter = new SuffixFileFilter(ext);
		}

		return listFiles(new File("./"), filter,
				false ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE);
	}

	private ArrayList<String> ChangeExtensionToBeNodeSpecific(ArrayList<String> extension, int node) {
		final ArrayList<String> returnArray = new ArrayList<>();
		for (final String ext : extension) {
			returnArray.add(ext.replace("*", Integer.toString(node)));
		}
		return returnArray;
	}

	void setupTest(TestConfig experiment) {
		this.testConfig = experiment;

		setExperimentTime();
		configFile = new ConfigBuilder(regConfig, testConfig);
		settingsFile = new SettingsBuilder(testConfig);
	}

	public TestConfig getTestConfig() {
		return testConfig;
	}

	public SettingsBuilder getSettingsFile() {
		return settingsFile;
	}
}
