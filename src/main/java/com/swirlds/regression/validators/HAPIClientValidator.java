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
import com.swirlds.regression.slack.SlackMsg;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HAPIClientValidator extends Validator {
	private List<NodeData> testClientNodeData;
	private boolean isValid;
	private boolean isValidated;

	private int resultErrorsOfSuite = 0;
	private int resultPassedOfSuite = 0;
	private int wrongStatus = 0;
	private int numProblems = 0;
	private String suiteName = "";

	private ArrayList<String> failedSpecs;
	private List<List<String>> passedSuites;
	private List<List<String>> failedSuites;

	private static final String WRONG_STATUS = "wrong status";
	private static final String STARTING_OF_SUITE = "-------------- STARTING ";
	private static final String RESULTS_OF_SUITE = "-------------- RESULTS OF ";
	private static final String EXCEPTION = "Exception";
	private static final String ERROR = "ERROR";

	private static final String SEPERATOR = ", ";

	/**
	 * Validator to validate logs from test client when running hedera-services regression
	 *
	 * @param testClientNodeData
	 */
	public HAPIClientValidator(final List<NodeData> testClientNodeData) {
		this.testClientNodeData = testClientNodeData;
		isValid = true;
		isValidated = false;
		initializeLists();
		buildColumnHeaders();
	}

	/**
	 * Check logs of test results. Parse test logs of each test suite ran starting with
	 * "-------------- STARTING" and results in " -------------- RESULTS OF".
	 * Also validates if there are any exceptions
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
				if (start == null || !start.getLogEntry().contains(STARTING_OF_SUITE)) {
					break;
				} else {
					String[] logEntryArray = start.getLogEntry().split("STARTING ");
					suiteName = logEntryArray[1].split("\\s+")[0];
				}

				HAPIClientLogEntry end = clientLogReader.nextEntryContaining(
						Arrays.asList(RESULTS_OF_SUITE, WRONG_STATUS, EXCEPTION, ERROR));

				if (end == null) {
					addError(String.format("Suite %s started, but did not finish!", suiteName));
					validateSuiteResults();
					isValid = false;
					break;
				} else if (end.getLogEntry().contains(WRONG_STATUS)) {
					wrongStatus++;
				} else if (end.isException()) {
					//Some exception is found
					numProblems++;
				} else {
					//check results of the suite and validate problems
					start = validateResultsOfCurrentSuite(clientLogReader);
				}
			}
		}
		createTableOfResults();
		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValid && isValidated;
	}

	/**
	 * Creates a table with the passed and failed suites of the test, to post to slack channel.
	 */
	private void createTableOfResults() {
		if (passedSuites.size() > 1) {
			StringBuilder passedTests = new StringBuilder();
			SlackMsg.table(passedTests, passedSuites);
			addInfo(passedTests.substring(4, passedTests.lastIndexOf("\n")));
		}
		if (failedSuites.size() > 1) {
			StringBuilder failedTests = new StringBuilder();
			SlackMsg.table(failedTests, failedSuites);
			addError(failedTests.substring(4, failedTests.lastIndexOf("\n")));
		}
	}

	/**
	 * Checks all the results of current Suite in RESULTS section.
	 * Break the loop after reaching to starting of next suite or when reaching end of the file
	 *
	 * @param logReader
	 * @return
	 * @throws IOException
	 */
	private HAPIClientLogEntry validateResultsOfCurrentSuite(LogReader<HAPIClientLogEntry> logReader)
			throws IOException {
		HAPIClientLogEntry entry;
		while (true) {
			entry = logReader.nextEntry();
			if (entry == null) {
				validateSuiteResults();
				return null;
			}
			boolean isCurrentSuiteResults = checkResultsOfSuite(entry);
			if (!isCurrentSuiteResults) {
				validateSuiteResults();
				break;
			}
		}
		return entry;
	}

	/**
	 * Validates if the suite has passed or failed based on number of wrong status messages,
	 * number of exceptions and number of failed tests in the suite
	 */
	private void validateSuiteResults() {
		boolean isPassed;
		if (isSuitePassed()) {
			isValid &= true;
			isPassed = true;
		} else {
			isValid &= false;
			isPassed = false;
		}
		buildMessage(isPassed);
		clear();
	}

	/**
	 * validates if suite is passed or failed based on number of wrong status messages,
	 * number of exceptions and number of failed/passed tests in the suite
	 *
	 * @return
	 */
	private boolean isSuitePassed() {
		return wrongStatus == 0 && resultErrorsOfSuite == 0
				&& numProblems == 0 && resultPassedOfSuite > 0;
	}

	/**
	 * Build passed/fail message of the suite result
	 *
	 * @param isPassed
	 */
	private void buildMessage(boolean isPassed) {
		if (isPassed) {
			passedSuites.add(new ArrayList<>(Arrays.asList(suiteName, "PASSED")));
		} else {
			StringBuilder failResult = new StringBuilder();
			failResult.append(StringUtils.join(failedSpecs, SEPERATOR));
			if (wrongStatus > 0) {
				failResult.append("\n");
				failResult.append("Wrong Status:" + wrongStatus);
			}
			if (numProblems > 0) {
				failResult.append("\n");
				failResult.append("Exceptions:" + numProblems);
			}
			failedSuites.add(new ArrayList<>(Arrays.asList(suiteName, "FAILED", failResult.toString())));
		}
	}

	/**
	 * After reaching RESULTS section of the suite verify if any tests in suite failed/passed
	 *
	 * @param resultEntry
	 * @return
	 */
	private boolean checkResultsOfSuite(HAPIClientLogEntry resultEntry) {
		if (!resultEntry.getLogEntry().contains(suiteName)) {
			return false;
		} else if (isResultEntryPassed(resultEntry)) {
			resultPassedOfSuite++;
		} else if (isResultEntryFailed(resultEntry)) {
			parseFailedSpecs(resultEntry);
			resultErrorsOfSuite++;
		}
		return true;
	}

	/**
	 * Check if the result entry contains FAILED or ERROR
	 *
	 * @param resultEntry
	 * @return
	 */
	private boolean isResultEntryFailed(HAPIClientLogEntry resultEntry) {
		return resultEntry.getLogEntry().contains("status=FAILED")
				|| resultEntry.getLogEntry().contains("status=ERROR");
	}

	/**
	 * check if result entry contains PASSED
	 *
	 * @param resultEntry
	 * @return
	 */
	private boolean isResultEntryPassed(HAPIClientLogEntry resultEntry) {
		return resultEntry.getLogEntry().contains("status=PASSED");
	}

	/**
	 * get names of failed tests of the suite from the results section
	 *
	 * @param resultEntry
	 */
	private void parseFailedSpecs(HAPIClientLogEntry resultEntry) {
		failedSpecs.addAll(Stream.of(resultEntry.getLogEntry().
				split("Spec"))
				.filter(s -> s.contains("name="))
				.map(s -> s.substring(s.indexOf("=") + 1, s.indexOf(",")))
				.collect(Collectors.toList()));
	}

	/**
	 * reset all variables used
	 */
	private void clear() {
		resultErrorsOfSuite = 0;
		resultPassedOfSuite = 0;
		wrongStatus = 0;
		numProblems = 0;
		suiteName = "";
		failedSpecs = new ArrayList<>();
	}

	/**
	 * initialize arraylists used in the validator
	 */
	private void initializeLists() {
		failedSpecs = new ArrayList<>();
		passedSuites = new ArrayList<>();
		failedSuites = new ArrayList<>();
	}

	/**
	 * build column headers for the passed tests table and failed tests table
	 */
	private void buildColumnHeaders() {
		passedSuites.add(new ArrayList<>(Arrays.asList("Suite", "Result")));
		failedSuites.add(new ArrayList<>(Arrays.asList("Suite", "Result", "Failures")));
	}
}
