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
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.SlackConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class SlackSummaryMsg extends SlackMsg {

	private List<Experiment> experiments;

	private List<Throwable> exceptionList;

	private RegressionConfig regressionConfig;

	private String resultFolder;

	private GitInfo gitInfo;

	public SlackSummaryMsg(SlackConfig slackConfig, RegressionConfig regressionConfig,
			GitInfo gitInfo, String resultFolder) {
		super(slackConfig);
		experiments = new ArrayList<>();
		exceptionList = new ArrayList<>();
		this.regressionConfig = regressionConfig;
		this.gitInfo = gitInfo;
		this.resultFolder = resultFolder;
	}

	/**
	 * Add an experiment to this summary.
	 */
	public void addExperiment(Experiment experiment) {
		if (experiment.hasWarnings()) {
			warnings = true;
		}
		if (experiment.hasErrors()) {
			errors = true;
		}
		if (experiment.hasExceptions()) {
			exceptions = true;
		}
		experiments.add(experiment);
	}

	/**
	 * Register an exception that prevents an experiment from being added to this summary.
	 */
	public void registerException(Throwable e) {
		exceptionList.add(e);
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

		// TODO write test result folder, maybe give link to results
		// TODO link to each individual test results
		// TODO add total test time

		List<Experiment> successes = new ArrayList<>();
		List<Experiment> warnings = new ArrayList<>();
		List<Experiment> failures = new ArrayList<>();

		// TODO maybe sort experiments alphabetically

		// Sort experiments
		for (Experiment experiment: experiments) {
			if (experiment.hasExceptions() || experiment.hasErrors()) {
				failures.add(experiment);
			} else if (experiment.hasWarnings()) {
				warnings.add(experiment);
			} else {
				successes.add(experiment);
			}
		}

		List<Attachment> attachments = new ArrayList<>();

		List<String> columnHeaders = new ArrayList<>();
		columnHeaders.add("Test");
		columnHeaders.add("Unique Identifier");
		columnHeaders.add("Test Start Time");

		// Passing tests
		if (successes.size() > 0) {
			StringBuilder sb = new StringBuilder();
			bold(sb, "Tests that passed");
			newline(sb);
			List<List<String>> rows = new ArrayList<>();
			rows.add(columnHeaders);
			Attachment.Builder attachment = Attachment.builder();
			for (Experiment experiment: successes) {
				List<String> row = new ArrayList<>();
				row.add(experiment.getName());
				row.add(experiment.getName() + "-" + resultFolder);
				ZonedDateTime time = experiment.getExperimentTime();
				row.add(time.getHour() + ":" + time.getMinute() + ":" + time.getSecond());
				rows.add(row);
			}
			table(sb, rows);

			attachment.setText(sb.toString());
			attachment.setColor("#00FF00");
			attachments.add(attachment.build());
		}

		// Tests with warnings
		if (warnings.size() > 0) {
			StringBuilder sb = new StringBuilder();
			bold(sb, "Tests with warnings");
			newline(sb);
			List<List<String>> rows = new ArrayList<>();
			rows.add(columnHeaders);
			Attachment.Builder attachment = Attachment.builder();
			for (Experiment experiment: warnings) {
				List<String> row = new ArrayList<>();
				row.add(experiment.getName());
				row.add(experiment.getName() + "-" + resultFolder);
				ZonedDateTime time = experiment.getExperimentTime();
				row.add(time.getHour() + ":" + time.getMinute() + ":" + time.getSecond());
				rows.add(row);
			}
			table(sb, rows);
			attachment.setText(sb.toString());
			attachment.setColor("#FFFF00");
			attachments.add(attachment.build());
		}

		// Tests with errors
		if (failures.size() > 0) {
			StringBuilder sb = new StringBuilder();
			bold(sb, "Tests with errors");
			newline(sb);
			List<List<String>> rows = new ArrayList<>();
			rows.add(columnHeaders);
			Attachment.Builder attachment = Attachment.builder();
			for (Experiment experiment: failures) {
				List<String> row = new ArrayList<>();
				row.add(experiment.getName());
				row.add(experiment.getName() + "-" + resultFolder);
				ZonedDateTime time = experiment.getExperimentTime();
				row.add(time.getHour() + ":" + time.getMinute() + ":" + time.getSecond());
				rows.add(row);
			}
			table(sb, rows);
			attachment.setText(sb.toString());
			attachment.setColor("#FF0000");
			attachments.add(attachment.build());

		}

		// Exceptions
		for (Throwable t: exceptionList) {
			Attachment.Builder attachment = Attachment.builder();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			attachment.setText(sw.toString());
			attachment.setColor("#FF0000");
			attachments.add(attachment.build());
		}

		return attachments;
	}

	@Override
	public String getPlaintext() {
		return null;
	}
}
