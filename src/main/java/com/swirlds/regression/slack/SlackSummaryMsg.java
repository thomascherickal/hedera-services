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

package com.swirlds.regression.slack;

import com.hubspot.slack.client.models.Attachment;
import com.swirlds.regression.GitInfo;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.experiment.ExperimentSummary;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.SlackConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class SlackSummaryMsg extends SlackMsg {

    private List<Pair<ExperimentSummary, List<ExperimentSummary>>> experimentsWithHistorical;

    private List<Throwable> exceptionList;

    private RegressionConfig regressionConfig;

    private String resultFolder;

    private GitInfo gitInfo;

    static final int MAX_ATTACHMENT_CHAR_LENGTH = 2700;

    public SlackSummaryMsg(SlackConfig slackConfig, RegressionConfig regressionConfig,
                           GitInfo gitInfo, String resultFolder) {
        super(slackConfig);
        experimentsWithHistorical = new ArrayList<>();
        exceptionList = new ArrayList<>();
        this.regressionConfig = regressionConfig;
        this.gitInfo = gitInfo;
        this.resultFolder = resultFolder;
    }

    /**
     * Add an experiment to this summary.
     */
    public void addExperiment(ExperimentSummary experiment) {
        addExperiment(experiment, null);
    }

    /**
     * Add an experiment to this summary with historical data.
     */
    public void addExperiment(ExperimentSummary experiment, List<ExperimentSummary> historical) {
        if (experiment.hasWarnings()) {
            warnings = true;
        }
        if (experiment.hasErrors()) {
            errors = true;
        }
        if (experiment.hasExceptions()) {
            exceptions = true;
        }
        experimentsWithHistorical.add(Pair.of(experiment, historical));
    }

    /**
     * Adds a string to a list and returns the size of the string added
     *
     * @param listToAddTo - List to add the string to
     * @param stringToAdd - string to put in the list
     * @return - size of the string added
     */
    private int addStringToList(List<String> listToAddTo, String stringToAdd) {
        listToAddTo.add(stringToAdd);
        return stringToAdd.length();
    }

    /**
     * Register an exception that prevents an experiment from being added to this summary.
     */
    public void registerException(Throwable e) {
        exceptionList.add(e);
    }

    protected List<Attachment> generateAttachment(String description,
                                                  List<Pair<ExperimentSummary, List<ExperimentSummary>>> experiments, String color) {

        List<Attachment> returnAttachmentList = new ArrayList<Attachment>();
        /* It is far more cost efiecient to keep track of the potential size of the attachment than constantly calculating it */
        int attachmentLength = 0;
        List<String> columnHeaders = new ArrayList<>();

        attachmentLength += generateColumnHeaders(columnHeaders);

        if (experiments.size() > 0) {
            StringBuilder attachmentDescription = setAttachmentDescription(description);
            attachmentLength += attachmentDescription.length();

            List<List<String>> rows = new ArrayList<>();
            rows.add(columnHeaders);
            for (Pair<ExperimentSummary, List<ExperimentSummary>> pair : experiments) {
                attachmentLength += generateRowForExperiment(rows, pair);

                /* close off attachment and reset counters */
                if (attachmentLength >= MAX_ATTACHMENT_CHAR_LENGTH) {
                    returnAttachmentList.add(buildNewAttachment(color, attachmentDescription, rows));
                    rows.clear();
                    attachmentLength = 0;
                    attachmentDescription = new StringBuilder();
                }
            }
            /* close off last attachment */
            if(attachmentLength > 0) {
                returnAttachmentList.add(buildNewAttachment(color, attachmentDescription, rows));
            }

            return returnAttachmentList;
        }
        return null;
    }

    /**
     * Set the description for a given attachment
     *
     * @param description - Describe what type of attachment this is (passing, failing, etc.)
     * @return - String builder with the description of the attachment
     */
    private StringBuilder setAttachmentDescription(String description) {
        StringBuilder sb = new StringBuilder();
        bold(sb, description);
        newline(sb);
        return sb;
    }

    /**
     * Populates a list of column headers in passed in list
     *
     * @param columnHeaders - List of strings containing the column headers
     * @return - the length of all header strings concatenated together
     */
    private int generateColumnHeaders(List<String> columnHeaders) {
        int concatenatedLengthOfHeaderStrings = 0;
        concatenatedLengthOfHeaderStrings += addStringToList(columnHeaders, "Test");
        concatenatedLengthOfHeaderStrings += addStringToList(columnHeaders, "UID");
        concatenatedLengthOfHeaderStrings += addStringToList(columnHeaders, "History");
        return concatenatedLengthOfHeaderStrings;
    }

    /**
     * Generates a row for the table that will populate the slack attachment
     *
     * @param rows - List of each row that will be in the attachment
     * @param pair - A pair containing the experiment summary on the left, and experiment history on the right
     * @return the total number of characters in all of the strings contained in the new row
     */
    private int generateRowForExperiment(List<List<String>> rows, Pair<ExperimentSummary, List<ExperimentSummary>> pair) {
        int concatenatedLengthOfRowStrings = 0;
        ExperimentSummary experiment = pair.getLeft();
        List<String> row = new ArrayList<>();
        String experimentURL = RegressionUtilities.buildResultsFolderURL(slackConfig, resultFolder, experiment.getName());
        String experimentName = experiment.getName();
        concatenatedLengthOfRowStrings += addStringToList(row, createLinkOrReturn(experimentURL, experimentName));
        concatenatedLengthOfRowStrings += addStringToList(row, createLinkOrReturn(experiment.getSlackLink(), experiment.getUniqueId()));
        String hist = "";
        if (pair.getRight() != null) {
            hist = generateExperimentHistory(pair.getRight());
        }
        concatenatedLengthOfRowStrings += hist.length();
        row.add(hist);
        rows.add(row);
        return concatenatedLengthOfRowStrings;
    }

    /**
     * generates all known history for a given experiment
     *
     * @param knownExperimentHistory - a list of ExperimentSummaries of the known history of this experiment
     * @return - A string of the history and slack links for given experiment
     */
    private String generateExperimentHistory(List<ExperimentSummary> knownExperimentHistory) {
        StringBuilder hist = new StringBuilder();
        for (ExperimentSummary experimentSummary : knownExperimentHistory) {
            if (experimentSummary.hasErrors() || experimentSummary.hasExceptions()) {
                hist.append(createLinkOrReturn(experimentSummary.getSlackLink(), "E"));
            } else if (experimentSummary.hasWarnings()) {
                hist.append(createLinkOrReturn(experimentSummary.getSlackLink(), "W"));
            } else {
                hist.append(createLinkOrReturn(experimentSummary.getSlackLink(), "P"));
            }
        }
        return hist.toString();
    }

    /**
     * builds the slack attachment
     *
     * @param color       - color of the outline of the slack attachment
     * @param description - description of the attachment
     * @param rows        - rows to form into a table for the body of the attachment
     * @return - Attachment to pass to slack
     */
    private Attachment buildNewAttachment(String color, StringBuilder description, List<List<String>> rows) {
        Attachment.Builder attachment = Attachment.builder();
        table(description, rows);

        attachment.setText(description.toString());
        attachment.setColor(color);

        return attachment.build();
    }

    @Override
    public List<Attachment> generateSlackMessage(StringBuilder stringBuilder) {

        bold(stringBuilder, "--- Regression Summary ---");
        newline(stringBuilder);

        bold(stringBuilder, "Name: ");
        codeSnippet(stringBuilder, regressionConfig.getName());
        newline(stringBuilder);

        String experimentURL = RegressionUtilities.buildResultsFolderURL(slackConfig, resultFolder, "");
        bold(stringBuilder, "Results Folder:");
        stringBuilder.append(" " + createLinkOrReturn(experimentURL, resultFolder));
        newline(stringBuilder);

        bold(stringBuilder, "Commit:");
        stringBuilder.append(" " + gitInfo.getGitInfo(true));
        newline(stringBuilder);

        stringBuilder.append("Note: historical test data is displayed from left to right (oldest->newest).");
        newline(stringBuilder);

        List<Pair<ExperimentSummary, List<ExperimentSummary>>> successes = new ArrayList<>();
        List<Pair<ExperimentSummary, List<ExperimentSummary>>> warnings = new ArrayList<>();
        List<Pair<ExperimentSummary, List<ExperimentSummary>>> failures = new ArrayList<>();

        // TODO maybe sort experiments alphabetically

        // Sort experiments
        for (Pair<ExperimentSummary, List<ExperimentSummary>> pair : experimentsWithHistorical) {
            ExperimentSummary experiment = pair.getLeft();
            if (experiment.hasExceptions() || experiment.hasErrors()) {
                failures.add(pair);
            } else if (experiment.hasWarnings()) {
                warnings.add(pair);
            } else {
                successes.add(pair);
            }
        }

        List<Attachment> attachments = new ArrayList<>();

        // Passing tests
        List<Attachment> attachment = generateAttachment("Tests that passed", successes, "#00FF00");
        if (attachment != null) {
            attachments.addAll(attachment);
        }

        // Tests with warnings
        attachment = generateAttachment("Tests with warnings", warnings, "#FFFF00");
        if (attachment != null) {
            attachments.addAll(attachment);
        }

        // Failing tests
        attachment = generateAttachment("Tests with errors", failures, "#FF0000");
        if (attachment != null) {
            attachments.addAll(attachment);
        }

        // Exceptions
        for (Throwable t : exceptionList) {
            Attachment.Builder ab = Attachment.builder();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            ab.setText(sw.toString());
            ab.setColor("#FF0000");
            attachments.add(ab.build());
        }

        return attachments;
    }

    @Override
    public String getPlaintext() {
        return null;
    }
}
