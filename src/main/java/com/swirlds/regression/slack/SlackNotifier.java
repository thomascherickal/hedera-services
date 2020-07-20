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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.hubspot.algebra.Result;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.methods.params.chat.ChatGetPermalinkParams;
import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.models.response.SlackError;
import com.hubspot.slack.client.models.response.chat.ChatGetPermalinkResponse;
import com.hubspot.slack.client.models.response.chat.ChatPostMessageResponse;
import com.swirlds.regression.ExecStreamReader;
import com.swirlds.regression.RegressionUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Arrays;

public class SlackNotifier {
	private static final Logger log = LogManager.getLogger(SlackNotifier.class);
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	public static final String CURL_FILE_OPTION = "file=@%s";
	public static final String CURL_INITIAL_COMMENT_OPTION = "initial_comment=%s-Stats graph";
	public static final String CURL_AS_USER_FALSE = "as_user=False";
	public static final String CURL_CHANNELS = "channels=%s";
	public static final String CURL_AUTHORIZATION_TOKEN = "Authorization: Bearer %s";
	public static final String CURL_FILE_UPLOAD_ADDRESS = "https://slack.com/api/files.upload";
	public static final String CURL_DASH_F = "-F";
	public static final String CURL_DASH_H = "-H";

	private final SlackClient slackClient;

	/**
	 * Do not use this, this is for Guice
	 */
	@Inject
	public SlackNotifier(SlackClient slackClient) {
		this.slackClient = slackClient;
	}

	public static SlackNotifier createSlackNotifier(String token) {
		SlackNotifier sn = Guice.createInjector(
				new SlackModule(token))
				.getInstance(SlackNotifier.class);
		return sn;
	}

	public ChatPostMessageResponse messageChannel(String message, boolean notifyChannel, String channel) {
		Result<ChatPostMessageResponse, SlackError> postResult = slackClient.postMessage(
				ChatPostMessageParams.builder()
						.setText(notifyChannel ? "<!here> " + message : message)
						.setChannelId(channel)
						.build()
		).join();

		return postResult.unwrapOrElseThrow(); // release failure here as a RTE
	}

	public ChatPostMessageResponse messageChannel(String message, String channel) {
		return messageChannel(message, false, channel);
	}

	public ChatPostMessageResponse messageChannel(SlackMsg message, String channel) {
		try {
			Result<ChatPostMessageResponse, SlackError> postResult = slackClient.postMessage(
					message.build(channel)
			).join();

			return postResult.unwrapOrElseThrow();
		} catch (Throwable t) {
			// slack sometimes throws an exception but still sends the message
			// the exceptions have been inconsistent and don't affect the outcome,
			// this is why we are only logging the message, not the full stack trace,
			// to have less useless noise in the log
			log.error(ERROR, "Slack threw an exception: {}", t.getMessage());
			return null;
		}
	}

	public String getLinkTo(ChatPostMessageResponse response) {
		try {
			String channel = response.getChannel();
			String ts = response.getTs();

			Result<ChatGetPermalinkResponse, SlackError> result = slackClient.getPermalink(
					ChatGetPermalinkParams.builder()
							.setChannelId(channel)
							.setMessageTs(ts)
							.build())
					.join();
			return result.unwrapOrElseThrow().getPermalink();
		} catch (Throwable t) {
			log.error(ERROR, "Slack threw an exception: {}", t.getMessage());
			return null;
		}
	}

	public void uploadFile(SlackTestMsg message, String fileLocation, String experimentName) {
		/* If unix based change file to be universally accessible so curl can open it. */
		if (!RegressionUtilities.isWindows()) {
			ExecStreamReader.outputProcessStreams(new String[] { "chmod", "777", fileLocation });
		}

		/* build the curl command and execute it to upload graph files to slack */
		String[] uploadFileToSlackCmd = buildCurlString(message, fileLocation, experimentName);
		ExecStreamReader.outputProcessStreams(uploadFileToSlackCmd);
	}

	public static String[] buildCurlString(SlackTestMsg message, String fileLocation, String experimentName) {
		// TODO Why is this using CURL? Shouldn't we be using the slack API?
		// TODO eventually there should be a SlackMsg subtype that builds this message
		String fileOption = String.format(CURL_FILE_OPTION, fileLocation);
		String commentOption = String.format(CURL_INITIAL_COMMENT_OPTION,
				experimentName + "\n" + message.getUniqueId());
		String channelOption = String.format(CURL_CHANNELS, message.slackConfig.getChannel());
		String botAuthToken = String.format(CURL_AUTHORIZATION_TOKEN, message.slackConfig.getBotToken());

		String[] uploadFileToSlackCmd = new String[] { "curl", CURL_DASH_F, fileOption, CURL_DASH_F, commentOption,
				CURL_DASH_F, CURL_AS_USER_FALSE, CURL_DASH_F, channelOption, CURL_DASH_H, botAuthToken,
				CURL_FILE_UPLOAD_ADDRESS };
		log.trace(MARKER, "Curl Array: {}", Arrays.toString(uploadFileToSlackCmd));
		return uploadFileToSlackCmd;
	}
}
