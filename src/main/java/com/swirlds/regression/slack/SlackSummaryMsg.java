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
import com.swirlds.regression.Experiment;
import com.swirlds.regression.GitInfo;
import com.swirlds.regression.experiment.ExperimentSummary;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.SlackConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class SlackSummaryMsg extends SlackMsg {

	private List<Pair<ExperimentSummary, List<ExperimentSummary>>> experimentsWithHistorical;

	private List<Throwable> exceptionList;

	private RegressionConfig regressionConfig;

	private String resultFolder;

	private GitInfo gitInfo;

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
	 * Register an exception that prevents an experiment from being added to this summary.
	 */
	public void registerException(Throwable e) {
		exceptionList.add(e);
	}

	protected Attachment generateAttachment(String description,
			List<Pair<ExperimentSummary, List<ExperimentSummary>>> experiments, String color) {
		List<String> columnHeaders = new ArrayList<>();
		columnHeaders.add("Test");
		columnHeaders.add("UID");
		columnHeaders.add("History");

		if (experiments.size() > 0) {
			Attachment.Builder attachment = Attachment.builder();
			StringBuilder sb = new StringBuilder();
			bold(sb, description);
			newline(sb);
			List<List<String>> rows = new ArrayList<>();
			rows.add(columnHeaders);
			for (Pair<ExperimentSummary, List<ExperimentSummary>> pair : experiments) {
				ExperimentSummary experiment = pair.getLeft();
				List<String> row = new ArrayList<>();
				String experimentName = experiment.getName();
				if(slackConfig.getWebServer().isUseWebServer()){
					experimentName = "<http://" + slackConfig.getWebServer().getWebServerAddress() +
							":" + slackConfig.getWebServer().getWebServerPort() +"/" +
							resultFolder + "/" + experimentName + "|" + experimentName + ">" ;
				}
				row.add(experimentName);
				row.add(experiment.getUniqueId());
				StringBuilder hist = new StringBuilder();
				if (pair.getRight() != null) {
					for (ExperimentSummary experimentSummary : pair.getRight()) {
						if (experimentSummary.hasErrors() || experimentSummary.hasExceptions()) {
							hist.append('E');
						} else if (experimentSummary.hasWarnings()) {
							hist.append('W');
						} else {
							hist.append('P');
						}
					}
				}
				row.add(hist.toString());
				rows.add(row);
			}
			table(sb, rows);

			attachment.setText(sb.toString());
			attachment.setColor(color);

			return attachment.build();
		}
		return null;
	}

	@Override
	public List<Attachment> generateSlackMessage(StringBuilder stringBuilder) {

		bold(stringBuilder, "--- Regression Summary ---");
		newline(stringBuilder);

		bold(stringBuilder, "Name: ");
		codeSnippet(stringBuilder, regressionConfig.getName());
		newline(stringBuilder);

		bold(stringBuilder, "Results Folder:");
		stringBuilder.append(" " + resultFolder);
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
		Attachment attachment = generateAttachment("Tests that passed", successes, "#00FF00");
		if (attachment != null) {
			attachments.add(attachment);
		}

		// Tests with warnings
		attachment = generateAttachment("Tests with warnings", warnings, "#FFFF00");
		if (attachment != null) {
			attachments.add(attachment);
		}

		// Failing tests
		attachment = generateAttachment("Tests with errors", failures, "#FF0000");
		if (attachment != null) {
			attachments.add(attachment);
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
