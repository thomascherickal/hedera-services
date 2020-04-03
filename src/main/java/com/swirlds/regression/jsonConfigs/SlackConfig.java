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

package com.swirlds.regression.jsonConfigs;

import com.swirlds.regression.slack.SlackTestMsg;

import java.util.List;

public class SlackConfig {
	private String token;
	private String botToken;
	private String channel;
	private String summaryChannel;
	private SlackTestMsg.NotifyOn notifyOn = SlackTestMsg.NotifyOn.ERROR;
	private boolean notifyChannel = false;
	private List<String> notifyUserIds = null;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getBotToken() {
		return botToken;
	}

	public void setBotToken(String botToken) {
		this.botToken = botToken;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getSummaryChannel() {
		return summaryChannel;
	}

	public void setSummaryChannel(String summaryChannel) {
		this.summaryChannel = summaryChannel;
	}

	public SlackTestMsg.NotifyOn getNotifyOn() {
		return notifyOn;
	}

	public void setNotifyOn(SlackTestMsg.NotifyOn notifyOn) {
		this.notifyOn = notifyOn;
	}

	public boolean isNotifyChannel() {
		return notifyChannel;
	}

	public void setNotifyChannel(boolean notifyChannel) {
		this.notifyChannel = notifyChannel;
	}

	public List<String> getNotifyUserIds() {
		return notifyUserIds;
	}

	public void setNotifyUserIds(List<String> notifyUserIds) {
		this.notifyUserIds = notifyUserIds;
	}
}
