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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.jsonConfigs.JvmOptionParametersConfig;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.swirlds.common.logging.PlatformLogMessages.PTD_FINISH;
import static com.swirlds.common.logging.PlatformLogMessages.PTD_SUCCESS;

public class RegressionUtilities {

	public static final String PLATFORM_TESTING_APP = "PlatformTestingApp.jar";
	public static final String WRITE_FILE_DIRECTORY = "tmp/";
	public static final String PUBLIC_IP_ADDRESS_FILE = WRITE_FILE_DIRECTORY + "publicAddresses.txt";
	public static final String PRIVATE_IP_ADDRESS_FILE = WRITE_FILE_DIRECTORY + "privateAddresses.txt";

	public static final String SDK_DIR = "../sdk/";
	public static final String PTD_CONFIG_DIR = "../platform-apps/tests/PlatformTestingApp/src/main/resources/";
	public static final String SETTINGS_FILE = "settings.txt";
	public static final String DEFAULT_SETTINGS_DIR = "../sdk/";
	public static final String CONFIG_FILE = "config.txt";
	public static final Charset STANDARD_CHARSET = Charset.forName("UTF-8");
	public static final String RESULTS_FOLDER = "results";
	public static final String TAR_NAME = "remoteExperiment.tar.gz";
	public static final ArrayList<String> DIRECTORIES_TO_INCLUDE = new ArrayList<>(Arrays.asList("data"));
	public static final String JVM_OPTIONS_DEFAULT = "-Xmx100g -Xms8g -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
			"-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=32g";
	public static final String JVM_OPTIONS_PARAMETER_STRING = "-Xmx%dg -Xms%dg -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
			"-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=%dg";
	public static final String GET_TOTAL_MB_MEMORY_ON_NODE = "vmstat -s -SM | head -n1 | awk '{ printf  \"%10s\\n\", $1 }' | sed 's/^[[:space:]]*//g'";
	/* this section is for dynamic allocation of huge pages and memory of instances */
	public static final String STOP_POSTGRESQL_SERVICE = "sudo systemctl stop postgresql;";
	public static final String START_POSTGRESQL_SERVICE = "sudo systemctl start postgresql";
	public static final String CHANGE_HUGEPAGE_NUMBER = "sudo sysctl -w vm.nr_hugepages=%d";
	public static final String CHANGE_POSTGRES_MEMORY_ALLOCATION = "sudo sed -i " +
			"'s/shared_buffers = [0-9]*MB/shared_buffers = %dMB/g\n" +
			"s/temp_buffers = [0-9]*MB/temp_buffers = %dMB/g\n" +
			"s/max_prepared_transactions = [0-9]*/max_prepared_transactions = %d/g\n" +
			"s/work_mem = [0-9]*MB/work_mem = %dMB/g\n" +
			"s/maintenance_work_mem = [0-9]*MB/maintenance_work_mem = %dMB/g\n" +
			"s/autovacuum_work_mem = [0-9]*MB/autovacuum_work_mem = %dMB/g' " +
			"/etc/postgresql/10/main/postgresql.conf";

	/* the huge pages on ubuntu are 2MB this information is needed for calculation to the number of huge pages. */
	static final int UBUNTU_HUGE_PAGE_SIZE_DIVISOR = 2048;
	static final String POSTGRES_DEFAULT_WORK_MEM = "256MB";
	static final int POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS = 100;
	static final String POSTGRES_DEFAULT_TEMP_BUFFERS = "64MB";
	static final String OS_RESERVE_MEMORY = "2GB";
	public static final String POSTGRES_DEFAULT_SHARED_BUFFERS = "512MB";

	public static final int SHA1_DIVISOR = 25;
	public static final long JAVA_PROC_CHECK_INTERVAL = 2 * 60 * 1000; // min * sec * millis
	public static final int MB = 1024 * 1024;
	public static final String CHECK_JAVA_PROC_COMMAND = "pgrep -fl java";
	public static final String KILL_JAVA_PROC_COMMAND = "sudo pkill -f java";

	public static final String KILL_REGRESSION_PROC_COMMAND = "sudo pkill -f regression";
	public static final String KILL_NET_COMMAND = "sudo -n iptables -A INPUT -p tcp --dport 10000:65535 -j DROP; sudo " +
			"-n iptables -A OUTPUT -p tcp --sport 10000:65535 -j DROP;";
	public static final String REVIVE_NET_COMMAND = "sudo -n iptables -D INPUT -p tcp --dport 10000:65535 -j DROP; sudo" +
			" -n iptables -D OUTPUT -p tcp --sport 10000:65535 -j DROP;";
	public static final String CHECK_FOR_PTD_TEST_MESSAGE = "egrep \"TEST SUCCESS|TEST FAIL|TRANSACTIONS FINISHED|TEST " +
			"ERROR\" remoteExperiment/swirlds.log";
	public static final String REMOTE_SWIRLDS_LOG = "remoteExperiment/swirlds.log";

