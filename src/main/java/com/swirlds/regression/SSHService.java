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

import com.swirlds.regression.jsonConfigs.TestConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.swirlds.regression.RegressionUtilities.CHANGE_HUGEPAGE_NUMBER;
import static com.swirlds.regression.RegressionUtilities.CHANGE_POSTGRES_MEMORY_ALLOCATION;
import static com.swirlds.regression.RegressionUtilities.CHECK_FOR_STATE_MANAGER_QUEUE_MESSAGE;
import static com.swirlds.regression.RegressionUtilities.CREATE_DATABASE_FCFS_EXPECTED_RESPONCE;
import static com.swirlds.regression.RegressionUtilities.DROP_DATABASE_FCFS_EXPECTED_RESPONCE;
import static com.swirlds.regression.RegressionUtilities.DROP_DATABASE_FCFS_KNOWN_RESPONCE;
import static com.swirlds.regression.RegressionUtilities.EVENT_MATCH_MSG;
import static com.swirlds.regression.RegressionUtilities.HEDERA_NODE_JAR;
import static com.swirlds.regression.RegressionUtilities.REMOTE_EXPERIMENT_LOCATION;
import static com.swirlds.regression.RegressionUtilities.REMOTE_STATE_LOCATION;
import static com.swirlds.regression.RegressionUtilities.SAVED_STATE_LOCATION;
import static com.swirlds.regression.RegressionUtilities.START_POSTGRESQL_SERVICE;
import static com.swirlds.regression.RegressionUtilities.STOP_POSTGRESQL_SERVICE;
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
    private static final long MAXIMUM_TIMEOUT_ALLOWANCE = 500; // seconds


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
		int extensionCount = 0;
		int dataExtensionCount = 0;
        String extensions = "\\( ";
		String dataExtensions = "";
        for (int i = 0; i < extension.size(); i++) {
			if (!extension.get(i).contains("data/")) {
				if (extensionCount++ > 0) {
					extensions += " -o ";
				}
				extensions += "-name \"" + extension.get(i) + "\"";
			}
			else {
				dataExtensionCount++;
				dataExtensions += "-e \"" + extension.get(i) + "\" ";
			}
		}
        extensions += " \\) ";

		if (extensionCount > 0) {
			String commandStr = "find . " + extensions;
			String pruneDirectory = "-not -path \"*/data/*\"";
			commandStr += pruneDirectory;

	        Session.Command cmd = null;
	        while ((cmd = execCommand(commandStr, "Find list of Files based on extension", -1)) == null) {

	            try {
	                log.info(MARKER, "Connection might be down? Sleeping for 10 seconds and retrying..");
	                Thread.sleep(10000);
	            } catch (InterruptedException ie) {
	                log.error(ERROR, "Unable to sleep thread before retrying to execCommand {}", commandStr);
	            }
	        }
			log.info(MARKER, "Extensions to look for on node {}: {}", ipAddress, extensions);

			returnCollection.addAll(readCommandOutput(cmd));
		}

		if (dataExtensionCount > 0) {
			String dataCommandStr = "find . | grep " + dataExtensions;
			
			Session.Command dataCmd = null;
			while((dataCmd = execCommand(dataCommandStr, "Find list of Files based on name", -1)) == null) {
			
				try {
	                log.info(MARKER, "Connection might be down? Sleeping for 10 seconds and retrying..");
	                Thread.sleep(10000);
	            } catch (InterruptedException ie) {
	                log.error(ERROR, "Unable to sleep thread before retrying to execCommand {}", dataCommandStr);
	            }
	        }
			log.info(MARKER, "Files to look for on node {}: {}", ipAddress, dataExtensions);
			returnCollection.addAll(readCommandOutput(dataCmd));
		}

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

    boolean scpFrom(String topLevelFolders, ArrayList<String> downloadExtensions) {
        try {
            log.info(MARKER, "top level folder: {}", topLevelFolders);
            Collection<String> foundFiles = getListOfFiles(
                    RegressionUtilities.getSDKFilesToDownload(downloadExtensions));
			scpFilesFromList(topLevelFolders, foundFiles);
			return true;
		} catch (IOException | StringIndexOutOfBoundsException e) {
			log.error(ERROR, "Could not download files", e);
			return false;
		}
	}

	void scpFromListOnly(String topLevelFolders, ArrayList<String> patternsToMatch) {
		try {
			log.info(MARKER, "top level folder: {}", topLevelFolders);
			Collection<String> foundFiles = getListOfFiles(patternsToMatch);
			scpFilesFromList(topLevelFolders, foundFiles);
		} catch (IOException | StringIndexOutOfBoundsException e) {
			log.error(ERROR, "Could not download files", e);
		}
	}

	private void scpFilesFromList(String topLevelFolders, Collection<String> foundFiles) throws IOException {
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

    // Use rsync to copy selected files to a list of IP addresses
    public boolean rsyncTo(ArrayList<File> additionalFiles, File log4j2Xml, List<String> toIPAddresses) {
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

        // chain multiple rsync command together and run them in background
        String addCmd = "";
        for(String address:toIPAddresses){
            addCmd += rsyncCmd + "--exclude=\"*\" --delete --delete-excluded ./" + RegressionUtilities.REMOTE_EXPERIMENT_LOCATION +
                    " " + user + "@" + address + ":./remoteExperiment/ & ";
        }
        // wait all background rsync command to finish
        addCmd += " wait  ";
        log.trace(MARKER, "rsyncTo cmd = " + addCmd );

        // command running on remote has format " rsync ip1 & rsync ip2 & rsync ip3 & rsync ip4 & wait"
        // all rsync run in background in parallel and "wait" make executeCmd returns only when all rsync are done
        Session.Command result = executeCmd(addCmd);
        if (result.getExitStatus() != 0) {
            String cmdResultString = readCommandOutput(result).toString();
            log.error(ERROR, "RSYNC to {} failed, cmd result = \n\n {}\n\n", ipAddress, cmdResultString);
            return false;
        }
        long endTime = System.nanoTime();
        log.trace(MARKER, "took {} seconds to rsync from node {} to node {}", (endTime - startTime) / 1000000000,
                ipAddress, toIPAddresses);
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
        logIfExitCodeBad(cmd, desc);
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
                        "java %s -Dlog4j.configurationFile=log4j2-regression.xml -jar swirlds.jar >>output.log 2>&1 & " +
                        "disown -h",
                RegressionUtilities.REMOTE_EXPERIMENT_LOCATION,
                jvmOptions);
        String description = "Start swirlds.jar";

        return execCommand(command, description, 5).getExitStatus();
    }


    int execHGCAppWithProcessID(String jvmOptions) {
        if (jvmOptions == null || jvmOptions.trim().length() == 0) {
            jvmOptions = RegressionUtilities.JVM_OPTIONS_DEFAULT;
        }

        String command = String.format(
                "cd %s; " +
                        "java %s -Dlog4j.configurationFile=log4j2-regression.xml -cp 'data/lib/*' com.swirlds.platform.Browser >>output.log 2>&1 & " +
                        "disown -h",
                RegressionUtilities.REMOTE_EXPERIMENT_LOCATION,
                jvmOptions);
        String description = "Start Browser";

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
                cmd.join(MAXIMUM_TIMEOUT_ALLOWANCE, TimeUnit.SECONDS);
            } else {
                cmd.join(joinSec, TimeUnit.SECONDS);
            }
            returnValue = cmd.getExitStatus();
            log.trace(MARKER, "'{}' command:\n{}\nexit status: {} :: {}",
                    description, command, returnValue, cmd.getExitErrorMessage());
            lastExec = Instant.now();
            return cmd;
        } catch (ConnectionException e) {
            log.error(ERROR, " Join wait time out, joinSec={} command={} description={}", joinSec, command,
                    description, e);
        } catch (IOException | NullPointerException e) {
            log.error(ERROR, "'{}' command failed!", description, e);
        } catch (Exception e) {
            log.error(ERROR, "Unexpected error, joinSec={} command={} description={}", joinSec, command,
                    description, e);
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

    void killNetwork() {
        final Session.Command cmd = execCommand(RegressionUtilities.KILL_NET_COMMAND,
                "command to kill the network", -1);
        log.info(MARKER, "**network kill exit status: " + cmd.getExitStatus() + " :: " + cmd.getExitErrorMessage());
    }

    void reviveNetwork() {
        final Session.Command cmd = execCommand(RegressionUtilities.REVIVE_NET_COMMAND,
                "command to revive the network", -1);
        log.info(MARKER, "**network revive exit status: " + cmd.getExitStatus() + " :: " + cmd.getExitErrorMessage());
    }

    String checkTotalMemoryOnNode() {
        final Session.Command cmd = execCommand(RegressionUtilities.GET_TOTAL_MB_MEMORY_ON_NODE, "command to get total MB of memory on node", -1);
        log.info(MARKER, "Check memory exit status:" + cmd.getExitStatus() + " :: " + cmd.getExitErrorMessage());
        /* M is added because vmstat doesn't have a human readable option. but the -SM option forces it to report in MB */
        return getFirstLineOfStream(cmd.getInputStream()) + "MB";
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
        if (isConnected()) {
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
     * Search a lists of messages in the log file and
     * return the total number of occurrences of all messages
     * Or return -1 if error happened
     *
     * @param msgList  A list of message to search for
     * @param fileName File name to search for the message
     */
    int countSpecifiedMsg(List<String> msgList, String fileName) {
        String joined = String.join("|", msgList);
        String searchCmd = "egrep " + " \"" + joined + "\" " + fileName;
        String countCmd = " | wc -l";
        final Session.Command cmd = execCommand(searchCmd + countCmd ,
                "Check specified msg", -1);
        ArrayList<String> output = readCommandOutput(cmd);
        for (String out : output) {
            if (out.contains("No such file or directory")) {
                log.error(ERROR, "Something wrong, test is not running. No swirlds.log found");
                return -1;
            }
        }

        return Integer.valueOf(output.get(0));
    }

    /**
     * Search a lists of messages in the log file and
     * return the number of occurrences of each message
     *
     * @param msgList  A list of message to search for
     * @param fileName File name to search for the message
     * @return a Map which contains the number of occurrences of each message
     *      Or return null if error happened
     */
    Map<String, Integer> countSpecifiedMsgEach(List<String> msgList, String fileName) {
        String joined = String.join("|", msgList);
        String searchCmd = "egrep  -Iho " + " \"" + joined + "\" " + fileName;
        String countCmd = " | sort | uniq -c";
        final Session.Command cmd = execCommand(searchCmd + countCmd ,
                "Check specified msg", -1);
        ArrayList<String> output = readCommandOutput(cmd);
        Map<String, Integer> map = new HashMap<>();
        for (String out : output) {
            if (out.contains("No such file or directory")) {
                log.error(ERROR, "Something wrong, test is not running. No {} found", fileName);
                return null;
            }

            log.trace(MARKER, out);
            out = out.trim();
            // if the string is Empty, it means no occurrence
            // if the string is not Empty, it should be: "num string"
            if (!out.isEmpty()) {
                String[] strs = out.split(" ", 2);
                map.put(strs[1].trim(), Integer.valueOf(strs[0]));
            }
        }
        log.trace(MARKER, "countSpecifiedMsgEach resultMap: {}", map);
        return map;
    }

    /**
     * Checks file for messages reporting that the state has been successfully saved
     * and that a pg_backup has successfully been processed for each rounnd
     *
     * @param fileName File name to search for the message
     *
     * @return a map containing round numbers and whether their backup was completed
     * for a given node
     */
    HashMap<Long,Boolean> checkSavedStateProgress(String fileName) {
        HashMap<Long,Boolean> retMap = new HashMap<>();

        final Session.Command cmd = execCommand(CHECK_FOR_STATE_MANAGER_QUEUE_MESSAGE,
                "Get all state manager queuing messages", -1);;
        ArrayList<String> output = readCommandOutput(cmd);
        try {
            for (String out : output) {

                if (out.contains("No such file or directory")) {
                    log.error(ERROR, "Something wrong, test is not running. No swirlds.log found");
                    return null;
                }

                Long roundNum = null;
                Boolean complete = null;

                String[] keyVals = out.split(", ");
                for (String keyVal : keyVals) {
                    String[] parts = keyVal.split("=", 2);
                    if (parts.length == 2) {
                        if (parts[0].contentEquals("roundNumber")) {
                            roundNum = Long.parseLong(parts[1]);
                        } else if (parts[0].contentEquals("complete")) {
                            if (parts[1].contentEquals("false")) {
                                complete = false;
                            } else if (parts[1].contentEquals("true")) {
                                complete = true;
                            } else {
                                throw new NumberFormatException();
                            }
                        }
                    }
                }

                if ((roundNum != null) && (complete != null)) {
                    if (complete == false) {
                        if (retMap.containsKey(roundNum)) {
                            //already know this round completed
                            continue;
                        } else {
                            retMap.put(roundNum, false);
                        }
                    } else {
                        //round number completed
                        retMap.put(roundNum, true);
                    }
                } else {
                    throw new SSHException("invalid line read");
                }
            }
        } catch (NumberFormatException | SSHException e) {
            log.error(ERROR,"State message manager improperly formed");
        }
        if (retMap.size() == 0) {
            log.info(MARKER,"No saved rounds found for node");
            return null;
        }
        return retMap;
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

        Session.Command dbDropTableCmd = execCommand(RegressionUtilities.DROP_DATABASE_FCFS_TABLE_BEFORE_NEXT_TEST,
                "Dropping fcfs table");
        String dropDBInputReturn = getFirstLineOfStream(dbDropTableCmd.getInputStream());
        String dropDBErrorReturn = getFirstLineOfStream(dbDropTableCmd.getErrorStream());
        log.info(MARKER, "Dropping database: input {} \t error {}", dropDBInputReturn, dropDBErrorReturn);
        /* database can still be tired up by a lingering java process, kill the javaprocess and retry dropping the db */
        if (DROP_DATABASE_FCFS_KNOWN_RESPONCE.equals(dropDBErrorReturn)) {
            killJavaProcess();
            dbDropTableCmd = execCommand(RegressionUtilities.DROP_DATABASE_FCFS_TABLE_BEFORE_NEXT_TEST,
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
        logIfExitCodeBad(cmd, description);
    }

    public void listDirFiles(String destination) {
        String command = String.format(
                "ls -al %s",
                destination
        );
        String description = String.format(
                "list files in %s: %s",
                ipAddress, destination
        );
        Session.Command cmd = execCommand(
                command,
                description
        );
        ArrayList<String> output = readCommandOutput(cmd);
        for (String outputStr : output) {
            log.info(MARKER, outputStr);
        }
        logIfExitCodeBad(cmd, description);
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
                "test -f %s && gunzip --keep %s && sudo -u postgres pg_restore -d fcfs -F t %s",
                tarGzPath, tarGzPath, tarPath
        );
        String description = String.format(
                "Restore db from file '%s'",
                tarGzPath
        );
        Session.Command cmd = execCommand(command, description, (int) MAXIMUM_TIMEOUT_ALLOWANCE);
        logIfExitCodeBad(cmd, description);
    }

    private void logIfExitCodeBad(Session.Command cmd, String description) {
        if (cmd.getExitStatus() != 0) {
            String output = readCommandOutput(cmd).toString();
            log.error(ERROR,"'{}' FAILED with error code '{%d}. Output: {}",description,cmd.getExitStatus(),output);
        }
    }

    /**
     * Read a list of directories of signed state from file system, parse them to a list of SavedStatePathInfo
     * instances
     * <p>
     * Example of returned string value from ls command
     * <p>
     * [remoteExperiment/data/saved/com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/1,
     * remoteExperiment/data/saved/com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/185,
     * remoteExperiment/data/saved/com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/30,
     * remoteExperiment/data/saved/com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/351,
     * remoteExperiment/data/saved/com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/515,
     * remoteExperiment/data/saved/com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/674]
     *
     * @return A list of SavedStatePathInfo instances
     */
    public List<SavedStatePathInfo> getSavedStatesDirectories() {
        List<SavedStatePathInfo> dirList = new ArrayList<>();
        String listCmd =
                "ls -d " + getSavedStateDirectory() + "/*";
        Session.Command cmd = executeCmd(listCmd);
        if (cmd.getExitStatus() == 0) {
            String cmdResultString = readCommandOutput(cmd).toString();
            cmdResultString = cmdResultString.replace("[", "").replace("]", ""); //remove bracket
            String[] directories = cmdResultString.split(" ");
            for (String dir : directories) {
                SavedStatePathInfo result = parseSavedStatPath(dir.replace(",", ""));
                if (result != null) {
                    dirList.add(result);
                }
            }
            log.info(MARKER, "Saved states are {}", dirList.toString());
        } else {
            log.error(ERROR, "Exception running getSavedStatesDirectories command {} cmd result {}",
                    cmd, readCommandOutput(cmd).toString());
        }
        //sorting by round number
        dirList = dirList.stream().sorted(Comparator.comparingLong(SavedStatePathInfo::getRoundNumber)).collect(
                Collectors.toList());
        ;
        return dirList;
    }

    /**
     * Parse saved signed state path and return its main class name, nodeId, swirds name in a SavedStatePathInfo
     * instance
     *
     * @param path A file system path of an saved signed state. It contains the main class name of swirlds App,
     *             node ID, swirlds name and round number of the signed state
     *             <p>
     *             An example is : com.swirlds.demo.platform.PlatformTestingDemoMain/0/123/33
     * @return An SavedStatePathInfo instance contains the parsed results
     */
    private SavedStatePathInfo parseSavedStatPath(String path) {
        String pathWithoutBase = path.replace(REMOTE_STATE_LOCATION, "");
        String[] segments = pathWithoutBase.split("/");

        try {
            SavedStatePathInfo result = new SavedStatePathInfo(path, Long.parseLong(segments[3]),
                    Long.parseLong(segments[1]), segments[2], segments[0]);

            return result;
        } catch (NumberFormatException e) {
            log.error(ERROR, "Parsing saved state path {} -> {} error ", path, Arrays.toString(segments), e);
            return null;
        }

    }

    private String getSavedStateDirectory() {
        return SAVED_STATE_LOCATION;
    }

    /**
     * List names of signed state directories currently on disk
     *
     * @param memo Memo string
     */
    void displaySignedStates(String memo) {
        String displayCmd =
                "ls -tr " + getSavedStateDirectory();
        Session.Command cmd = executeCmd(displayCmd);
        log.info(MARKER, "Node {}: States directories {} are {}", ipAddress, memo, readCommandOutput(cmd).toString());
    }

    /**
     * Restore database from backup file
     */
    void recoverDatabase() {
        var list = getSavedStatesDirectories();
        String targetDir = list.get(list.size() - 1).getFullPath();
        // get the last state directory
        String restoreCmd = "cd " + targetDir + "; tar -xf PostgresBackup.tar.gz; pwd  ";
        Session.Command cmd = executeCmd(restoreCmd);
        log.info(MARKER, "Node {}: Unzip data base result is {}", ipAddress, readCommandOutput(cmd).toString());


        restoreCmd = "pwd ; sudo -u postgres psql -f \"" + REMOTE_EXPERIMENT_LOCATION + "drop_database.psql\" ";
        cmd = executeCmd(restoreCmd);
        log.info(MARKER, "Node {}: Drop data base result is {}", ipAddress, readCommandOutput(cmd).toString());

        restoreCmd = "cd " + targetDir + ";  sudo -u postgres createdb fcfs ; " +
                " chmod 666 * ;" +  // enable access
                " sudo -u postgres pg_restore  --format=d --dbname=fcfs ./ ; cd -";

        log.info(MARKER, "Before run cmd {}:", restoreCmd);
        cmd = executeCmd(restoreCmd);
        log.info(MARKER, "Node {}: Restore data base result is {}", ipAddress, readCommandOutput(cmd).toString());

    }


    /**
     * Hide expected map directory
     */
    void backupSavedExpectedMap() {
        String mvCmd = "mv " + REMOTE_EXPERIMENT_LOCATION + "data/platformtesting" + " " + REMOTE_EXPERIMENT_LOCATION + "data/platformtestingBackup";
        Session.Command cmd = executeCmd(mvCmd);
    }

    /**
     * Restore expected map directory
     */
    void restoreSavedExpectedMap() {
        String mvCmd = "mv " + REMOTE_EXPERIMENT_LOCATION + "data/platformtestingBackup" + " " + REMOTE_EXPERIMENT_LOCATION + "data/platformtesting";
        Session.Command cmd = executeCmd(mvCmd);
    }


    /**
     * Backup signed state to a temp directory
     */
    void backupSavedSignedState(String tempDir) {
        String cpCmd = "cp -r " + REMOTE_STATE_LOCATION + " " + tempDir;
        Session.Command cmd = executeCmd(cpCmd);
    }

    /**
     * Restore signed state from a temp directory
     */
    void restoreSavedSignedState(String tempDir) {
        String rmCmd = " rm -rf " + REMOTE_STATE_LOCATION + "/* ; ";
        String cpCmd = "cp -r " + tempDir + "/* " + REMOTE_STATE_LOCATION + " ; ";
        String lsCmd = " ls " + REMOTE_STATE_LOCATION;
        Session.Command cmd = executeCmd(rmCmd + cpCmd + lsCmd);
    }

    private void deleteSignedState(SavedStatePathInfo state) {
        deleteRemoteFileOrDirectory(state.getFullPath());
    }

    public boolean deleteRemoteFileOrDirectory(String path) {
        String rmStatesCmd = "rm -r " + path;
        Session.Command cmd = executeCmd(rmStatesCmd);
        if (cmd.getExitStatus() != 0) {
            log.error(ERROR, "Exception running rm command {}", rmStatesCmd);
            return false;
        } else {
            log.info(MARKER, "Delete {} is OK", path);
            return true;
        }
    }

    /**
     * Find how many signed state subdirectories have been created
     */
    int getNumberOfSignedStates() {
        List<SavedStatePathInfo> result = getSavedStatesDirectories();
        return result.size();
    }

    /**
     * Delete multiple signed state saved on disk
     */
    void deleteLastNSignedStates(int deleteAmount, List<SavedStatePathInfo> currentStates) {
        int size = currentStates.size();
        displaySignedStates("BEFORE deleting States");
        for (int i = size - deleteAmount; i < size; i++) {
            deleteSignedState(currentStates.get(i));
        }
        displaySignedStates("AFTER deleting States");
    }

    /**
     * Compare event files generated during recover mode whether match original ones
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

    void badgerize() {
        // call the badgerize.sh script that prepares database logs for download

        String command = String.format("cd /home/ubuntu/; chmod -R 777 %s; sudo ./%sbadgerize.sh -u postgres;",
                RegressionUtilities.REMOTE_EXPERIMENT_LOCATION,
                RegressionUtilities.REMOTE_EXPERIMENT_LOCATION);

        String description = "Badgerizing and taring database logs";

        Session.Command cmd = execCommand(command, description);
        logIfExitCodeBad(cmd, description);
    }

    public void adjustNodeMemoryAllocations(NodeMemory memoryNeeds) {
        adjustNodeHugePageNumber(memoryNeeds.getHugePagesNumber());
        adjustNodePostgresMemory(memoryNeeds);
    }

	private void adjustNodePostgresMemory(NodeMemory postgresMemoryReqs) {
//        "sudo sed -i 's/shared_buffers = [0-9]*MB/shared_buffers = %dMB/g\n" +
//			"s/temp_buffers = [0-9]*MB/temp_buffers = %dMB/g\n" +
//			"s/max_prepared_transactions = [0-9]*/max_prepared_transactions = %d/g\n" +
//        "s/work_mem = [0-9]*MB/work_mem = %dMB/g\n" +
//                "s/maintenance_work_mem = [0-9]*MB/maintenance_work_mem = %dMB/g\n" +
//                "s/autovacuum_work_mem = [0-9]*MB/autovacuum_work_mem = %dMB/g' /etc/postgresql/10/main/postgresql.conf";
        MemoryType mb = MemoryType.MB;
        String adjustPostgresMemoryCommand = String.format(CHANGE_POSTGRES_MEMORY_ALLOCATION,(int)postgresMemoryReqs.getPostgresSharedBuffers().getAdjustedMemoryAmount(mb)
        , (int)postgresMemoryReqs.getPostgresTempBuffers().getAdjustedMemoryAmount(mb), postgresMemoryReqs.getPostgresMaxPreparedTransaction()
        , (int)postgresMemoryReqs.getPostgresWorkMem().getAdjustedMemoryAmount(mb), (int)postgresMemoryReqs.getPostgresMaintWorkMem().getAdjustedMemoryAmount(mb)
        , (int)postgresMemoryReqs.getPostgresAutovWorkMem().getAdjustedMemoryAmount(mb));

        Session.Command cmd = execCommand(STOP_POSTGRESQL_SERVICE, "stopping postgresql");
        logIfExitCodeBad(cmd, "stopping postgresql");

        cmd = execCommand(adjustPostgresMemoryCommand, "Adjusting postgres memory");
        logIfExitCodeBad(cmd, "adjusting postgres memory");

        cmd = execCommand(START_POSTGRESQL_SERVICE, "restarting postgresql");
        logIfExitCodeBad(cmd, "restarting postgresql");

	}

	private void adjustNodeHugePageNumber(int hugePagesNumber) {
        // CHANGE_HUGEPAGE_NUMBER = "sysctl -w vm.nr_hugepages=%d";
        String adjustHugepagesCommand = String.format(CHANGE_HUGEPAGE_NUMBER, hugePagesNumber);

        Session.Command cmd = execCommand(adjustHugepagesCommand, "Adjusting number of huge pages to " + hugePagesNumber);
        logIfExitCodeBad(cmd, "Adjusting number of huge pages to " + hugePagesNumber);
	}

	void printCurrentTime(final int nodeId) {
        String dateCmd = "date -u";

        Session.Command cmd = executeCmd(dateCmd);
        String cmdResult = readCommandOutput(cmd).toString();
        log.trace(MARKER, "node{} CurrentTime: {}", nodeId, cmdResult);
    }

}
