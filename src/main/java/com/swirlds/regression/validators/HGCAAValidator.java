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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HGCAAValidator extends Validator{
    private List<NodeData> hederaNodeData;
    private boolean isValid;
    private boolean isValidated;
    private int errorCount = 0;
    private int exceptionCount = 0;
    private List<List<String>> resultMessage;
    private List<String> errors;
    private List<String> exceptions;

    private static final String EXCEPTION = "Exception";
    private static final String ERROR = "ERROR";

    public HGCAAValidator(List<NodeData> testClientNodeData) {
        this.hederaNodeData = testClientNodeData;
        isValid = true;
        isValidated = false;
        resultMessage = new ArrayList<>();
        errors = new ArrayList<>();
        exceptions = new ArrayList<>();
    }

    @Override
    public void validate() throws IOException {
        int nodeNum = hederaNodeData.size();

        for (int i = 0; i < nodeNum; i++) {
            LogReader<HAPIClientLogEntry> hgcaaLogReader = hederaNodeData.get(i).
                    getHapiClientLogReader();

            HAPIClientLogEntry logLine = hgcaaLogReader.nextEntry();

            while ( logLine != null ) {
                if(logLine.getLogEntry().contains(EXCEPTION)) {
                    isValid = false;
                    exceptions.add(logLine.getLogEntry());
                    exceptionCount++;
                } else if(logLine.getLogEntry().contains(ERROR)) {
                    isValid = false;
                    errorMessages.add(logLine.getLogEntry());
                    errorCount++;
                }
                logLine = hgcaaLogReader.nextEntry();
            }
            validateResultsAndBuildMessage();
        }
        createTableOfResults();
        isValidated = true;
        return;
    }

    private void validateResultsAndBuildMessage() {
        if( errorCount > 0 || exceptionCount > 0 ){
            resultMessage.add(new ArrayList<>(Arrays.asList(String.format("Number of errors : {}", errorCount))));
            resultMessage.add(new ArrayList<>(Arrays.asList(String.format("Number of exceptions : {}", exceptionCount))));
            resultMessage.add(errors);
            resultMessage.add(exceptions);
        }
    }

    /**
     * Creates a table with the passed and failed suites of the test, to post to slack channel.
     */
    private void createTableOfResults() {
//        if (resultMessage.size() == 1) {
//            StringBuilder passed = new StringBuilder();
//            SlackMsg.table(passed, resultMessage);
//            addInfo(passed.substring(4, passed.lastIndexOf("\n")));
//        } else {
            StringBuilder failedTests = new StringBuilder();
            SlackMsg.table(failedTests, resultMessage);
            if(!failedTests.toString().isEmpty()) {
                addError(failedTests.toString());
            }
//        }
    }

    @Override
    public boolean isValid() {
        return isValid && isValidated;
    }
}
