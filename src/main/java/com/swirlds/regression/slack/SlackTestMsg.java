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

package com.swirlds.regression.slack;

import com.hubspot.slack.client.models.Attachment;
import com.swirlds.regression.GitInfo;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.validators.Validator;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SlackTestMsg extends SlackMsg {

	private RegressionConfig regConfig;
	private TestConfig testConfig;
	private String resultsFolder;
	private GitInfo gitInfo;

	private List<Pair<Validator, Throwable>> validators = new LinkedList<>();

	private List<String> Errors = new LinkedList<>();
	private List<String> Warnings = new LinkedList<>();
	private List<Throwable> Exceptions = new LinkedList<>();

	private String uniqueId;

	public SlackTestMsg(String uniqueId, RegressionConfig regConfig, TestConfig testConfig,
			String resultsFolder, GitInfo gitInfo) {
		super(regConfig.getSlack());
		this.regConfig = regConfig;
		this.testConfig = testConfig;
		this.resultsFolder = resultsFolder;
		this.gitInfo = gitInfo;
		this.uniqueId = uniqueId;
	}

	public SlackTestMsg(String uniqueId, RegressionConfig regConfig, TestConfig testConfig) {
		this(uniqueId, regConfig, testConfig, null, null);
	}

	public SlackTestMsg(String uniqueId, RegressionConfig regConfig) {
		this(uniqueId, regConfig, null, null, null);
	}

	public void addValidatorInfo(Validator v) {
		validators.add(Pair.of(v, null));
	}

	public void addValidatorException(Validator v, Throwable t) {
		validators.add(Pair.of(v, t));
	}

	public void addError(String error) {
		errors = true;
		Errors.add(error);
	}

	public void addExceptions(Throwable exception) {
		exceptions = true;
		Exceptions.add(exception);
	}

	public void addWarning(String warning) {
		warnings = true;
		Warnings.add(warning);
	}

	public String getUniqueId() {
		return uniqueId == null ? "" : uniqueId;
	}

	@Override
	public List<Attachment> generateSlackMessage(StringBuilder stringBuilder) {
		addTestSummary(stringBuilder);

		List<Attachment> attachments = new ArrayList<>();
		for (Pair<Validator, Throwable> pair : validators) {
			Validator v = pair.getLeft();
			Throwable t = pair.getRight();
			StringBuilder sbVal = new StringBuilder();
			if (t != null) {
				appendValidatorException(v, sbVal, t);
			} else {
				appendValidatorInfo(v, sbVal);
			}
			Attachment.Builder attach = Attachment.builder();
			attach.setText(sbVal.toString());

			// if [there is an exception] OR [test not valid] OR [test has errors]
			if (t != null || !v.isValid() || v.hasErrors()) {
				attach.setColor("#FF0000");
				errors = true;
			} else if (v.hasWarnings()) {
				attach.setColor("#FFFF00");
				warnings = true;
			} else {
				attach.setColor("#00FF00");
			}

			attachments.add(attach.build());
		}

		return attachments;
	}

	public String getPlaintext() {
		StringBuilder s = new StringBuilder();

		addTestSummary(s);
		for (Pair<Validator, Throwable> pair : validators) {
			Validator v = pair.getLeft();
			Throwable t = pair.getRight();
			if (t != null) {
				appendValidatorException(v, s, t);
			} else {
				appendValidatorInfo(v, s);
			}
		}

		return s.toString();
	}

	private void addTestSummary(StringBuilder s) {
		s.append("*Run:* ").append(regConfig.getName()).append(NEWLINE);
		if (testConfig != null) {
			s.append("*Test:* `").append(testConfig.getName()).append("`").append(NEWLINE);
			s.append("*Configured Test Duration:* ").append(testConfig.getDuration()).append(NEWLINE);
		}
		if (resultsFolder != null) {
			s.append("*Results Folder:* ").append(resultsFolder).append(NEWLINE);
		}
		if (uniqueId != null) {
			s.append("*Unique Identifier:* ").append(uniqueId).append(NEWLINE);
		}
		if (gitInfo != null) {
			s.append("*Git:* ")
					.append(gitInfo.getGitInfo(true))
					.append(" - ")
					.append(gitInfo.getLastCommitDate())
					.append(NEWLINE);
		}

		if (Errors.size() > 0 || Warnings.size() > 0) {
			s.append("--------------------------------").append(NEWLINE);
		}

		for (String testWarning : Warnings) {
			s.append("*WARNING*: ").append(testWarning).append(NEWLINE);
		}
		for (String testError : Errors) {
			s.append("*ERROR*: ").append(testError).append(NEWLINE);
		}

		for (Throwable testException: Exceptions){
			s.append("*EXCEPTION*: ").append(testException.getMessage()).append(NEWLINE);
			s.append("```").append(NEWLINE);
			s.append(ExceptionUtils.getStackTrace(testException));
			s.append("```").append(NEWLINE);
		}
	}

	private void appendValidatorException(Validator validator, StringBuilder sb, Throwable t) {
		sb.append("```");
		sb.append("---- ");
		sb.append(validator.getClass().getSimpleName());
		sb.append(" failed with an exception:").append(NEWLINE);

		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		sb.append(sw.toString());
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sb.append("```").append(NEWLINE);
	}

	private void appendValidatorInfo(Validator validator, StringBuilder sb) {
		sb.append("```");
		sb.append(NEWLINE).append("---- ");
		sb.append(validator.getClass().getSimpleName());
		if (!validator.isValid()) {
			sb.append(" FAILED");
		} else {
			sb.append(" SUCCEEDED");
		}
		sb.append(" validation");
		sb.append(" ----").append(NEWLINE);

		// If there are many warnings in the test validation,
		// errors are not displayed in slack post.
		// So, errors are displayed before warnings.

		appendValidatorMessages(validator, sb, "INFO");
		appendValidatorMessages(validator, sb, "ERROR");
		appendValidatorMessages(validator, sb, "WARNING");

		sb.append("```").append(NEWLINE);
	}

	private void appendValidatorMessages(Validator validator, StringBuilder sb, String type) {
		List<String> msgs = null;
		type = type.toUpperCase();
		switch (type) {
			case "INFO":
				msgs = validator.getInfoMessages();
				break;
			case "WARNING":
				msgs = validator.getWarningMessages();
				break;
			case "ERROR":
				msgs = validator.getErrorMessages();
				break;
			default:
				return;
		}

		if (msgs.size() == 0) {
			return;
		}

		sb.append("<<" + type + ">>");
		sb.append(NEWLINE);

		for (String msg : msgs) {
			sb.append(msg);
			sb.append(NEWLINE);
		}
	}
}


