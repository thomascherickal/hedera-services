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

package com.swirlds.regression;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SharePointUploader {

	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private static final String GRANT_TYPE = "";
	private static final String CLIENT_ID = "atfnightly@hedera.com";
	private static final String CLIENT_SECRET = "fvi42OKhJrYCcXgS";
	private static final String BASE_SITE = "hederatest.sharepoint.com";
	private static final int BASE_PORT = 443;
	private static final String BASE_SCHEME = "https";
	private static final String BASE_SITE_PATH = BASE_SITE + "/sites/Engineering";
	private static final String ATF_FOLDER = "Shared%20Documents/General/ATF%20Nightly%20Results";
	private static final String OAUTH_URL = "";
	private static final String URL_PARAMETER = "";
	private static final String OAUTH_FAIL_MESSAGE = "SharePoint refused Oauth2 token";
	private static final String UPLOAD_FOLDER_URL = "";
	private static final String UPLOAD_SUCCESS_MESSAGE = "Successfully uploaded file:%s to SharePoint";
	private static final String UPLOAD_FAIL_MESSAGE = " Failed to upload file:%s to SharePoint";

	public static void main(String[] args) {
		String domain = "hederatest";
		SharePointManager splm = new SharePointManager();
		splm.login();
		SharePointManager spm = new SharePointManager();
		spm.login();
		try (Stream<Path> walkerPath = Files.walk(Paths.get("results\\20190318-180647-663-restart-validation" +
				"\\restart\\"))) {
			List<String> foundFiles = walkerPath.filter(Files::isRegularFile).map(x -> x.toString()).collect(
					Collectors.toList());
			for (String file : foundFiles) {
				File currentFile = new File(file);
				String path = "AAA-SharepointUploadTest" + file.substring(file.indexOf("results/", 1) + 8);
				spm.uploadFile(path, currentFile);
			}
		} catch (IOException e) {
			log.error(ERROR, "unable to walk experiments results", e);
		}
	}

	private static String prettyFormatJson(String jsonString) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Object json = mapper.readValue(jsonString, Object.class);

			String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
			return indented;
		} catch (IOException e) {
			log.error(ERROR, "Could not format JSON for printing", e);
		}
		return null;
	}
}