	public static final String RESET_NODE = "sudo rm -rf remoteExperiment";
	public static final String EMPTY_HASH = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
	public static final long CLOUD_WAIT_MILLIS = 30000;
	public static final long POSTGRES_WAIT_MILLIS = 30000;
	public static final int MILLIS = 1000;
	public static final int MS_TO_NS = 1000_000;
	public static final int WAIT_NODES_READY_TIMES = 9;

	public static final ArrayList<String> PTD_LOG_FINISHED_MESSAGES = new ArrayList<>(
			Arrays.asList(PTD_SUCCESS, PTD_FINISH));
	public static final String DROP_DATABASE_BEFORE_NEXT_TEST = "sudo -i -u postgres psql -c \"drop extension crypto;" +
			" drop database fcfs; create database fcfs with owner = swirlds;\"";
	public static final String DROP_DATABASE_EXTENSION_BEFORE_NEXT_TEST = "sudo -i -u postgres psql -c \"drop " +
			"extension crypto;\"";
	public static final String DROP_DATABASE_FCFS_TABLE_BEFORE_NEXT_TEST = "sudo -i -u postgres psql -c \" drop " +
			"database fcfs;\"";
	public static final ArrayList<String> PTD_LOG_SUCCESS_OR_FAIL_MESSAGES = new ArrayList<>(
			Arrays.asList(PTD_SUCCESS, PTD_FINISH));

	public static final int AMAZON_INSTANCE_WAIT_TIME_SECONDS = 3;
	static final String DROP_DATABASE_FCFS_EXPECTED_RESPONCE = "DROP DATABASE";
	static final String DROP_DATABASE_FCFS_KNOWN_RESPONCE = "ERROR:  database \"fcfs\" is being accessed by other " +
			"users";
	public static final String CREATE_DATABASE_FCFS_TABE_BEFORE_NEXT_TEST = "sudo -i -u postgres psql -c \"create " +
			"database fcfs with owner = swirlds;\"";
	static final String CREATE_DATABASE_FCFS_EXPECTED_RESPONCE = "CREATE DATABASE";

	public static final String GIT_NOT_FOUND = "Git repo was not found in base directory.\n";

	public static final int EXCEPTIONS_SIZE = 1000;
	public static final int SSH_TEST_CMD_AFTER_SEC = 60;
	public static final String MVN_ERROR_FLAG = "[ERROR]";

	public static final String INSIGHT_CMD = "%s %s -p -d%s -g -cPlatformTesting -N";
    public static final Object INSIGHT_SCRIPT_LOCATION = "./insight.py";

	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");


	static final String TEST_CONFIG = "configs/testRestartCfg.json";
	static final String REGRESSION_CONFIG = "configs/AwsRegressionCfg_Freeze.json";
	static final String REMOTE_EXPERIMENT_LOCATION = "remoteExperiment/";
	static final String REMOTE_STATE_LOCATION = REMOTE_EXPERIMENT_LOCATION + "data/saved/";
	static final String DB_BACKUP_FILENAME = "PostgresBackup.tar.gz";

	static final String REG_SLACK_CHANNEL = "regression";
	static final String REG_GIT_BRANCH = "develop";
	static final boolean CHECK_BRANCH_CHANNEL = true;
	static final String REG_GIT_USER_EMAIL = "swirlds-test@swirlds.org";
	static final boolean CHECK_USER_EMAIL_CHANNEL = true;

	static final String SWIRLDS_NAME = "123";
	static final String SAVED_STATE_LOCATION = REMOTE_STATE_LOCATION + "*/*/" + SWIRLDS_NAME;
	public static final String EVENT_MATCH_MSG = "Recovered file match original ones";

	static final boolean USE_STAKES_IN_CONFIG = true;
	// total stakes are the same as the number of the number of tinybars in existence
	// (50 billion)*(times 100 million)
	static final long TOTAL_STAKES = 50L * 1_000_000_000L * 100L * 1_000_000L;

	public static final String TEST_TIME_EXCEEDED_MSG = "Test time exceeded; moving to next phase";

	static final String[] NIGHTLY_REGRESSION_SERVER_LIST = {
			"i-03c90b3fdeed8edd7",
			"i-050d3f864b99b796f",
			"i-0611e2febc6a73d5a",
			"i-0cc227bff247a8a09" };
	static final String NIGHTLY_REGRESSION_KICKOFF_SERVER = "172.31.9.236";

