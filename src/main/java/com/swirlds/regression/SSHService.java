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

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.swirlds.regression.RegressionUtilities.CREATE_DATABASE_FCFS_EXPECTED_RESPONCE;
import static com.swirlds.regression.RegressionUtilities.DROP_DATABASE_FCFS_EXPECTED_RESPONCE;
import static com.swirlds.regression.RegressionUtilities.DROP_DATABASE_FCFS_KNOWN_RESPONCE;
import static com.swirlds.regression.RegressionUtilities.EVENT_MATCH_MSG;
import static com.swirlds.regression.RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
import static com.swirlds.regression.RegressionUtilities.SAVED_STATE_LOCATION;
import static com.swirlds.regression.validators.RecoverStateValidator.EVENT_MATCH_LOG_NAME;
import static com.swirlds.regression.validators.StreamingServerValidator.EVENT_FILE_LIST;
import static com.swirlds.regression.validators.StreamingServerValidator.EVENT_LIST_FILE;
import static com.swirlds.regression.validators.StreamingServerValidator.EVENT_SIG_FILE_LIST;
import static com.swirlds.regression.validators.StreamingServerValidator.FINAL_EVENT_FILE_HASH;
import static java.time.temporal.ChronoUnit.SECONDS;


public class SSHService {

	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private static final long MAX_COMMAND_OUTPUT_WATCH = 5000000000l;


	private String user;
	private String ipAddress;
	private ArrayList<String> files;
	private File keyFile;
	private SSHClient ssh;
	private Session session;

	private Instant lastExec;
	private String streamDirectory;

	public SSHService(String user, String ipAddress, File keyFile) throws SocketException {
		this.user = user;
		this.ipAddress = ipAddress;
		this.keyFile = keyFile;

		ssh = buildSession();
		if (this.ssh == null) {
			throw new SocketException("Unable to connect to cloud instance via ssh.");
		}
		lastExec = Instant.now();
	}

	private String readStream(InputStream is) {
		String returnString = "";
		byte[] tmp = new byte[1024];
		try {
			while (is.available() > 0) {
				int i = is.read(tmp, 0, 1024);
				if (i < 0) {
					break;
				}
				returnString += new String(tmp, 0, i);
				log.info(MARKER, new String(tmp, 0, i));
			}
		} catch (IOException | NullPointerException e) {
			log.error(ERROR, "SSH Command failed! Could not read returned streams", e);
		}
		return returnString;
	}

