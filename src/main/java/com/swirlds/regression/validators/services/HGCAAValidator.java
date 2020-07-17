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

package com.swirlds.regression.validators.services;

import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogEntry;
import com.swirlds.regression.validators.NodeData;
import com.swirlds.regression.validators.Validator;

import java.io.IOException;
import java.util.List;

/**
 * Validate hgcaa.log and queries.log in server node when running services-regression
 */

public class HGCAAValidator extends Validator {
	private List<NodeData> hederaNodeData;
	private boolean isValid;
	private boolean isValidated;
	private int errorCount = 0;

	/** string to indicate jar file has been updated successfully */
	private static final String NEW_JAR_MESSAGE = "new version jar";

	/** when testing freeze handler or update feature, hedera-services will restart after freeze */
	private boolean checkHGCAppRestart = false;

	public HGCAAValidator(List<NodeData> hederaNodeData) {
		this.hederaNodeData = hederaNodeData;
		isValid = true;
		isValidated = false;
	}

	@Override
	public void validate() throws IOException {
		int nodeNum = hederaNodeData.size();

		for (int i = 0; i < nodeNum; i++) {
			LogReader<PlatformLogEntry> hgcaaLogReader = hederaNodeData.get(i).getLogReader();

			PlatformLogEntry logLine = hgcaaLogReader.nextEntry();

			boolean foundNewJarVersionMarker = false;
			while (logLine != null) {
				if (logLine.isException()) {
					isValid = false;
					errorCount++;
					//addError(logLine.getLogEntry());
				} else if (hasNewJarMessage(logLine)) {
					addInfo("Node " + i + " finished jar update");
					foundNewJarVersionMarker = true;
				}

				logLine = hgcaaLogReader.nextEntry();
			}

			if (checkHGCAppRestart && !foundNewJarVersionMarker) {
				isValid = false;
				addError("Node " + i + " did not finish jar update");
			}

			if (errorCount > 0) {
				addError("Node " + i + " has " + errorCount + " errors");
			}
		}

		isValidated = true;
	}

	private boolean hasNewJarMessage(PlatformLogEntry logLine) {
		return checkHGCAppRestart && logLine.getLogEntry().contains(NEW_JAR_MESSAGE);
	}

	@Override
	public boolean isValid() {
		return isValid && isValidated;
	}

	public boolean isCheckHGCAppRestart() {
		return checkHGCAppRestart;
	}

	public void setCheckHGCAppRestart(boolean checkHGCAppRestart) {
		this.checkHGCAppRestart = checkHGCAppRestart;
	}
}
