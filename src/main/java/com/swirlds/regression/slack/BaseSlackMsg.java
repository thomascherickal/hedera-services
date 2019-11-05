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
import com.swirlds.regression.jsonConfigs.SlackConfig;

import java.util.LinkedList;
import java.util.List;

abstract public class BaseSlackMsg {
	static final char NEWLINE = '\n';
	SlackConfig slackConfig;

	boolean hasWarnings = false;
	boolean hasErrors = false;
	boolean hasExceptions = false;

	List<String> Errors = new LinkedList<>();
	List<String> Warnings = new LinkedList<>();
	List<Throwable> Exceptions = new LinkedList<>();

	public BaseSlackMsg(SlackConfig slackConfig) {
		this.slackConfig = slackConfig;
	}

	StringBuilder bold(StringBuilder s, String text) {
		return s.append('*').append(text).append('*');
	}

	public enum NotifyOn {
		ALL,
		WARNING,
		ERROR
	}

	public void setSlackConfig(SlackConfig slackConfig) {
		this.slackConfig = slackConfig;
	}

	abstract ChatPostMessageParams build(String channel);
	abstract public String getPlainText();

	public void addError(String error) {
		Errors.add(error);
	}

	public void addExceptions(Throwable exception) {
		Exceptions.add(exception);
	}

	public void addWarning(String warning) {
		Warnings.add(warning);
	}

	void checkForAnomolies(){
		if (Warnings.size() > 0) {
			hasWarnings = true;
		}
		if (Errors.size() > 0) {
			hasErrors = true;
		}
		if (Exceptions.size() > 0){
			hasExceptions = true;
		}
	}

	void addNotify(StringBuilder s, boolean hasWarnings, boolean hasErrors, boolean hasExceptions) {
		// SlackConfig slackConfig = regConfig.getSlack();
		if (slackConfig == null) {
			return;
		}

		// check the notifyOn to see if we need notifications
		switch (slackConfig.getNotifyOn()) {
			case ERROR:
				if (!hasErrors && !hasExceptions) {
					return;
				}
			case WARNING:
				if (!hasErrors && !hasExceptions && !hasWarnings) {
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
