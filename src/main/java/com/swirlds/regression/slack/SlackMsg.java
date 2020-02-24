/*
 * (c) 2016-2018 Swirlds, Inc.
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

import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.models.Attachment;
import com.swirlds.regression.jsonConfigs.SlackConfig;

import java.util.List;

abstract public class SlackMsg {
	static final char NEWLINE = '\n';
	SlackConfig slackConfig;

	protected boolean warnings = false;
	protected boolean errors = false;
	protected boolean exceptions = false;

	public boolean hasWarnings() {
		return warnings;
	}

	public boolean hasErrors() {
		return errors;
	}

	public boolean hasExceptions() {
		return exceptions;
	}

	public SlackMsg(SlackConfig slackConfig) {
		this.slackConfig = slackConfig;
	}

	/**
	 * Utility function for adding bold slack text to a string builder.
	 */
	public static void bold(StringBuilder s, String text) {
		s.append('*').append(text).append('*');
	}

	/**
	 * Utility function for adding a code snippet to slack.
	 */
	public static void codeSnippet(StringBuilder s, String text) {
		s.append('`').append(text).append('`');
	}

	/**
	 * Add a newline character to a string builder.
	 */
	public static void newline(StringBuilder s) {
		s.append(NEWLINE);
	}

	public enum NotifyOn {
		ALL,
		WARNING,
		ERROR
	}

	public ChatPostMessageParams build(String channel) {
		ChatPostMessageParams.Builder msg = ChatPostMessageParams.builder();
		msg.setChannelId(channel);

		StringBuilder stringBuilder = new StringBuilder();

		List<Attachment> attachments = generateSlackMessage(stringBuilder);

		// Add attachments, if any
		if (attachments != null) {
			for (Attachment attachment: attachments) {
				msg.addAttachments(attachment);
			}
		}

		// Add notification if needed
		maybeNotify(stringBuilder);

		msg.setText(stringBuilder.toString());

		return msg.build();
	}

	/**
	 * Generate a slack message using a string builder.
	 * @param stringBuilder The contents of the message should be added to this string builder.
	 * @return A list of attachments. If there are no attachments it is ok to return null.
	 */
	abstract public List<Attachment> generateSlackMessage(StringBuilder stringBuilder);

	abstract public String getPlaintext();

	protected void maybeNotify(StringBuilder s) {
		// SlackConfig slackConfig = regConfig.getSlack();
		if (slackConfig == null) {
			return;
		}

		// check the notifyOn to see if we need notifications
		switch (slackConfig.getNotifyOn()) {
			case ERROR:
				if (!errors && !exceptions) {
					return;
				}
			case WARNING:
				if (!errors && !exceptions && !warnings) {
					return;
				}
		}

		if (slackConfig.isNotifyChannel() ||
				(slackConfig.getNotifyUserIds() != null && slackConfig.getNotifyUserIds().size() > 0)) {
			s.insert(0, NEWLINE);
		} else {
			return;
		}

		if (slackConfig.isNotifyChannel()) {
			s.insert(0, "<!here> ");
		}

		if (slackConfig.getNotifyUserIds() == null) {
			return;
		}

		for (String user : slackConfig.getNotifyUserIds()) {
			s.insert(0, "<@" + user + "> ");
		}
	}
}
