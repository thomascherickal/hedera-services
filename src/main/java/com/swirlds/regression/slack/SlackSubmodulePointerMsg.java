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
import com.swirlds.regression.jsonConfigs.SlackConfig;

import java.io.IOException;
import java.util.List;

import static com.swirlds.regression.slack.SlackNotifier.createSlackNotifier;

public class SlackSubmodulePointerMsg extends SlackMsg {

    private static final String SLACK_TOKEN = "xoxp-344480056389-344925970834-610132896599" +
            "-fb69be9200db37ce0b0d55a852b2a5dc";
    private static final String SLACK_BOT_TOKEN = "xoxb-344480056389-723753217792-D5RXu4lKOPt3mDFyLTqtSHKo";
    private static final String SLACK_CHANNEL = "regression-test";
    private static final String SLACK_FILE_TO_UPLOAD = "./regression/multipage_pdf.pdf";
    private static final String SLACK_EXPERIMENT_NAME = "SlackUnitTestForFileUpload";

    private static final String SLACK_TEST_FILE_LOCATION = "logs/PTD-FCM1K-success/";
    private static final String INSIGHT_FILE_LOCATION = "regression/insight.py";
    private static final String SLACK_REGRESSION_NAME = "Slack Regression Unit Test";

    public SlackSubmodulePointerMsg(SlackConfig slackConfig) {
        super(slackConfig);
    }

    public static void main(String[] args) throws IOException {

        SlackNotifier sn = createSlackNotifier(
                SLACK_TOKEN);
        sn.messageChannel(args[0], "UA5K2LZ1D");
    }
    @Override
    public List<Attachment> generateSlackMessage(StringBuilder stringBuilder) {
        return null;
    }

    @Override
    public String getPlaintext() {
        return null;
    }
}
