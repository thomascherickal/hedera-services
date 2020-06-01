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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * check GC log and show report
 */
public class MemoryLeakValidator extends Validator {
	private boolean isValidated = false;
	private boolean isValid = false;
	private File experimentFolder;
	private final String GC_LOG_FILES = "gc*.log";
	private final String NODE_FOLDER_NAME = "node*";
	private final String GC_API_KEY = "cb052084-d873-4027-bb8c-5e60d2ef5a67";
	private final String GCEASY_URL = "https://api.gceasy.io/analyzeGC";

	public void setFolder(String experimentFolder) {
		this.experimentFolder = new File(experimentFolder);
		if (!this.experimentFolder.exists()) {
			addError("experiment path did not exist");
		}
	}

	/**
	 *
	 * return
	 */
	void checkForEachNode(File logFile) throws Exception {
		URL obj = new URL(GCEASY_URL + "?apiKey=" + GC_API_KEY);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type","text");
		con.setRequestProperty("Content-Encoding","zip");

		con.setDoOutput(true);
		IOUtils.copy(new FileInputStream(logFile), con.getOutputStream());

		int responseCode = con.getResponseCode();
		System.out.println("Response Code : " + responseCode);

		BufferedReader iny = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = iny.readLine()) != null) {
			response.append(output);
		}
		iny.close();

		System.out.println(response);
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
}
