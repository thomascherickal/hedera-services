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
	private final String GC_LOG_FILES = "gc*.log";
	private final String NODE_FOLDER_NAME = "node*";
	private final String GC_API_KEY = "cb052084-d873-4027-bb8c-5e60d2ef5a67";
	static final String GCEASY_URL = "https://api.gceasy.io/analyzeGC";

	public static final String ERROR_READ_STREAM = "MemoryLeakValidator got IOException when reading from inputStream: ";
	public static final String ERROR_STREAM = "MemoryLeakValidator got error message from GCEasy API: ";
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
		if (response == null) {
			addWarning(RESPONSE_EMPTY);
		}
		String errMsg = readFromStream(con.getErrorStream());
		if (errMsg != null) {
			addWarning(ERROR_STREAM + errMsg);
		}
		System.out.println("response:" + response);
	}

	/**
	 * build URL for sending GC log to GCEasy API
	 * @return
	 * @throws MalformedURLException
	 */
	URL buildURL() throws MalformedURLException {
		return new URL(GCEASY_URL + "?apiKey=" + GC_API_KEY);
	}

	/**
	 * read content from inputStreamE
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
			addError(ERROR_READ_STREAM + ex.getMessage());
		}
		return response.toString();
	}

	/**
	 * show responseCode in message
	 * @param responseCode
	 */
	void showResponseCode(final int responseCode) {
		if (responseCode == HttpURLConnection.HTTP_OK) {
			addInfo("ResponseCode: OK");
		} else if (responseCode / 100 == 2) {
			// 1xx: Informational
			// 2xx: Success
			addInfo("ResponseCode: " + responseCode);
		} else {
			// 3xx: Redirection
			// 4xx: Client Error
			// 5xx: Server Error
			addWarning("ResponseCode: " + responseCode);
		}
	}
}
