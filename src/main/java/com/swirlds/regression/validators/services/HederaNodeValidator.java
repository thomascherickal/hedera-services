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

import com.swirlds.common.logging.LogMarkerInfo;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogEntry;
import com.swirlds.regression.logs.services.HAPIClientLogEntry;
import com.swirlds.regression.validators.NodeData;
import com.swirlds.regression.validators.Validator;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.swirlds.regression.RegressionUtilities.FALL_BEHIND_MSG;

/**
 * Validates swirlds.log in services-regression.
 * It needs a different validator from STANDARD Validator, as it will ignore some errors differently
 * from STANDARD Validator. This validator will be improved in future
 */
public class HederaNodeValidator extends Validator {
	private List<NodeData> hederaNodeData;
	private boolean isValid;
	private boolean isValidated;

	private static final String SIGNATURE_FAILURE = "Adv Crypto Subsystem: Signature Verification Failure";

	public HederaNodeValidator(List<NodeData> nodeData) {
		this.hederaNodeData = nodeData;
		isValid = true;
		isValidated = false;
	}

	@Override
	public void validate() throws IOException {
		validateNodeLogs();
		isValidated = true;
	}

	private void validateNodeLogs() throws IOException {
		int nodeNum = hederaNodeData.size();

		for (int i = 0; i < nodeNum; i++) {
			LogReader<PlatformLogEntry> nodeLog = hederaNodeData.get(i).getLogReader();
			nodeLog.readFully();
		}
		for (int i = 0; i < nodeNum; i++) {
			LogReader<PlatformLogEntry> nodeLog = hederaNodeData.get(i).getLogReader();
			int sockExAtEnd = 0;
			int badExceptions = 0;
			long signatureFailure = 0;

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
				} else if (ex.getLogEntry().contains(SIGNATURE_FAILURE)) {
					// signature failure messages can be ignored when running services-regression
					// as some tests intentionally send bad transactions
					signatureFailure++;
				} else {
					if (ex.getLogEntry().contains(FALL_BEHIND_MSG)) {
						addError(String.format("Node %d has fallen behind.", i));
					} else {
						addError(String.format("Node %d has exception:%s", i, ex.getLogEntry()));
					}
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
				addError(String.format("Node %d has %d errors!", i, badExceptions));
			}

			if (signatureFailure > 0) {
				addInfo(String.format(
						"Node %d has %d signature failure exceptions at the end of the run. " +
								"This can happens as test clients might submit transactions " +
								"signed intentionally with wrong keys",
						i, signatureFailure));
			}
		}
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
