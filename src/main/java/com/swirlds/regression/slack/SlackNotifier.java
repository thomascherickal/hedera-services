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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.hubspot.algebra.Result;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.models.response.SlackError;
import com.hubspot.slack.client.models.response.chat.ChatPostMessageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class SlackNotifier {
    private static final Logger log = LogManager.getLogger(SlackNotifier.class);
    private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

    private static final String BASE_CURL_STRING = "curl -F \"file=@%s\" -F \"initial_comment=%s-Stats graph\" -F \"as_user=False\" -F \"channels=%s\" -H \"Authorization: Bearer %s\" https://slack.com/api/files.upload";

    private final SlackClient slackClient;
    private String channel;

    /**
     * Do not use this, this is for Guice
     */
    @Inject
    public SlackNotifier(SlackClient slackClient) {
        this.slackClient = slackClient;
    }

    public static SlackNotifier createSlackNotifier(String token, String channel) {
        SlackNotifier sn = Guice.createInjector(
                new SlackModule(token))
                .getInstance(SlackNotifier.class);
        sn.setChannel(channel);
        return sn;
    }

    public ChatPostMessageResponse messageChannel(String message, boolean notifyChannel) {
        Result<ChatPostMessageResponse, SlackError> postResult = slackClient.postMessage(
                ChatPostMessageParams.builder()
                        .setText(notifyChannel ? "<!here> " + message : message)
                        .setChannelId(channel)
                        .build()
        ).join();

        return postResult.unwrapOrElseThrow(); // release failure here as a RTE
    }

    public ChatPostMessageResponse messageChannel(String message) {
        return messageChannel(message, false);
    }

    public ChatPostMessageResponse messageChannel(SlackTestMsg message) {
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

    public void uploadFile(SlackTestMsg message, String fileLocation, String experimentName){
        String processResponseString;
        Process slackFile;
        try{
            //String uploadFileToSlackCmd = String.format(BASE_CURL_STRING,fileLocation,experimentName,message.slackConfig.getChannel(), message.slackConfig.getBotToken());
            //System.out.println(uploadFileToSlackCmd);
            String fOption = "-F";
            String hOption = "-H";
            String fileOption = String.format("-F \"file=@%s\"",fileLocation);
            String commentOption = String.format("-F \"initial_comment=%s-Stats graph\"",experimentName);
            String userOption = String.format("-F \"as_user=False\"");
            String channelOption = String.format("-F \"channels=%s\"",message.slackConfig.getChannel());
            String authOption = String.format(" -H \"Authorization: Bearer %s\"", message.slackConfig.getBotToken());
            String slackOption = String.format("https://slack.com/api/files.upload");
            String [] uploadFileToSlackCmd = new String [] {"curl", fOption, fileOption, fOption, commentOption, fOption, userOption, fOption, channelOption, hOption, authOption, slackOption};
//            String uploadFileToSlackCmd = String.format(BASE_CURL_STRING,fileLocation,experimentName,message.slackConfig.getChannel(), message.slackConfig.getBotToken());
            System.out.println(Arrays.toString(uploadFileToSlackCmd));

            slackFile = Runtime.getRuntime().exec(uploadFileToSlackCmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(slackFile.getErrorStream()));
            while((processResponseString = br.readLine()) != null){
                System.out.println("line: " + processResponseString);
            }
            slackFile.waitFor();
            System.out.println("exit: " + slackFile.exitValue());
            slackFile.destroy();
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    private void setChannel(String channel) {
        this.channel = channel;
    }
}
