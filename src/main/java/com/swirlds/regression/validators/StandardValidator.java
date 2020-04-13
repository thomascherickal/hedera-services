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
import com.swirlds.regression.logs.PlatformLogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
				} else {
					badExceptions++;
					isValid = false;
				}
			}

			if (sockExAtEnd > 0) {
				addInfo(String.format(
						"Node %d has %d socket exceptions at the end of the run. " +
								"This happens when nodes don't die at the same time.",
						i, sockExAtEnd));
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
