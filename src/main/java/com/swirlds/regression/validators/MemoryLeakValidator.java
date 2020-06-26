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

package com.swirlds.regression.validators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.jsonConfigs.MemoryLeakCheckConfig;
import com.swirlds.regression.utils.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static com.swirlds.regression.RegressionUtilities.GC_LOG_ZIP_FILE;
import static org.apache.logging.log4j.core.util.Loader.getClassLoader;

/**
 * check GC log by sending zipped GC log to GCEASY API, report problem and show report link
 */
public class MemoryLeakValidator extends Validator {
	private static final Logger log = LogManager.getLogger(MemoryLeakValidator.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	/**
	 * result folders of all nodes
	 */
	private String[] resultFolders;
	/**
	 * config which specifies which nodes we want to check GC logs
	 */
	private MemoryLeakCheckConfig memoryLeakCheckConfig;
	/**
	 * key: nodeId, value: GC log .gz file
	 */
	private Map<Integer, File> gcFilesMap;
	/**
	 * the URL for sending GC log to GCEasy API
	 */
	private URL url;
	/**
	 * whether GC log Files has been validated or not
	 */
	private boolean isValidated = false;
	/**
	 * isValid would be false when a node's GCLog has `problem` in the report
	 */
	private boolean isValid = true;
	/**
	 * API KEY
	 */
	private static String GC_API_KEY;
	/**
	 * path of the file from which we read GC API KEY
	 */
	static final String GC_API_KEY_PATH = "GC_API_KEY";
	/**
	 * GCEASY URL which we send GC logs for getting analyse report
	 */
	static final String GCEASY_URL = "https://api.gceasy.io/analyzeGC";
	/**
	 * the String to parse as a URL for sending GC log to GCEasy API
	 */
	static String URL_SPEC;

	/**
	 * `isProblem` field in response
	 * if isProblem in response is true, log an error
	 */
	private static final String IS_PROBLEM_FIELD = "isProblem";
	/**
	 * `problem` field in response
	 * if isProblem in response is true, we should report problem content
	 */
	private static final String PROBLEM_FIELD = "problem";
	/**
	 * `fault` field in response
	 * if API key is missing or invalid, response would contain this field
	 */
	private static final String FAULT_FIELD = "fault";
	/**
	 * `webReport` field in response
	 * in this field is not null in response, log an info which contains a link of webReport
	 */
	private static final String WEB_REPORT_FIELD = "webReport";

	public static final String FAULT_FIELD_MSG = "fault message in response: ";
	public static final String PROBLEM_FIELD_MSG = "has problem message in response: ";
	public static final String WEB_REPORT_FIELD_MSG = "GC webReport URL: ";

	public static final String ERROR_READ_STREAM = "got IOException when reading from " +
			"inputStream: ";
	public static final String ERROR_CONN_API = "got IOException when communicating with GCEasy API: ";
	public static final String ERROR_PARSE_RESPONSE = "Fail to get GCLog analysis report, because got Exception when " +
			"parsing response to Json";
	public static final String ERROR_STREAM = "got error message from GCEasy API: ";
	public static final String RESPONSE_CODE = "ResponseCode: ";
	public static final String RESPONSE_EMPTY = "MemoryLeakValidator received empty response";

	public static final String GC_LOG_ZIP_FILE_MISS = "node%d's GC log zip file is missing";

	public static final String GC_LOG_FILE_MISS = "node%d's GC log file is missing";
	public static final String CHECK_GC_LOG = "checking GC log file for node%d";
	public static final String GC_LOG_REPORT_NODE = "GCLog analysis report of node{}: \n {}";

	/**
	 * `gc start` denotes an GC event in GC logs
	 */
	private static final String GC_START = "gc,start";

	public static final String GC_LOG_FILES_REGEX = "gc(.*).log";

	public MemoryLeakValidator(final MemoryLeakCheckConfig memoryLeakCheckConfig,
			final String[] resultFolders) {
		this.memoryLeakCheckConfig = memoryLeakCheckConfig;
		this.resultFolders = resultFolders;
		GC_API_KEY = readGcApiKey();
		URL_SPEC = GCEASY_URL + "?apiKey=" + GC_API_KEY;
		this.url = buildURL();
		this.gcFilesMap = getGCLogZipsForNodes();
	}

	/**
	 * for unit test
	 */
	MemoryLeakValidator(final Map<Integer, File> gcFilesMap) {
		this.gcFilesMap = gcFilesMap;
		GC_API_KEY = readGcApiKey();
		URL_SPEC = GCEASY_URL + "?apiKey=" + GC_API_KEY;
		System.out.println(URL_SPEC);
		this.url = buildURL();
	}

	/**
	 * for unit test
	 */
	MemoryLeakValidator(final Map<Integer, File> gcFilesMap, final URL url) {
		this.gcFilesMap = gcFilesMap;
		this.url = url;
	}

	@Override
	public void validate() {
		if (url == null) {
			addWarning("Cannot validate GCLogs because failed to build URL: " + URL_SPEC);
			return;
		}
		// check gcLog.zip for this node
		// add warn if GC log zip file is missing;
		gcFilesMap.forEach((id, file) -> {
			if (file == null || !file.exists()) {
				addWarning(String.format(GC_LOG_ZIP_FILE_MISS, id));
			} else {
				addInfo(String.format(CHECK_GC_LOG, id));
				log.info(MARKER, String.format(CHECK_GC_LOG, id));
				checkGCFile(file, url, id);
			}
		});

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}

	/**
	 * send zipLogFile to GCEasy API and parse response
	 */
	void checkGCFile(final File zipLogFile, final URL url, final int nodeId) {
		HttpURLConnection conn = null;
		try {
			conn = buildConnection(url);
			try (FileInputStream fileInputStream = new FileInputStream(zipLogFile)) {
				IOUtils.copy(fileInputStream, conn.getOutputStream());

				int responseCode = conn.getResponseCode();
				showResponseCode(responseCode);

				String response = readFromStream(conn.getInputStream());
				log.info(MARKER, GC_LOG_REPORT_NODE, nodeId, response);
				checkResponse(response, nodeId);

				String errMsg = readFromStream(conn.getErrorStream());
				if (errMsg != null) {
					addWarning(ERROR_STREAM + errMsg);
				}
			}
		} catch (IOException ex) {
			addWarning(ERROR_CONN_API + ex.getMessage());
		} finally {
			if (conn == null) {
				conn.disconnect();
			}
		}
	}

	/**
	 * zip GC log files for given node
	 * if succeed, put the zip file into gcFilesMap
	 */
	void putZipGCToMap(final Map<Integer, File> gcFilesMap, final String folder, final File[] files,
			final int nodeIndex) {
		File zipFile = new File(folder.concat(GC_LOG_ZIP_FILE));
		try {
			FileUtils.zip(files, zipFile);
			gcFilesMap.put(nodeIndex, zipFile);
		} catch (IOException e) {
			log.error(ERROR, "Got exception while zipping files as {}:", zipFile.getName(), e);
			addError("node " + nodeIndex + "got IOException while zipping GC log files, so could not get analysis");
		}
	}

	/**
	 * for nodes we want to check GC logs:
	 * (1) if GC logs not exist, log a warning;
	 * (2) check whether the nodes' GC logs contains any GC Events;
	 * (3) if not, we don't need to submit it to GCeasy API;
	 * (4) if yes, we generate a .gz file and put it into a Map for submit to GCeasy API later
	 *
	 * @return
	 */
	private Map<Integer, File> getGCLogZipsForNodes() {
		final Map<Integer, File> gcFilesMap = new HashMap<>();

		final int totalNum = resultFolders.length;
		final int lastStakedNode = getLastStakedNode();
		for (int nodeIndex = 0; nodeIndex < totalNum; nodeIndex++) {
			// only check GC log for nodes specified in MemoryLeakCheckConfig
			if (memoryLeakCheckConfig.shouldCheck(nodeIndex, totalNum, lastStakedNode)) {
				String folder = resultFolders[nodeIndex];
				File[] files = FileUtils.getFilesMatchRegex(folder, GC_LOG_FILES_REGEX);
				if (files.length == 0) {
					addWarning(String.format(GC_LOG_FILE_MISS, nodeIndex));
				} else if (hasGCEvents(files, nodeIndex)) {
					// zip GC log files and put into
					putZipGCToMap(gcFilesMap, folder, files, nodeIndex);
				}
			}
		}
		return gcFilesMap;
	}

	/**
	 * Build a connection to the given URL
	 *
	 * @throws IOException
	 */
	HttpURLConnection buildConnection(final URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "text");
		conn.setRequestProperty("Content-Encoding", "zip");

		conn.setDoOutput(true);
		return conn;
	}

