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

package com.swirlds.regression.validators;

import com.swirlds.common.PlatformLogMarker;
import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.OLD_EVENT_PARENT;

public class MigrationValidator extends NodeValidator {

	private static final String START_LOADING = "MigrationTestingApp started loading state from disk";

	private static final String END_LOADING = "MigrationTestingApp loaded state from disk successfully";

	private static final String START_PROCESSING = "MigrationTestingApp started handling";

	private static final String END_PROCESSING = "MigrationTestingApp finished handling";

	public MigrationValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	boolean isValidated = false;
	boolean isValid = true;

	@Override
	public void validate() throws IOException {
		final int nodeNum = nodeData.size();

		for (int i = 0; i < nodeNum - 1; i++) {
			LogReader nodeLog = nodeData.get(i).getLogReader();
			if (nodeLog == null) {
				log.error(MARKER, "could not load log, exiting validation for node {}", i);
				continue;
			}

			nodeLog.readFully();

			int socketExceptions = 0;
			int invalidEvent = 0;
			int unexpectedErrors = 0;
			for (LogEntry e : nodeLog.getExceptions()) {
				if (e.getMarker() == PlatformLogMarker.SOCKET_EXCEPTIONS) {
					socketExceptions++;
				} else if (e.getMarker() == PlatformLogMarker.INVALID_EVENT_ERROR) {
					invalidEvent++;
				} else {
					if ( e.getLogEntry().contains(OLD_EVENT_PARENT)) {
						addWarning(String.format("Node %d has error:[ %s ]", i, e.getLogEntry()));
					}else {
						unexpectedErrors++;
						isValid = false;
					}
				}
			}

			if (socketExceptions > 0) {
				addInfo(String.format("Node %d has %d socket exceptions. Some are expected for Reconnect.", i,
						socketExceptions));
			}
			if (invalidEvent > 0) {
				addInfo(String.format("Node %d has %d invalid event errors. Some are expected for Reconnect.", i,
						invalidEvent));
			}
			if (unexpectedErrors > 0) {
				addError(String.format("Node %d has %d unexpected errors!", i, unexpectedErrors));
			}

			nodeLog = nodeData.get(nodeNum - 1).getLogReader();
			this.isValid = this.isMigrationValid(nodeNum, nodeLog);
		}

		this.isValidated = true;
	}

	private boolean isMigrationValid(final int nodeNum, final LogReader nodeLog) throws IOException {
		final LogEntry startLoading = nodeLog.nextEntryContaining(Arrays.asList(START_LOADING));
		if (startLoading == null) {
			addError(String.format("Node %d didn't start loading state from disk", nodeNum - 1));
			return false;
		}

		final LogEntry endLoading = nodeLog.nextEntryContaining(Arrays.asList(END_LOADING));
		if (endLoading == null) {
			addError(String.format("Node %d started loading state from disk but didn't finish", nodeNum - 1));
			return false;
		}

		final LogEntry startProcessing = nodeLog.nextEntryContaining(Arrays.asList(START_PROCESSING));
		if (startProcessing == null) {
			addError(String.format("Node %d didn't start processing transactions", nodeNum - 1));
			return false;
		}

		final LogEntry endProcessing = nodeLog.nextEntryContaining(Arrays.asList(END_PROCESSING));
		if (endProcessing == null) {
			addError(String.format("Node %d didn't finish processing transactions", nodeNum - 1));
			return false;
		}

		return true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