	private static final String OS = System.getProperty("os.name").toLowerCase();

	static TestConfig importExperimentConfig() {
		return importExperimentConfig(TEST_CONFIG);
	}

	protected static TestConfig importExperimentConfig(String testConfigFileLocation) {
		return importExperimentConfig(Paths.get(testConfigFileLocation));
	}

	protected static TestConfig importExperimentConfig(URI testConfigFileLocation) {
		return importExperimentConfig(Paths.get(testConfigFileLocation));
	}

	protected static TestConfig importExperimentConfig(Path testConfigFileLocation) {
		try {
			log.info(MARKER, "Importing experiment file: {}", testConfigFileLocation);
			byte[] jsonData = Files.readAllBytes(testConfigFileLocation);
			ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);

			log.info(MARKER, "Parsing Test JSON......");
			TestConfig testConfig = objectMapper.readValue(jsonData, TestConfig.class);
			log.info(MARKER, "Parsed Test JSON......");
			return testConfig;

		} catch (JsonParseException e) {
			log.error(ERROR, "could not parse the json");
			e.printStackTrace();
		} catch (JsonMappingException e) {
			log.error(ERROR, "Couldn't map the JSON");
			e.printStackTrace();
		} catch (IOException e) {
			log.error(ERROR, "There was an issue with the json file.");
			e.printStackTrace();
		}
		return null;
	}

	protected static RegressionConfig importRegressionConfig() {
		return importRegressionConfig(REGRESSION_CONFIG);
	}

	protected static RegressionConfig importRegressionConfig(String regressionConfigFileLocation) {
		try {
			log.info(MARKER, "Importing regression file: {}", regressionConfigFileLocation);
			byte[] jsonData = Files.readAllBytes(Paths.get(regressionConfigFileLocation));
			ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);

			log.info(MARKER, "Parsing JSON......");
			RegressionConfig regressionConfig = objectMapper.readValue(jsonData, RegressionConfig.class);

			return regressionConfig;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static RegressionConfig importRegressionConfig(URI regressionConfigFileLocation) {
		try {
			log.info(MARKER, "Importing regression file: {}", regressionConfigFileLocation.toString());
			byte[] jsonData = Files.readAllBytes(Paths.get(regressionConfigFileLocation));
			ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);

			log.info(MARKER, "Parsing JSON......");
			RegressionConfig regressionConfig = objectMapper.readValue(jsonData, RegressionConfig.class);

			return regressionConfig;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static Collection<File> getSDKFilesToUpload(File keyFile, File log4jFile,
			ArrayList<File> configSpecifiedFiles) {
		Collection<File> returnIterator = new ArrayList<>();
		returnIterator.add(new File(SDK_DIR + "data/apps/"));
		returnIterator.add(new File(SDK_DIR + "data/backup/"));
		returnIterator.add(new File(SDK_DIR + "data/keys/"));
		returnIterator.add(new File(SDK_DIR + "data/lib/"));
		returnIterator.add(new File(SDK_DIR + "data/repos/"));
		returnIterator.add(new File(SDK_DIR + "kernels/"));
		returnIterator.add(new File(SDK_DIR + "swirlds.jar"));
		returnIterator.add(new File(SDK_DIR + "testing/badgerize.sh"));
		returnIterator.add(new File(PRIVATE_IP_ADDRESS_FILE));
		returnIterator.add(new File(PUBLIC_IP_ADDRESS_FILE));
		returnIterator.add(keyFile);
		returnIterator.add(new File(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.CONFIG_FILE));
		returnIterator.add(new File(RegressionUtilities.WRITE_FILE_DIRECTORY + RegressionUtilities.SETTINGS_FILE));
		returnIterator.add(log4jFile);
		if (configSpecifiedFiles != null) {
			returnIterator.addAll(configSpecifiedFiles);
		}

		return returnIterator;
	}

	protected static ArrayList<String> getRsyncListToUpload(File keyFile, File log4jFile,
			ArrayList<File> configSpecifiedFiles) {
		ArrayList<String> returnIterator = new ArrayList<>();

		returnIterator.add("data/");
		returnIterator.add("data/apps/");
		returnIterator.add("data/apps/**");
		returnIterator.add("data/backup/");
		returnIterator.add("data/backup/**");
		returnIterator.add("data/keys/");
		returnIterator.add("data/keys/**");
		returnIterator.add("data/lib/");
		returnIterator.add("data/lib/**");
		returnIterator.add("data/repos/");
		returnIterator.add("data/repos/**");
		returnIterator.add("kernels/");
		returnIterator.add("kernels/**");
		returnIterator.add("swirlds.jar");
		returnIterator.add("privateAddresses.txt");
		returnIterator.add("publicAddresses.txt");
		returnIterator.add("badgerize.sh");
		returnIterator.add(keyFile.getName());
		returnIterator.add(RegressionUtilities.CONFIG_FILE);
		returnIterator.add(RegressionUtilities.SETTINGS_FILE);
		returnIterator.add(log4jFile.getName());

		if (configSpecifiedFiles != null) {
			for (File file : configSpecifiedFiles) {
				returnIterator.add(file.getName());
			}
		}
		return returnIterator;
	}

	protected static ArrayList<String> getSDKFilesToDownload(ArrayList<String> configSpecifiedFiles) {
		ArrayList<String> returnIterator = new ArrayList<>();

		returnIterator.add("*.csv");
		returnIterator.add("*.log");
		returnIterator.add("*.xml");
		returnIterator.add("*.txt");
		returnIterator.add("*.json");
		returnIterator.add("badger_*");
		//returnIterator.add("stream_*");
		returnIterator.add("postgres_reports"); // badgerized web summaries
		returnIterator.add("latest_postgres*"); // raw log files(s)

		if (configSpecifiedFiles != null) {
			returnIterator.addAll(configSpecifiedFiles);
		}

		return returnIterator;
	}

	protected static String getExperimentTimeFormattedString(ZonedDateTime timeToFormat) {
		return timeToFormat.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
	}

	protected static String getResultsFolder(ZonedDateTime timeToFormat, String testName) {
		return getExperimentTimeFormattedString(timeToFormat) + "-" + testName;
	}

	public static String getRemoteSavedStatePath(String mainClass, long nodeId, String swirldName, long round) {
		return String.format("%s%s/%d/%s/%d/",
				REMOTE_STATE_LOCATION, mainClass, nodeId, swirldName, round);
	}

	public static List<String> getFilesInDir(String dirPath, boolean returnPaths) {
		File dir = new File(dirPath);
		if (!dir.isDirectory()) {
			return null;
		}
		return Arrays.stream(dir.listFiles()).filter(File::isFile)
				.map(returnPaths ? File::getAbsolutePath : File::getName).collect(Collectors.toList());
    }

	/**
	 * Is the current OS windows?
	 * @return return true if windows, false if not
	 */
	public static boolean isWindows() {
        return OS.indexOf("win") >= 0;
    }

	/**
	 * Get the executable to call on the node based on os type.
	 * @return - python3 for unix flavors, python for windows flavors
	 */

	public static String getPythonExecutable() {
        String pythonExecutable = "python3";
        if (isWindows()) {
            pythonExecutable = "python";
        }
        return pythonExecutable;
    }

	/**
	 * Build the java parameter string to call the java function with based on the memory variables passed in
	 * @param maxMemory - maximum memory allowed for the JVM
	 * @param minMemory - minimum memory allowed for the JVM
	 * @param maxDirectMemory
	 * @return - formatted string with list of parameters and their values to use when launching the JVM on a remote node.
	 *   EX. "-Xmx%dg -Xms%dg -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
	 * 				"-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=%dg
	 */
    public static String buildParameterString(int maxMemory, int minMemory, int maxDirectMemory) {

		/* parameter string: "-Xmx%dg -Xms%dg -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
				"-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=%dg"
		 */
		if(maxMemory <= 0 || minMemory <= 0 || maxDirectMemory <= 0) {
			return JVM_OPTIONS_DEFAULT;
		} else {
			return String.format(JVM_OPTIONS_PARAMETER_STRING, maxMemory, minMemory, maxDirectMemory);
		}
	}

	/**
	 *
	 * @param jvmOptionParametersConfig - Object with JVM option parameters loaded from the experiment JSON file
	 * @return - formatted string with list of parameters and their values to use when launching the JVM on a remote node.
	 * 	  EX. "-Xmx%dg -Xms%dg -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
	 * 	 			"-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=%dg
	 */
	public static String buildParameterString(JvmOptionParametersConfig jvmOptionParametersConfig) {
		if(jvmOptionParametersConfig == null) { return JVM_OPTIONS_DEFAULT; }
		int maxMemory = jvmOptionParametersConfig.getMaxMemory();
		int minMemory = jvmOptionParametersConfig.getMinMemory();
		int maxDirectMemory = jvmOptionParametersConfig.getMaxDirectMemory();
		if(maxMemory <= 0 || minMemory <= 0 || maxDirectMemory <= 0) {
			return JVM_OPTIONS_DEFAULT;
		}
		else {
			return buildParameterString(maxMemory, minMemory, maxDirectMemory);
		}
	}
}