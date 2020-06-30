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

import com.swirlds.regression.logs.PlatformLogEntry;
import com.swirlds.regression.logs.services.HAPIClientLogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static com.swirlds.common.logging.PlatformLogMessages.CHANGED_TO_ACTIVE;
import static com.swirlds.common.logging.PlatformLogMessages.FINISHED_RECONNECT;
import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_ERROR;
import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_HASH_MISMATCH;
import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_IO_EXCEPTION;
import static com.swirlds.common.logging.PlatformLogMessages.START_RECONNECT;


public class HAPIClientValidator extends Validator {
	private List<NodeData> testClientNodeData;
	private boolean isValid;
	private boolean isValidated;

	private static final String WRONG_STATUS = "wrong status";
	private static final String STARTING_OF_SUITE = "-------------- STARTING ";
	private static final String RESULTS_OF_SUITE = "-------------- RESULTS OF ";

	public HAPIClientValidator(final List<NodeData> testClientNodeData) {
		this.testClientNodeData = testClientNodeData;
		isValid = false;
		isValidated = false;
	}

	/**
	 * Check log of test results. Parse test logs of each test suite ran starting with
	 * "-------------- STARTING" and results in " -------------- RESULTS OF"
	 *
	 * @throws IOException
	 */
	@Override
	public void validate() throws IOException {
		int nodeNum = testClientNodeData.size();
		int numProblems = 0;
		int wrongStatus = 0;
//		for (int i = 0; i < nodeNum; i++) {
//			LogReader<HAPIClientLogEntry> clientLogReader = testClientNodeData.get(i).
//					getHapiClientLogReader();
//			clientLogReader.readFully();
//			if (clientLogReader.getExceptionCount() > 0) {
//				isValid = false;
//				for (HAPIClientLogEntry le : clientLogReader.getExceptions()) {
//					numProblems++;
//				}
//			}
//		}

		for (int i = 0; i < nodeNum; i++) {
			LogReader<HAPIClientLogEntry> clientLogReader = testClientNodeData.get(i).
					getHapiClientLogReader();
			HAPIClientLogEntry start = clientLogReader.nextEntryContaining(
					Arrays.asList(STARTING_OF_SUITE));
			while (true) {
				int resultErrorsOfSuite = 0;
				int resultPassedOfSuite = 0;
				String suiteName;
				if (start == null) {
					break;
				} else {
					String[] logEntryArray = start.getLogEntry().split("STARTING ");
					suiteName = logEntryArray[1].split("\\s+")[0];
				}

				HAPIClientLogEntry end = clientLogReader.nextEntryContaining(
						Arrays.asList(RESULTS_OF_SUITE, WRONG_STATUS));
				if (end == null) {
					addError(String.format("Node %s started a test, but did not finish!", suiteName));
					isValid = false;
					break;
				} else if (end.getLogEntry().contains(WRONG_STATUS)) {
					wrongStatus++;
				} else {
					while (true) {
						start = clientLogReader.nextEntry();
						if (!start.getLogEntry().contains(suiteName)) {
							break;
						} else if (start.getLogEntry().contains("status=ERROR")) {
							resultErrorsOfSuite++;
						} else if (start.getLogEntry().contains("status=PASSED")) {
							resultPassedOfSuite++;
						}
					}

				}
				if (wrongStatus > 0 || resultErrorsOfSuite > 0) {
					addError(String.format("Suite %s has %d wrong status and %d errors in results",
							suiteName, wrongStatus, resultErrorsOfSuite));
				}
				if (wrongStatus == 0 && resultErrorsOfSuite == 0 && resultPassedOfSuite > 0) {
					addInfo(String.format("Suite %s has %d passed results", suiteName, resultPassedOfSuite));
				}

			}

		}

		if (numProblems == 0) {
			isValid = true;
		}

		if (numProblems > 0) {
			addError("Test had " + numProblems + " exceptions!");
		}

		if (wrongStatus > 0)
			addWarning("Test had " + wrongStatus + " unexpected status");

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValid && isValidated;
	}
}
