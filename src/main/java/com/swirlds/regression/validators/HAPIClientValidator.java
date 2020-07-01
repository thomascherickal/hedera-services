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

import com.amazonaws.http.conn.ssl.ShouldClearSslSessionPredicate;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.services.HAPIClientLogEntry;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collector;
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
	private ArrayList<String> failedSpecs = new ArrayList<>();

	private static final String WRONG_STATUS = "wrong status";
	private static final String STARTING_OF_SUITE = "-------------- STARTING ";
	private static final String RESULTS_OF_SUITE = "-------------- RESULTS OF ";
	private static final String EXCEPTION = "Exception";
	private static final String ERROR = "ERROR";

	private static final String SEPERATOR = "	";

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
					validateProblemsInSuite();
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

	private boolean checkResultsOfSuite(HAPIClientLogEntry resultEntry) {
		if (!resultEntry.getLogEntry().contains(suiteName)) {
			return false;
		} else if (isPassed(resultEntry)) {
			resultPassedOfSuite++;
		} else if (isFailed(resultEntry)) {
			parseFailedSpecs(resultEntry);
			resultErrorsOfSuite++;
		}
		return true;
	}

	@Override
	public boolean isValid() {
		return isValid && isValidated;
	}

	private void validateProblems() {
		StringBuilder result = new StringBuilder();
		result.append(suiteName);

		if (wrongStatus == 0 && resultErrorsOfSuite == 0
				&& numProblems == 0 && resultPassedOfSuite > 0) {
			isValid &= true;
			result.append(SEPERATOR);
			result.append("PASSED ");
			addInfo(result.toString());
		} else if (wrongStatus > 0 || resultErrorsOfSuite > 0 || numProblems > 0) {
			result.append(SEPERATOR);
			result.append("FAILED ");
			addError(buildErrorMessage(result));
			isValid &= false;
		}
	}

	private String buildErrorMessage(StringBuilder result) {
		result.append(SEPERATOR);
		result.append(failedSpecs.size() > 0 ?
				"Failed Tests: "+ StringUtils.join(failedSpecs, ", ")
				:"");
		if (wrongStatus > 0) {
			result.append(SEPERATOR);
			result.append("Wrong Status:" + wrongStatus);
		}
		if (numProblems > 0) {
			result.append(SEPERATOR);
			result.append("Exceptions:" + numProblems);
		}
		return result.toString();
	}

	private void initializeVariables() {
		resultErrorsOfSuite = 0;
		resultPassedOfSuite = 0;
		wrongStatus = 0;
		numProblems = 0;
		suiteName = "";
		failedSpecs = new ArrayList<>();
	}

	private boolean isFailed(HAPIClientLogEntry resultEntry) {
		return resultEntry.getLogEntry().contains("status=FAILED")
				|| resultEntry.getLogEntry().contains("status=ERROR");
	}

	private boolean isPassed(HAPIClientLogEntry resultEntry) {
		return resultEntry.getLogEntry().contains("status=PASSED");
	}

	private void parseFailedSpecs(HAPIClientLogEntry resultEntry) {
		failedSpecs.addAll(Stream.of(resultEntry.getLogEntry().
				split("Spec"))
				.filter(s -> s.contains("name="))
				.map(s -> s.substring(s.indexOf("=") + 1, s.indexOf(",")))
				.collect(Collectors.toList()));
	}

}
