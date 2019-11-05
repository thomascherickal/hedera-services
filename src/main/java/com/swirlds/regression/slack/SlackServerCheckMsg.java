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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.hubspot.algebra.Result;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.models.Attachment;
import com.hubspot.slack.client.models.response.SlackError;
import com.hubspot.slack.client.models.response.chat.ChatPostMessageResponse;
import com.swirlds.regression.AWSServerCheck;

import java.util.ArrayList;

public class SlackServerCheckMsg {

	private String channel;
	private SlackClient slackClient;

	private ArrayList<StringBuilder> regionInstanceInformation;


	/**
	 * Do not use this, this is for Guice
	 */
	@Inject
	public SlackServerCheckMsg(SlackClient slackClient) {
		this.slackClient = slackClient;
	}

	public static SlackServerCheckMsg createSlackServerChk(String token, String channel) {
		SlackServerCheckMsg sn = Guice.createInjector(
				new SlackModule(token))
				.getInstance(SlackServerCheckMsg.class);
		sn.setChannel(channel);
		return sn;
	}

	private void setChannel(String chan) {
		this.channel = chan;
	}

	public String getPlainText() {
		return null;
	}

	ChatPostMessageParams build() {
		ChatPostMessageParams.Builder msg = ChatPostMessageParams.builder();
		msg.setChannelId(channel);

		for (StringBuilder attachText : regionInstanceInformation) {
			Attachment.Builder attach = Attachment.builder();
			attach.setText(attachText.toString());
			int indexofRegionTotal = attachText.indexOf("$") + 1;
			int endIndexOfRegionTotal = attachText.indexOf("*\n", indexofRegionTotal);
			float regionTotal = Float.parseFloat(attachText.substring(indexofRegionTotal, endIndexOfRegionTotal));
			// if [there is an exception] OR [test not valid] OR [test has errors]
			if (regionTotal <= AWSServerCheck.SAFE_SPENDING_AMOUNT) {
				attach.setColor("#00FF00");
			} else if (regionTotal > AWSServerCheck.DANGEROUS_SPENDING_AMOUNT) {
				attach.setColor("#FF0000");
			} else {
				attach.setColor("#FFFF00");
			}
			msg.addAttachments(attach.build());
		}
		return msg.build();
	}


	public ChatPostMessageResponse messageChannel() {

		Result<ChatPostMessageResponse, SlackError> postResult = slackClient.postMessage(
				build()
		).join();
		return postResult.unwrapOrElseThrow();
	}

	public void SetRegionInstanceInformation(ArrayList<StringBuilder> newRegionInstanceInformation) {
		this.regionInstanceInformation = newRegionInstanceInformation;
	}

	public void appendRegionInformation(StringBuilder newRegionInformation) {
		if (this.regionInstanceInformation == null) {
			this.regionInstanceInformation = new ArrayList<>();
		}
		this.regionInstanceInformation.add(newRegionInformation);
	}
}


