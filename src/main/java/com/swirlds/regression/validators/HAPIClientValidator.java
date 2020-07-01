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

import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.services.HAPIClientLogEntry;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class HAPIClientValidator extends Validator {
	private List<NodeData> testClientNodeData;
	private boolean isValid;
	private boolean isValidated;

	private int resultErrorsOfSuite = 0;
	private int resultPassedOfSuite = 0;
	private int wrongStatus = 0;
	private int numProblems = 0;
	private String suiteName = "";

	private static final String WRONG_STATUS = "wrong status";
	private static final String STARTING_OF_SUITE = "-------------- STARTING ";
	private static final String RESULTS_OF_SUITE = "-------------- RESULTS OF ";
	private static final String EXCEPTION = "Exception";
	private static final String ERROR = "ERROR";

	public HAPIClientValidator(final List<NodeData> testClientNodeData) {
		this.testClientNodeData = testClientNodeData;
		isValid = true;
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

		for (int i = 0; i < nodeNum; i++) {
			LogReader<HAPIClientLogEntry> clientLogReader = testClientNodeData.get(i).
					getHapiClientLogReader();
			HAPIClientLogEntry start = clientLogReader.nextEntryContaining(
					Arrays.asList(STARTING_OF_SUITE));
			while (true) {
				if (start == null) {
					break;
				} else {
					String[] logEntryArray = start.getLogEntry().split("STARTING ");
					suiteName = logEntryArray[1].split("\\s+")[0];
				}

				HAPIClientLogEntry end = clientLogReader.nextEntryContaining(
						Arrays.asList(RESULTS_OF_SUITE, WRONG_STATUS, EXCEPTION, ERROR));

				if (end == null) {
					addError(String.format("Suite %s started, but did not finish!", suiteName));
					validateProblemsInSuite();
					isValid = false;
					break;
				} else if (end.getLogEntry().contains(WRONG_STATUS)) {
					wrongStatus++;
					addError("Suite " + suiteName + " : " + end.getLogEntry());
				} else if (end.isException()) {
					//Some exception is found
					numProblems++;
					addError("Suite " + suiteName + " : " + end.getLogEntry());
				} else {
					//check results of the suite and validate problems
					start = validateResultsOfCurrentSuite(clientLogReader);
				}
			}
		}
		isValidated = true;
	}

	private HAPIClientLogEntry validateResultsOfCurrentSuite(LogReader<HAPIClientLogEntry> clientLogReader)
			throws IOException {
		HAPIClientLogEntry start;
		while (true) {
			start = clientLogReader.nextEntry();
			if (start == null) {
				validateProblemsInSuite();
				return null;
			}
			boolean isCurrentSuiteResults = checkResultsOfSuite(start);
			if (!isCurrentSuiteResults) {
				validateProblemsInSuite();
				break;
			}
		}
		return start;
	}

	private void validateProblemsInSuite() {
		validateProblems();
		initializeVariables();
	}

	private boolean checkResultsOfSuite(HAPIClientLogEntry start) {
		if (!start.getLogEntry().contains(suiteName)) {
			return false;
		} else if (start.getLogEntry().contains("status=ERROR")) {
			resultErrorsOfSuite++;
		} else if (start.getLogEntry().contains("status=PASSED")) {
			resultPassedOfSuite++;
		}
		return true;
	}

	@Override
	public boolean isValid() {
		return isValid && isValidated;
	}

	private void validateProblems() {
		if (wrongStatus > 0 || resultErrorsOfSuite > 0 || numProblems > 0) {
			addError(String.format("Suite %s has %d wrong status, %d exceptions and " +
							" %d failed tests",
					suiteName, wrongStatus, numProblems, resultErrorsOfSuite));
			isValid &= false;
		}
		if (wrongStatus == 0 && resultErrorsOfSuite == 0 && numProblems == 0 && resultPassedOfSuite > 0) {
			isValid &= true;
			addInfo(String.format("Suite %s has %d passed results", suiteName, resultPassedOfSuite));
		}
	}

	private void initializeVariables() {
		resultErrorsOfSuite = 0;
		resultPassedOfSuite = 0;
		wrongStatus = 0;
		numProblems = 0;
		suiteName = "";
	}
}
