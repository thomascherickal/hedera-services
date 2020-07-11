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

import com.swirlds.common.logging.LogMarkerInfo;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.logs.PlatformLogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.SYNC_CALLER_BROKEN;
import static com.swirlds.regression.RegressionUtilities.SYNC_LISTENER_BROKEN;
import static com.swirlds.regression.RegressionUtilities.SYNC_SERVER_BROKEN;

/**
 * Reads in swirlds.log file
 */

public class StandardValidator extends NodeValidator {
	private boolean isValidated = false;
	private boolean isValid = true;

	public StandardValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	@Override
	public void validate() throws IOException {
		int nodeNum = nodeData.size();
		for (int i = 0; i < nodeNum; i++) {
			LogReader<PlatformLogEntry> nodeLog = nodeData.get(i).getLogReader();
			nodeLog.readFully();
		}
		for (int i = 0; i < nodeNum; i++) {
			LogReader<PlatformLogEntry> nodeLog = nodeData.get(i).getLogReader();
			int sockExAtEnd = 0;
			int badExceptions = 0;
			/** Count the exception due to broken sync tcp connection */
			long syncBrokenCount = 0;
			Instant nodeEnd = nodeLog.getLastEntryRead().getTime();
			for (PlatformLogEntry ex : nodeLog.getExceptions()) {
				if (ex.getMarker() == LogMarkerInfo.SOCKET_EXCEPTIONS) {
					Duration dur = Duration.between(ex.getTime(), nodeEnd);
					if (dur.toSeconds() < 10) {
						// if the socket exceptions happen at the end, that's ok
						sockExAtEnd++;
					} else {
						badExceptions++;
						isValid = false;
					}
				} else if (ex.getLogEntry().contains(RegressionUtilities.FALL_BEHIND_MSG)){
					badExceptions++;
					isValid = false;
					addError(String.format("Node %d has fallen behind.", i));
				} else if (ex.getLogEntry().contains(SYNC_CALLER_BROKEN)){
					syncBrokenCount++;
				} else if (ex.getLogEntry().contains(SYNC_LISTENER_BROKEN)){
					syncBrokenCount++;
				} else if (ex.getLogEntry().contains(SYNC_SERVER_BROKEN)){
					syncBrokenCount++;
				} else {
					badExceptions++;
					addError(String.format("Node %d has exception:%s", i, ex.getLogEntry()));
					isValid = false;
				}
			}

			if (sockExAtEnd > 0) {
				addInfo(String.format(
						"Node %d has %d socket exceptions at the end of the run. " +
								"This happens when nodes don't die at the same time.",
						i, sockExAtEnd));
			}
			if (syncBrokenCount > 0){
				addWarning(String.format(
						"Node %d has %d broken sync tcp exceptions at the end of the run. ",
						i, syncBrokenCount));
			}
			if (badExceptions > 0) {
				addError(String.format("Node %d has %d unexpected errors!", i, badExceptions));
			}
		}

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
