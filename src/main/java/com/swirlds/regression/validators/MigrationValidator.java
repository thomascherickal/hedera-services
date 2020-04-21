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

import com.swirlds.common.logging.LogMarkerInfo;
import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogEntry;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_ERROR;

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

		for (int index = 0; index < nodeNum; index++) {
			final LogReader<PlatformLogEntry> nodeLog = nodeData.get(index).getLogReader();
			if (nodeLog == null) {
				addError(String.format( "Could not load log for node %d", index));
				this.isValid = false;
				continue;
			}

			this.isValid &= this.checkExceptions(nodeLog, index);

			this.isValid &= this.isMigrationValid(index, nodeLog);
		}

		this.isValidated = true;
	}

	private boolean isMigrationValid(final int nodeNum, final LogReader<PlatformLogEntry> nodeLog) throws IOException {
		final LogEntry startLoading = nodeLog.nextEntryContaining(Collections.singletonList(START_LOADING));
		if (startLoading == null) {
			addError(String.format("Node %d didn't start loading state from disk", nodeNum));
			return false;
		}

		final LogEntry endLoading = nodeLog.nextEntryContaining(Collections.singletonList(END_LOADING));
		if (endLoading == null) {
			addError(String.format("Node %d started loading state from disk but didn't finish", nodeNum));
			return false;
		}

		final LogEntry startProcessing = nodeLog.nextEntryContaining(Collections.singletonList(START_PROCESSING));
		if (startProcessing == null) {
			addError(String.format("Node %d didn't start processing transactions", nodeNum));
			return false;
		}

		final LogEntry endProcessing = nodeLog.nextEntryContaining(Collections.singletonList(END_PROCESSING));
		if (endProcessing == null) {
			addError(String.format("Node %d didn't finish processing transactions", nodeNum));
			return false;
		}

		return true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}

	boolean checkExceptions(final LogReader<PlatformLogEntry> nodeLog, final int nodeId) {
		int socketExceptions = 0;
		int unexpectedErrors = 0;

		for (PlatformLogEntry e : nodeLog.getExceptions()) {
			if (e.getMarker() == LogMarkerInfo.SOCKET_EXCEPTIONS) {
				socketExceptions++;
			} else if (e.getLogEntry().contains("Signed state loaded from disk has an invalid hash")) {
			} else {
				unexpectedErrors++;
				addError(String.format("Node %d has unexpected error: [%s]", nodeId, e.getLogEntry()));
			}
		}

		if (socketExceptions > 0) {
			addInfo(String.format("Node %d has %d socket exceptions.",
					nodeId,
					socketExceptions));
		}

		if (unexpectedErrors > 0) {
			addError(String.format("Node %d has %d unexpected errors!", nodeId, unexpectedErrors));
			return false;
		}

		return true;
	}
}
