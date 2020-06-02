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

/**
 * check GC log by sending zipped GC log to GCEASY API, report problem and show report link
 */
public class MemoryLeakValidator extends Validator {
	private MemoryLeakCheckConfig config;
	private int nodesNum;
	private boolean isValidated = false;
	private boolean isValid = false;
	private File experimentFolder;
	private static final String GC_LOG_FILES = "gc*.log";
	private static final String NODE_FOLDER_NAME = "node*";
	private static final String GC_API_KEY = "cb052084-d873-4027-bb8c-5e60d2ef5a67";
	static final String GCEASY_URL = "https://api.gceasy.io/analyzeGC";
	/**
	 * 	if isProblem in response is true, log an error
	 */
	private static final String IS_PROBLEM_FIELD = "isProblem";
	/**
	 * if isProblem in response is true, we should report problem content
	 */
	private static final String PROBLEM_FIELD = "problem";
	/**
	 * 	if API key is missing or invalid, response would contain this field
	 */
	private static final String FAULT_FIELD = "fault";
	/**
	 * in this field is not null in response, log an info which contains a link of webReport
	 */
	private static final String WEB_REPORT_FIELD = "webReport";

	public static final String FAULT_FIELD_MSG = "fault message in response: ";
	public static final String PROBLEM_FIELD_MSG = "problem message in response: ";
	public static final String WEB_REPORT_FIELD_MSG = "GC webReport URL: ";

	public static final String ERROR_READ_STREAM = "got IOException when reading from " +
			"inputStream: ";
	public static final String ERROR_PARSE_RESPONSE = "got Exception when parsing response to Json";
	public static final String ERROR_STREAM = "got error message from GCEasy API: ";
	public static final String RESPONSE_CODE = "ResponseCode: ";
	public static final String RESPONSE_CODE_OK = "ResponseCode: OK";
	public static final String RESPONSE_EMPTY = "MemoryLeakValidator received empty response";

	public MemoryLeakValidator(final MemoryLeakCheckConfig config, final int nodesNum) {
		this.config = config;
		this.nodesNum = nodesNum;
	}

	@Override
	public void validate() {
		isValid = true;
		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}

	public void setFolder(String experimentFolder) {
		this.experimentFolder = new File(experimentFolder);
		if (!this.experimentFolder.exists()) {
			addError("experiment path did not exist");
		}
	}

	/**
	 * return
	 */
	void checkGCFile(final File logFile, final URL url) throws Exception {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "text");
		con.setRequestProperty("Content-Encoding", "zip");

		con.setDoOutput(true);
		IOUtils.copy(new FileInputStream(logFile), con.getOutputStream());

		int responseCode = con.getResponseCode();
		showResponseCode(responseCode);

		String response = readFromStream(con.getInputStream());
		checkResponse(response);

		String errMsg = readFromStream(con.getErrorStream());
		if (errMsg != null) {
			addWarning(ERROR_STREAM + errMsg);
		}
		System.out.println("response:" + response);
	}

	/**
	 * build URL for sending GC log to GCEasy API
	 *
	 * @return
	 * @throws MalformedURLException
	 */
	URL buildURL() throws MalformedURLException {
		return new URL(GCEASY_URL + "?apiKey=" + GC_API_KEY);
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
	 *
	 * @param response
	 * @return return false when `isProblem` is true; otherwise return true;
	 */
	boolean checkResponse(final String response) {
		if (response == null || response.isBlank()) {
			addWarning(RESPONSE_EMPTY);
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode jsonNode = mapper.readTree(response);
			// if there is an `fault` field, it means API key is missing or invalid
			if (jsonNode.has(FAULT_FIELD)) {
				addWarning(FAULT_FIELD_MSG + jsonNode.get(FAULT_FIELD));
			}
			// if `isProblem` is true, log an error with problem content
			if (jsonNode.has(IS_PROBLEM_FIELD)) {
				boolean isProblem = jsonNode.get(IS_PROBLEM_FIELD).booleanValue();
				if (isProblem) {
					addError(PROBLEM_FIELD_MSG + jsonNode.get(PROBLEM_FIELD));
				}
			}
			// if there is an `webReport` field, show the link
			if (jsonNode.has(WEB_REPORT_FIELD)) {
				addInfo(WEB_REPORT_FIELD_MSG + jsonNode.get(WEB_REPORT_FIELD));
			}
		} catch (JsonProcessingException ex) {
			addWarning(ERROR_PARSE_RESPONSE + ex.getMessage());
		}
		return true;
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
