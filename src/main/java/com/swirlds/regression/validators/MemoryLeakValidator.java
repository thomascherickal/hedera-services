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
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * check GC log by sending zipped GC log to GCEASY API, report problem and show report link
 */
public class MemoryLeakValidator extends Validator {
	/**
	 * key: nodeId, value: GC log zip file
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
	private static final String GC_API_KEY = "cb052084-d873-4027-bb8c-5e60d2ef5a67";
	/**
	 * GCEASY URL which we send GC logs for getting analyse report
	 */
	static final String GCEASY_URL = "https://api.gceasy.io/analyzeGC";
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
	public static final String ERROR_PARSE_RESPONSE = "got Exception when parsing response to Json";
	public static final String ERROR_STREAM = "got error message from GCEasy API: ";
	public static final String RESPONSE_CODE = "ResponseCode: ";
	public static final String RESPONSE_CODE_OK = "ResponseCode: OK";
	public static final String RESPONSE_EMPTY = "MemoryLeakValidator received empty response";

	public static final String GC_LOG_FILE_MISS = "node%d's GC log file is missing";
	public static final String CHECK_GC_LOG = "checking GC log file for node%d";

	public MemoryLeakValidator(final Map<Integer, File> gcFilesMap) {
		this.url = buildURL();
		this.gcFilesMap = gcFilesMap;
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
			addWarning("URL is null");
			return;
		}
		// get gcLog.zip for this node
		// add warn if gc*.log is missing;
		gcFilesMap.forEach((id, file) -> {
			if (!file.exists()) {
				addWarning(String.format(GC_LOG_FILE_MISS, id));
			} else {
				addInfo(String.format(CHECK_GC_LOG, id));
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
		try (FileInputStream fileInputStream = new FileInputStream(zipLogFile)) {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "text");
			con.setRequestProperty("Content-Encoding", "zip");

			con.setDoOutput(true);
			IOUtils.copy(fileInputStream, con.getOutputStream());

			int responseCode = con.getResponseCode();
			showResponseCode(responseCode);

			String response = readFromStream(con.getInputStream());
			checkResponse(response, nodeId);

			String errMsg = readFromStream(con.getErrorStream());
			if (errMsg != null) {
				addWarning(ERROR_STREAM + errMsg);
			}
			con.disconnect();
		} catch (IOException ex) {
			addWarning(ERROR_CONN_API + ex.getMessage());
		}
	}

	/**
	 * build URL for sending GC log to GCEasy API
	 */
	URL buildURL() {
		try {
			return new URL(GCEASY_URL + "?apiKey=" + GC_API_KEY);
		} catch (MalformedURLException ex) {
			addWarning("Got MalformedURLException when building URL");
		}
		return null;
	}

	/**
	 * read content from inputStreamE
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
		if (responseCode == HttpURLConnection.HTTP_OK) {
			addInfo(RESPONSE_CODE_OK);
		} else if (responseCode / 100 == 2) {
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
}
