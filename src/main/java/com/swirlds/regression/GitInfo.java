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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class GitInfo {
	private static String windowsGit = "git.exe";
	private static String git = "git";

	String gitInfo = RegressionUtilities.GIT_NOT_FOUND;
	String gitVersion = "";
	String gitBranch = "";
	String gitLog = "gitLog.log";
	String userEmail = null;
	String OS = "";
	boolean isWindows = false;
	String currentGit = GitInfo.git;

	// String oldGitVersion = "";

	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	public static void main(String[] args) {
		GitInfo gi = new GitInfo();
	}

	public GitInfo() {
		OS = System.getProperty("os.name");
		log.info(MARKER, "running regression on: {}", OS);
		if (OS.contains("Windows")) {
			isWindows = true;
			currentGit = GitInfo.windowsGit;
		}

	}

	/**
	 * Finds the path to the swirlds-platform repository. Assumes that the current working directory
	 * is either the swirlds-platform directory or inside a directory tree which is in the swirlds-platform directory.
	 */
	protected File findSwirldsPlatform() {
		String cwd = System.getProperty("user.dir");
		File directory = new File(cwd);

		int maxDepth = 10;
		while (maxDepth-- > 0) {
			// Check if the current remote is swirlds-platform
			ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "remote", "-v");
			processBuilder.directory(directory);
			String currentRemote = runProcess(processBuilder, false);

			if (currentRemote.contains("swirlds-platform.git")) {
				// We are inside the correct repository
				return directory;
			} else {
				// Go one level higher in the directory tree
				String parent = directory.getParent();
				if (parent == null) {
					break;
				}
				directory = new File(directory.getParent());
			}
		}
		return new File(cwd);
	}

	/*TODO: switch this to JGit */
	public void gitVersionToFile() {
		ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "log", "-n", "1", "--decorate=short");
		runProcess(processBuilder, true);
	}

	public void gitVersionInfo() {
		ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "log", "-n", "1", "--decorate=short");
		processBuilder.directory(findSwirldsPlatform());
		gitInfo = runProcess(processBuilder, false);
	}

	public String getUserEmail() {
		if (userEmail == null) {
			ProcessBuilder processBuilder = new ProcessBuilder(
					currentGit, "config", "user.email");
			userEmail = runProcess(processBuilder, false).trim();
		}
		return userEmail;
	}

	public String updateGitBranch() {
		String returnString;
		ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "pull");
		returnString = runProcess(processBuilder, false);

		return returnString;
	}

	public String switchGitBranch(String branch) {
		String returnString;
		ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "checkout", branch);
		returnString = runProcess(processBuilder, false);

		return returnString;
	}

	public String stash() {
		String returnString;
		ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "stash");
		returnString = runProcess(processBuilder, false);

		return returnString;
	}

	public String unstash() {
		String returnString;
		ProcessBuilder processBuilder = new ProcessBuilder(currentGit, "stash", "pop");
		returnString = runProcess(processBuilder, false);

		return returnString;
	}

	private String getCommandString(ProcessBuilder pb) {
		List<String> commandsList = pb.command();
		StringBuilder commandsBuilder = new StringBuilder();
		int index = 0;
		for (String command : commandsList) {
			/* if there is a space in a command piece it must be surrounded by " */
			if (command.contains(" ")) {
				commandsBuilder.append("\"");
			}
			commandsBuilder.append(command);
			if (command.contains(" ")) {
				commandsBuilder.append("\"");
			}

			if (index != commandsList.size() - 1) {
				commandsBuilder.append(" ");
			}
			index++;
		}
		return commandsBuilder.toString();
	}

	private String runProcess(ProcessBuilder processBuilder, boolean isFileNeeded) {
		log.info(MARKER, "command to be run: {}", getCommandString(processBuilder));
		String procOutput = "";
		processBuilder.redirectErrorStream(true);
		if (isFileNeeded) {
			File procLog = new File("gitLog.log");
			processBuilder.redirectOutput(ProcessBuilder.Redirect.to(procLog));
		}
		try {
			Process process = processBuilder.start();

			if (!isFileNeeded) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				procOutput = builder.toString();
			}

			process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			log.error(ERROR, "could not run process", e);
			procOutput = RegressionUtilities.GIT_NOT_FOUND;
		}

		log.info(MARKER, "Git Info: {}", procOutput);
		return procOutput;
	}

	public String getGitVersion() {
		return gitVersion;
	}

	public String getGitBranch() {
		return gitBranch;
	}

	public String getGitLog() {
		return gitLog;
	}

	public String getGitInfo(boolean isSlackMsg) {
		if (gitInfo == null || gitInfo.isEmpty()) {
			return RegressionUtilities.GIT_NOT_FOUND;
		}
		if (isSlackMsg) {
			int newLineLoc = gitInfo.indexOf('\n');
			if (newLineLoc == -1) {
				return gitInfo;
			} else {
				return gitInfo.substring(0, gitInfo.indexOf('\n')).trim();
			}
		}
		return gitInfo;
	}

	public String getLastCommitDate() {
		String date = RegressionUtilities.GIT_NOT_FOUND;

		String beginDate = "Date:";
		int start = gitInfo.indexOf(beginDate);
		if (start == -1) {
			return date;
		}
		int end = gitInfo.indexOf('\n', start);
		if (end == -1) {
			end = gitInfo.length();
		}

		return gitInfo.substring(start + beginDate.length(), end).trim();
	}
}