	private ArrayList<String> readCommandOutput(Session.Command cmd) {
		ArrayList<String> returnArray = new ArrayList<>();
		if (cmd == null) {
			return returnArray;
		}

		String fullString = "";
		log.trace(MARKER, "reading command");

		InputStream is = cmd.getInputStream();
		InputStream es = cmd.getErrorStream();
		long startTime = System.nanoTime();

		while (true) {
			fullString += readStream(is);
			fullString += readStream(es);
			if (!cmd.isOpen()) {
				if (!isStreamEmpty(is)) {
					continue;
				}
				if (!isStreamEmpty(es)) {
					continue;
				}
				break;
			}
			if ((System.nanoTime() - startTime) > MAX_COMMAND_OUTPUT_WATCH) {
				log.info(MARKER, "monitored output for 5 seconds, exiting loop.");
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}

		String lines[] = fullString.split("\\r?\\n");
		log.trace(MARKER, "lines in output {}", lines.length);
		for (String line : lines) {
			returnArray.add(line);
			log.trace(MARKER, "Added {} to returnArray", line);
		}
		return returnArray;
	}

	private boolean isStreamEmpty(InputStream is) {
		try {
			if (is.available() == 0) {
				return true;
			}
		} catch (IOException | NullPointerException e) {
			log.error(ERROR, "SSH command failed! Failed to check if stream is empty", e);
		}
		return false;
	}

	Collection<String> getListOfFiles(ArrayList<String> extension) {
		Collection<String> returnCollection = new ArrayList<>();
		String extensions = "\\( ";
		for (int i = 0; i < extension.size(); i++) {
			if (i > 0) {
				extensions += " -o ";
			}
			extensions += "-name \"" + extension.get(i) + "\"";
		}
		extensions += " \\) ";
		String pruneDirectory = "-not -path \"*/data/*\"";
		String commandStr = "find . " + extensions + pruneDirectory;
		final Session.Command cmd = execCommand(commandStr, "Find list of Files based on extension", -1);
		log.info(MARKER, "Extensions to look for on node {}: ", ipAddress, extensions);
		returnCollection = readCommandOutput(cmd);

		return returnCollection;
	}

	void createNewTar(Collection<File> fileList) {
		File tarball = new File(RegressionUtilities.TAR_NAME);
		if (tarball.exists()) {
			return;
		}

		try (TarGzFile archive = new TarGzFile(Paths.get(new File(RegressionUtilities.TAR_NAME).toURI()))) {
			for (File file : fileList) {
				if (!file.exists()) {
					continue;
				}
				if (file.isFile()) {
					archive.bundleFile(Paths.get(file.toURI()));
				} else if (file.isDirectory()) {
					archive.bundleDirectory(Paths.get(file.toURI()));
				}
			}
		} catch (IOException e) {
			log.error(ERROR, "could not create tarball on node: {}", ipAddress, e);
		}
	}

	void scpFrom(String topLevelFolders, ArrayList<String> downloadExtensions) {
		try {
			log.info(MARKER, "top level folder: {}", topLevelFolders);
			Collection<String> foundFiles = getListOfFiles(
					RegressionUtilities.getSDKFilesToDownload(downloadExtensions));
			log.info(MARKER, "total files found:{}", foundFiles.size());
			for (String file : foundFiles) {
				String currentLine = file;
				/* remove everything before remoteExperiments and "remoteExpeirments" add in experiment folder and node
				. */
				String cutOffString = RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
				int cutOff = currentLine.indexOf(cutOffString) + cutOffString.length() - 1;

				log.info(MARKER,
						String.format("CutOff of '%d' computed for the line '%s' with cutOffString of " +
										"'%s'.",
								cutOff, currentLine, cutOffString));

				if (cutOff >= 0 && !currentLine.isEmpty() && cutOff < currentLine.length()) {
					currentLine = currentLine.substring(cutOff);
				} else {
					log.error(MARKER,
							String.format("Invalid cutOff of '%d' computed for the line '%s' with cutOffString of " +
											"'%s'.",
									cutOff, currentLine, cutOffString));
				}

				currentLine = topLevelFolders + currentLine;

				File fileToSplit = new File(currentLine);
				if (!fileToSplit.exists()) {

					/* has to be getParentFile().mkdirs() because if it is not JAVA will make a directory with the name
					of the file like remoteExperiments/swirlds.jar. This will cause scp to take the input as a filepath
					and not the file itself leaving the directory structure like remoteExperiments/swirlds.jar/swirlds
					.jar
					 */
					fileToSplit.getParentFile().mkdirs();

				}
				log.info(MARKER, "downloading {} from node {} putting it in {}", file, ipAddress,
						fileToSplit.getPath());
				ssh.newSCPFileTransfer().download(file, fileToSplit.getPath());
			}
		} catch (IOException | StringIndexOutOfBoundsException e) {
			log.error(ERROR, "Could not download files", e);
		}
	}

	void scpTo(ArrayList<File> additionalFiles) {
		makeRemoteDirectory(
				RegressionUtilities.REMOTE_EXPERIMENT_LOCATION); // make sure remoteExperiments directory exist before
		// massive copy */
		createNewTar(RegressionUtilities.getSDKFilesToUpload(keyFile, new File("log4j2.xml"), additionalFiles));
		File tarball = new File(RegressionUtilities.TAR_NAME);
		ArrayList<String> uploadValues = splitDirectorys(tarball, true);
		try {
			ssh.newSCPFileTransfer().upload(uploadValues.get(1), uploadValues.get(0));
		} catch (IOException e) {
			log.error(ERROR, "could not upload to {}", ipAddress, e);
		}
	}

	void scpFilesToRemoteDir(List<String> files, String remoteDir) throws IOException {
		if (!remoteDir.endsWith("/")) {
			throw new IllegalArgumentException("remoteDir must end with a slash(/)");
		}
		for (String file : files) {
			if (!new File(file).exists()) {
				throw new IllegalArgumentException("The file '" + file + "' does not exist!");
			}
		}
		makeRemoteDirPath(remoteDir);
		for (String file : files) {
			ssh.newSCPFileTransfer().upload(file, remoteDir);
		}
	}

	// return true if rsync finished successfully
	public boolean rsyncTo(ArrayList<File> additionalFiles, String toIPAddress, File log4j2Xml) {
		long startTime = System.nanoTime();
		makeRemoteDirectory(
				RegressionUtilities.REMOTE_EXPERIMENT_LOCATION); // make sure remoteExperiments directory exist before
		// massive copy */
		String rsyncCmd =
				"rsync -a -r -z -e \"ssh -o StrictHostKeyChecking=no -i ~/" + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION + this.keyFile.getName() + "\" ";
		for (String fileToUpload : RegressionUtilities.getRsyncListToUpload(keyFile, log4j2Xml,
				additionalFiles)) {
			rsyncCmd += "--include=\"" + fileToUpload + "\" ";
		}
		rsyncCmd += "--exclude=\"*\" --delete --delete-excluded ./" + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION +
				" " + user + "@" + toIPAddress + ":./remoteExperiment/ ";

		log.trace(MARKER, rsyncCmd);

		Session.Command result = executeCmd(rsyncCmd);
		if (result.getExitStatus() != 0) {
			log.error(ERROR, "RSYNC to {} failed, cmd result = \n\n {}\n\n", ipAddress, result);
			return false;
		}
		long endTime = System.nanoTime();
		log.trace(MARKER, "took {} seconds to rsync from node {} to node {}", (endTime - startTime) / 1000000000,
				ipAddress, toIPAddress);
		return true;
	}

	void scpToSpecificFiles(ArrayList<File> filesToUpload) {

		makeRemoteDirectory(RegressionUtilities.REMOTE_EXPERIMENT_LOCATION);
		long startTime = System.nanoTime();
		for (File file : filesToUpload) {
			ArrayList<String> uploadValues = splitDirectorys(file, true);
			try {
				ssh.newSCPFileTransfer().upload(uploadValues.get(1), uploadValues.get(0));

			} catch (IOException e) {
				log.error(ERROR, "could not upload {} to {}", uploadValues.get(0), ipAddress, e);
			}
		}
		long endTime = System.nanoTime();
		log.info(MARKER, "took {} milliseconds to upload {} ({}) files to {}", (endTime - startTime) / 1000000,
				filesToUpload.size(), filesToUpload.get(0).getName(), getIpAddress());
	}

	private void makeRemoteDirectory(String newDir) {
		log.trace(MARKER, "Making new Dir: {} ", newDir);
		String commandStr = "mkdir " + newDir;
		final Session.Command cmd = execCommand(commandStr, "make new directory:" + newDir, -1);
		log.info(MARKER, "** exit status for making directory: {} was {} ", newDir, cmd.getExitStatus());
	}

	private void makeRemoteDirPath(String dirPath) {
		String commandStr = "mkdir -p " + dirPath;
		String desc = "make new dir path:" + dirPath;
		Session.Command cmd = execCommand(commandStr, desc);
		throwIfExitCodeBad(cmd, desc);
	}

	void extractTar() {
		String cmdString =
				"cd " + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION + "; tar -zxvf " + RegressionUtilities.TAR_NAME + "; rm *.tar.gz ";
		final Session.Command cmd = execCommand(cmdString, "extract tar downloaded from node:" + this.ipAddress, -1);
		log.info(MARKER, "** exit status: " + cmd.getExitStatus() + " :: " + cmd.getExitErrorMessage());
	}

	ArrayList<String> splitDirectorys(File file, boolean isUpload) {
		ArrayList<String> returnList = new ArrayList<>();

		String[] splitPaths = file.getPath().split("[\\\\/]");
		String parentDirs = RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
		String baseDir;
		if (file.isDirectory() && splitPaths.length > 1) {
			for (int i = 0; i < splitPaths.length - 1; i++) {
				if (splitPaths[i].equals(".")) {
					continue;
				} // "." causes issues with directories.
				/* make sure it ends in "/" for upload() and prepare in case more subdirectories */
				parentDirs += splitPaths[i] + "/";
					/* due to the way SCPFileTransfar().upload() works the directory will need to be there,
					or SCP will dump the files in the directory in the parent directory */

				makeRemoteDirectory(parentDirs);

			}
			baseDir = file.getPath() + "/";

			returnList.add(parentDirs);
			returnList.add(baseDir);
		} else {
			returnList.add(parentDirs); //this is the base parentDirs set before the if statemenr
			returnList.add(file.getPath());
		}
		return returnList;
	}

	int execWithProcessID(String jvmOptions) {
		if (jvmOptions == null || jvmOptions.trim().length() == 0) {
			jvmOptions = RegressionUtilities.JVM_OPTIONS_DEFAULT;
		}
		String command = String.format(
				"cd %s; " +
						"java %s -Dlog4j.configurationFile=log4j2-regression.xml -jar swirlds.jar >output.log 2>&1 & " +
						"disown -h",
				RegressionUtilities.REMOTE_EXPERIMENT_LOCATION,
				jvmOptions);
		String description = "Start swirlds.jar";

		return execCommand(command, description, 5).getExitStatus();
	}


	int makeSha1sumOfStreamedEvents(String testName, int streamingServerNode, int testDuration) {
		final String streamDir = findStreamDirectory();
		if (streamDir == null) {
			return -1;
		}
		final String baseDir = "~/" + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
		final String evts_list = baseDir + EVENT_FILE_LIST;
		final String sha1Sum_log = baseDir + EVENT_LIST_FILE;
		final String finalHashLog = baseDir + FINAL_EVENT_FILE_HASH;
		final String evtsSigList = baseDir + EVENT_SIG_FILE_LIST;
		final String commandStr = String.format(
				"cd %s; " +
						"wc -c *.evts > %s ; " +    //create a list of file name and byte size
						"sha1sum *.evts > %s; " +  //create a list of hash of individual evts file
						"ls *.evts_sig > %s;" +
						"sha1sum %s > %s;",
				streamDir, evts_list, sha1Sum_log, evtsSigList, sha1Sum_log, finalHashLog);
		final String description =
				"Create sha1sum of .evts files on node: " + streamingServerNode + " dir: " + streamDir;
		log.trace(MARKER, "Hash creation commandStr = {}", commandStr);

		Session.Command result = execCommand(commandStr, description);
		if (result != null) {
			return result.getExitStatus();
		} else {
			return -1;
		}
	}

	private String findStreamDirectory() {
		if (this.streamDirectory != null && !"".equals(this.streamDirectory)) {
			return this.streamDirectory;
		}
		String commandStr = "find remoteExperiment -name \"*.evts\" | xargs dirname | sort -u";
		final Session.Command cmd = execCommand(commandStr, "Find where event files are being stored", -1);
		log.trace(MARKER, "Find events directory {}: ", ipAddress, commandStr);
		Collection<String> returnCollection = readCommandOutput(cmd);
		if (returnCollection.size() > 0) {
			log.info(MARKER, "Events Directory: {}", ((ArrayList<String>) returnCollection).get(0));
			this.streamDirectory = ((ArrayList<String>) returnCollection).get(0) + "/";
			return this.streamDirectory;
		}

		return null;
	}

	Session.Command executeCmd(String command) {
		final Session.Command cmd = execCommand(command, "generic command call:" + command, -1);
		log.info(MARKER, "**{} exit status: {} :: {}", command, cmd.getExitStatus(), cmd.getExitErrorMessage());
		return cmd;
	}

	private Session.Command execCommand(String command, String description) {
		return execCommand(command, description, -1, true);
	}

	private Session.Command execCommand(String command, String description, int joinSec) {
		return execCommand(command, description, joinSec, true);
	}

	private Session.Command execCommand(String command, String description, int joinSec, boolean reconnectIfNeeded) {
		int returnValue = -1;
		try {
			if (reconnectIfNeeded) {
				reconnectIfNeeded();
			}
			session = ssh.startSession();
			final Session.Command cmd = session.exec(command);
			if (joinSec <= 0) {
				cmd.join();
			} else {
				cmd.join(joinSec, TimeUnit.SECONDS);
			}
			returnValue = cmd.getExitStatus();
			log.trace(MARKER, "'{}' command:\n{}\nexit status: {} :: {}",
					description, command, returnValue, cmd.getExitErrorMessage());
			lastExec = Instant.now();
			return cmd;
		} catch (ConnectionException e) {
			log.error(ERROR, " Join wait time out, joinSec={} command={} description={}", e, joinSec, command,
					description);
		} catch (IOException | NullPointerException e) {
			log.error(ERROR, "'{}' command failed!", description, e);
		} catch (Exception e) {
			log.error(ERROR, "Unexpected error, joinSec={} command={} description={}", e, joinSec, command,
					description);
		} finally {
			try {
				if (session != null) {
					session.close();
				}
			} catch (IOException e) {
				log.error(ERROR, "could not close node {} session when executing '{}'",
						ipAddress, description, e);
			}
		}
		return null;
	}

	void killJavaProcess() {
		final Session.Command cmd = execCommand(RegressionUtilities.KILL_JAVA_PROC_COMMAND,
				"command to kill the java process", -1);
		log.info(MARKER, "**java kill exit status: " + cmd.getExitStatus() + " :: " + cmd.getExitErrorMessage());
	}

	SSHClient buildSession() {
		SSHClient client = new SSHClient();
		int count = 0;
		while (count < 10) {
			try {
				KeyProvider keys = client.loadKeys(keyFile.getPath());
				client.addHostKeyVerifier(new PromiscuousVerifier());
				client.useCompression();
				client.connect(this.ipAddress);
				client.authPublickey(this.user, keys);

				if (client.isConnected()) {
					return client;
				}
			} catch (IOException | IllegalThreadStateException e) {
				log.error(ERROR, "attempt {} to connect to node {} failed, will retry {} more times.", count,
						this.ipAddress, 10 - count, e);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error(ERROR, "Unable to sleep thread before retrying to connect to {}", this.ipAddress, e);
			}
			count++;
		}
		log.error(ERROR, "Could not connect with the node over ssh");
		return null;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public ArrayList<String> getFiles() {
		return files;
	}

	public void setFiles(ArrayList<String> files) {
		this.files = files;
	}

	public File getKeyFile() {
		return keyFile;
	}

	public void setKeyFile(File keyFile) {
		this.keyFile = keyFile;
	}

	public boolean isConnected() {
		if (ssh == null) {
			return false;
		} else {
			return ssh.isConnected();
		}
	}

	private void reconnectIfNeeded() {
		//TODO
		if (lastExec.until(Instant.now(), SECONDS) > RegressionUtilities.SSH_TEST_CMD_AFTER_SEC) {
			execCommand("echo test", "test if connection broken", -1, false);
		}
		if (ssh.isConnected()) {
			return;
		}
		close();
		log.debug(MARKER, "Reconnecting to node {}", this.ipAddress);
		ssh = buildSession();
		lastExec = Instant.now();
	}

	public boolean checkProcess() {
		Session.Command cmd = execCommand(RegressionUtilities.CHECK_JAVA_PROC_COMMAND,
				"Check if java process is running", 2);

		if (cmd != null) {
			ArrayList<String> output = readCommandOutput(cmd);

			if (output.size() == 0 || "".equals(output.get(0))) {
				log.info(MARKER, "Java proc is DOWN");
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	public boolean isTestFinished() {
		final Session.Command cmd = execCommand(RegressionUtilities.CHECK_FOR_PTD_TEST_MESSAGE,
				"Check if PTD test is done", -1);
		ArrayList<String> output = readCommandOutput(cmd);
		for (String out : output) {
			log.trace(MARKER, "is test finished output size({}): {}", output.size(), out);
			if (out.contains("No such file or directory")) {
				log.error(ERROR, "Something wrong, test is not running. No swirlds.log found");
				return true;
			}
		}
		/* egrep gives a blank line of output if it finds nothing */
		if (output.size() == 0 || output.get(0).isEmpty()) {
			log.trace(MARKER, "Test is not finished");
			return false;
		}

		return true;
	}

	/**
	 * Return number of test finished message, such as SUCCESS or FAIL, found in log
	 * Or return -1 if error happened
	 */
	public int countTestFinishedMsg() {
		final Session.Command cmd = execCommand(RegressionUtilities.CHECK_FOR_PTD_TEST_MESSAGE,
				"Check if PTD test is done", -1);
		ArrayList<String> output = readCommandOutput(cmd);
		for (String out : output) {
			log.trace(MARKER, "is test finished output size({}): {}", output.size(), out);
			if (out.contains("No such file or directory")) {
				log.error(ERROR, "Something wrong, test is not running. No swirlds.log found");
				return -1;
			}
		}
		if (output.get(0).isEmpty()) {
			return -1;
		}
		return output.size();
	}

	public void close() {
		try {
			ssh.close();
		} catch (Exception e) {
			log.error(ERROR, "Error while closing old connection to {}", this.ipAddress, e);
		}
	}

	public boolean reset() {
		killJavaProcess();
		Session.Command cmd = execCommand(RegressionUtilities.RESET_NODE,
				"Preparing node for next experiment", 2);
		boolean isDatabaseReset = resetRemoteDatabase();
		if (cmd == null || !isDatabaseReset) {
			return false;
		}
		return true;
	}

	private String getFirstLineOfStream(InputStream is) {
		BufferedReader inputBR = new BufferedReader(new InputStreamReader(is));
		try {
			return inputBR.readLine();
		} catch (IOException e) {
			log.error(ERROR, "can't read input from drop db extension command.", e);
			return null;
		}
	}

	private boolean resetRemoteDatabase() {
		Session.Command dbDropExtensionCmd = execCommand(RegressionUtilities.DROP_DATABASE_EXTENSION_BEFORE_NEXT_TEST,
				"Dropping fcfs extension");
		String dropExtensionInputReturn = getFirstLineOfStream(dbDropExtensionCmd.getInputStream());
		String dropExtensionErrorReturn = getFirstLineOfStream(dbDropExtensionCmd.getErrorStream());
		log.info(MARKER, "Dropping extension: input {} \t error {}", dropExtensionInputReturn,
				dropExtensionErrorReturn);
		if (dropExtensionErrorReturn != null) {
			log.info(MARKER, "Drop fcfs extension produced this error: {}", dropExtensionErrorReturn);
		}

		Session.Command dbDropTableCmd = execCommand(RegressionUtilities.DROP_DATABASE_FCFS_TABE_BEFORE_NEXT_TEST,
				"Dropping fcfs table");
		String dropDBInputReturn = getFirstLineOfStream(dbDropTableCmd.getInputStream());
		String dropDBErrorReturn = getFirstLineOfStream(dbDropTableCmd.getErrorStream());
		log.info(MARKER, "Dropping database: input {} \t error {}", dropDBInputReturn, dropDBErrorReturn);
		/* database can still be tired up by a lingering java process, kill the javaprocess and retry dropping the db */
		if (DROP_DATABASE_FCFS_KNOWN_RESPONCE.equals(dropDBErrorReturn)) {
			killJavaProcess();
			dbDropTableCmd = execCommand(RegressionUtilities.DROP_DATABASE_FCFS_TABE_BEFORE_NEXT_TEST,
					"Dropping fcfs table");
			dropDBInputReturn = getFirstLineOfStream(dbDropTableCmd.getInputStream());
			dropDBErrorReturn = getFirstLineOfStream(dbDropTableCmd.getErrorStream());
			log.info(MARKER, "Dropping database retry: input {} \t error {}", dropDBInputReturn, dropDBErrorReturn);
		}

		Session.Command dbCreateTableCmd = execCommand(RegressionUtilities.CREATE_DATABASE_FCFS_TABE_BEFORE_NEXT_TEST,
				"recreating fcfs table before next test");
		String dbCreateInputReturn = getFirstLineOfStream(dbCreateTableCmd.getInputStream());
		String dbCreateErrorReturn = getFirstLineOfStream(dbCreateTableCmd.getErrorStream());
		log.info(MARKER, "creating database: input {} \t error {}", dbCreateInputReturn, dbCreateErrorReturn);
		if (DROP_DATABASE_FCFS_EXPECTED_RESPONCE.equals(
				dropDBInputReturn) && CREATE_DATABASE_FCFS_EXPECTED_RESPONCE.equals(dbCreateInputReturn)) {
			log.info(MARKER, "Successfully dropped and recreated db");
			return true;
		}
		log.error(ERROR, "something went wrong with database drop/recreate");
		return false;
	}

	public void copyS3ToInstance(String source, String destination) {
		String command = String.format(
				"aws s3 cp %s %s --recursive",
				source, destination
		);
		String description = String.format(
				"Copy '%s' from S3 to '%s:%s'",
				source, ipAddress, destination
		);
		Session.Command cmd = execCommand(
				command,
				description
		);
		throwIfExitCodeBad(cmd, description);
	}

	void restoreDb(String tarGzPath) {
		if (tarGzPath == null) {
			throw new IllegalArgumentException("tarGzPath should not be null");
		}
		if (!tarGzPath.endsWith(".tar.gz")) {
			throw new IllegalArgumentException("tarGzPath should end with '.tar.gz'");
		}
		String tarPath = tarGzPath.substring(0, tarGzPath.lastIndexOf('.'));

		String command = String.format(
				"test -f %s && gunzip %s && sudo -u postgres pg_restore -d fcfs -F t %s",
				tarGzPath, tarGzPath, tarPath
		);
		String description = String.format(
				"Restore db from file '%s'",
				tarGzPath
		);
		Session.Command cmd = execCommand(command, description);
		throwIfExitCodeBad(cmd, description);
	}

	private void throwIfExitCodeBad(Session.Command cmd, String description) {
		if (cmd.getExitStatus() != 0) {
			String output = readCommandOutput(cmd).toString();
			throw new RuntimeException(
					String.format("'%s' FAILED with error code '%d'. Output:\n%s",
							description, cmd.getExitStatus(), output
					)
			);
		}
	}

	/**
	 * List names of signed state directories currently on disk
	 *
	 * @param memo
	 * 		Memo string
	 */
	void displaySignedStates(String memo) {
		String displayCmd =
				"ls -tr " + SAVED_STATE_LOCATION;
		Session.Command cmd = executeCmd(displayCmd);
		log.info(MARKER, "Node {}: States directories {} are {}", ipAddress, memo, readCommandOutput(cmd).toString());
	}

	private boolean deleteLastSignedState() {
		// this command returns the last directory, which is the round number
		// of last signed state
		String findLastStateCmd =
				"ls -tr " + SAVED_STATE_LOCATION + " | tail -1";
		Session.Command cmd = executeCmd(findLastStateCmd);
		if (cmd.getExitStatus() == 0) {
			String findLastStateRound = readCommandOutput(cmd).toString();
			findLastStateRound = findLastStateRound.replace("[", "").replace("]", ""); //remove bracket

			String rmStatesCmd =
					"rm -r " + SAVED_STATE_LOCATION + "/" + findLastStateRound;
			cmd = executeCmd(rmStatesCmd);
			if (cmd.getExitStatus() != 0) {
				log.error(ERROR, "Exception running rm state command {}", rmStatesCmd);
				return false;
			}

		} else {
			log.error(ERROR, "Exception running findLastStateCmd command {} cmd result {}", findLastStateCmd
					, readCommandOutput(cmd).toString());
			return false;
		}
		return true;
	}

	int getSignedStatesAmount() {
		// first find out how many signed state saved to disk, parse the result string to a number
		// this return how many signed state (sub-directories) created
		String lsStatesCmd =
				"ls -tr " + SAVED_STATE_LOCATION + " | wc -l";

		Session.Command cmd = executeCmd(lsStatesCmd);
		if (cmd.getExitStatus() == 0) {
			String result = readCommandOutput(cmd).toString();
			result = result.replace("[", "").replace("]", ""); //remove bracket
			try {
				return Integer.parseInt(result);
			} catch (NumberFormatException e) {
				log.error(ERROR, "Could not parse result of ls command {}", lsStatesCmd, e);
			}
		}
		return 0;
	}

	/**
	 * Delete multiple signed state saved on disk
	 */
	void deleteSignedStates(int deleteAmount) {
		displaySignedStates("BEFORE deleting States");
		log.info(MARKER, "Random delete {} signed states", deleteAmount);
		for (int i = 0; i < deleteAmount; i++) {
			if (!deleteLastSignedState()) {
				break;
			}
		}
		displaySignedStates("AFTER deleting States");
	}

	/**
	 * List event files created during recover mode
	 */
	void displayRecoveredEventFiles(String eventDir) {
		String displayCmd =
				"ls -tr " + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION + eventDir + "*.evts";
		Session.Command cmd = executeCmd(displayCmd);
		log.info(MARKER, "Node {}: Event files created during recover are {}", ipAddress, readCommandOutput(cmd).toString());
	}

	/**
	 * Compare event files generated during recover mode whether match original ones
	 *
	 * @param eventDir
	 * @param originalDir
	 * @return
	 */
	boolean checkRecoveredEventFiles(String eventDir, String originalDir) {

		// compare generated event stream files with original ones, ignore only files exist in original ones
		String compareCmd = "diff  " + REMOTE_EXPERIMENT_LOCATION + eventDir
				+ " " + REMOTE_EXPERIMENT_LOCATION + originalDir + " | grep diff | wc -l";

		Session.Command cmd = executeCmd(compareCmd);

		// return string is a number with bracket
		String cmdResult = readCommandOutput(cmd).toString();
		cmdResult = cmdResult.replace("[", "").replace("]", ""); //remove bracket

		try {
			if (Integer.parseInt(cmdResult) == 0) {
				log.info(MARKER, "Found NO difference in recovered event files and original ones");
				executeCmd("echo \"" + EVENT_MATCH_MSG + "\"  >> " +
						REMOTE_EXPERIMENT_LOCATION + EVENT_MATCH_LOG_NAME);
				return true; // no difference found
			} else {
				log.info(MARKER, "Found difference in recovered event files and original ones");
				return false;
			}
		} catch (NumberFormatException e) {
			log.error(ERROR, "Exception ", e);
			return false;
		}
	}

}