	/**
	 * build URL for sending GC log to GCEasy API
	 */
	URL buildURL() {
		try {
			return new URL(URL_SPEC);
		} catch (MalformedURLException ex) {
			addWarning("Got MalformedURLException when building URL: " + URL_SPEC);
		}
		return null;
	}

	/**
	 * read content from inputStream
	 *
	 * @param inputStream
	 * @return
	 */
	String readFromStream(final InputStream inputStream) {
		if (inputStream == null) {
			return null;
		}
		StringBuilder response = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream))) {
			String output;

			while ((output = bufferedReader.readLine()) != null) {
				response.append(output);
			}
		} catch (IOException ex) {
			addWarning(ERROR_READ_STREAM + ex.getMessage());
		}
		return response.toString();
	}

	/**
	 * check response, log an error when `isProblem` in response is true
	 */
	void checkResponse(final String response, final int nodeId) {
		if (response == null || response.isBlank()) {
			addWarning(RESPONSE_EMPTY);
			return;
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode jsonNode = mapper.readTree(response);
			// if there is an `fault` field, it means API key is missing or invalid
			if (jsonNode.has(FAULT_FIELD)) {
				addWarning(FAULT_FIELD_MSG + jsonNode.get(FAULT_FIELD));
			}

			if (jsonNode.has(IS_PROBLEM_FIELD)) {
				// if `isProblem` is true, log an error with problem content
				if (jsonNode.get(IS_PROBLEM_FIELD).booleanValue()) {
					addError(String.format("node%d %s %s", nodeId,
							PROBLEM_FIELD_MSG, jsonNode.get(PROBLEM_FIELD)));
					isValid = false;
				}
			}
			// if there is an `webReport` field, show the link
			if (jsonNode.has(WEB_REPORT_FIELD)) {
				addInfo(String.format("node%d's %s %s", nodeId,
						WEB_REPORT_FIELD_MSG, jsonNode.get(WEB_REPORT_FIELD)));
			}
		} catch (JsonProcessingException ex) {
			addWarning(ERROR_PARSE_RESPONSE + ex.getMessage());
		}
	}

	/**
	 * show responseCode in message
	 *
	 * @param responseCode
	 */
	void showResponseCode(final int responseCode) {
		if (responseCode < 300) {
			// 1xx: Informational
			// 2xx: Success
			addInfo(RESPONSE_CODE + responseCode);
		} else {
			// 3xx: Redirection
			// 4xx: Client Error
			// 5xx: Server Error
			addWarning(RESPONSE_CODE + responseCode);
		}
	}

	/**
	 * check for the given node, whether any GC log files contain GC Events
	 *
	 * @param files
	 * @param nodeIndex
	 * @return
	 */
	boolean hasGCEvents(final File[] files, final int nodeIndex) {
		for (File file : files) {
			if (hasGCEvents(file)) {
				return true;
			}
		}
		addInfo(String.format(
				"node%d's GC logs didn't report any GC Events, so we won't submit it for analysis. This indicates " +
						"there" +
						" is no Memory issue in this test.",
				nodeIndex));
		return false;
	}

	/**
	 * check GC log file, if there are any GC Events such as `gc start`.
	 * If not, we would not sent it to GCeasy API.
	 * Because GCeasy API would return fault message "GC Log format not recognized. Failed to parse GC Log" for GC logs
	 * which don't contain any GC Events
	 *
	 * @param file
	 * @return
	 */
	static boolean hasGCEvents(final File file) {
		if (file == null || !file.exists()) {
			return false;
		}

		String line;
		try (Scanner scanner = new Scanner(file)) {
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				if (line.contains(GC_START)) {
					return true;
				}
			}
		} catch (FileNotFoundException ex) {
			log.error(ERROR, "{} doesn't exist", file.getName());
		}
		return false;
	}

	/**
	 * get an array of GC Log files in the given folder
	 *
	 * @param folder
	 * @return
	 */
	static File[] getGCLogs(final String folder) {
		return FileUtils.getFilesMatchRegex(folder, GC_LOG_FILES_REGEX);
	}

	/**
	 * read GC API KEY from file
	 */
	String readGcApiKey() {
		try (InputStream inputStream = getClassLoader().getResourceAsStream(GC_API_KEY_PATH)) {
			return readFromStream(inputStream);
		} catch (IOException ex) {
			log.error("Fail to read GC API Key from: {} ", GC_API_KEY_PATH);
		}
		return "";
	}
}
