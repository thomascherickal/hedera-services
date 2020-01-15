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

import com.swirlds.regression.GitInfo;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.SlackConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.validators.DummyValidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static com.swirlds.regression.slack.SlackNotifier.createSlackNotifier;

public class SlackNotifierTester {
    private static final String BASE_CURL_STRING = "curl -F file=@%s -F \"initial_comment=%s - Stats graph\" -F \"as_user=False\" -F \"channels=%s\" -H \"Authorization: Bearer %s\" https://slack.com/api/files.upload";
    private static final String SLACK_TOKEN = "xoxp-344480056389-344925970834-610132896599-fb69be9200db37ce0b0d55a852b2a5dc";
    private static final String SLACK_BOT_TOKEN = "xoxb-344480056389-723753217792-D5RXu4lKOPt3mDFyLTqtSHKo";
    private static final String SLACK_CHANNEL = "UA5K2LZ1D";
    private static final String SLACK_FILE_TO_UPLOAD = "./regression/multipage_pdf.pdf";
    private static final String SLACK_EXPERIMENT_NAME = "SlackUnitTestForFileUpload";

    private static final String SLACK_TEST_FILE_LOCATION = "logs/PTD-FCM1K-success/";
    private static final String INSIGHT_FILE_LOCATION = "regression/insight.py";

    private static final String BASE_PYTHON_STRING = "%s %s -p -d%s -g -cPlatformTesting -N";
    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) throws IOException {

        SlackNotifier sn = createSlackNotifier(
                SLACK_TOKEN,
                SLACK_CHANNEL);

        //testNoExperiment(sn);
        testAllFeatures(sn);
        //testFailedExperiment(sn);
        //testSendFile(sn);
    }

    private static void testNoExperiment(SlackNotifier sn) {
        SlackTestMsg msg = new SlackTestMsg(
                getRegConfig());
        msg.addError("No test found");
        sn.messageChannel(msg);
    }

    private static void testFailedExperiment(SlackNotifier sn) {
        SlackTestMsg msg = new SlackTestMsg(
                getRegConfig(),
                getTestConfig()
        );
        msg.addError("An error has occurred while running the test");

        sn.messageChannel(msg);
    }

    private static void testAllFeatures(SlackNotifier sn) {
        GitInfo gi = new GitInfo();
        gi.gitVersionInfo();
        SlackTestMsg msg = new SlackTestMsg(
                getRegConfig(),
                getTestConfig(),
                "20200109-0017-regression-TestMultiRegionIssues",
                gi
        );

        msg.addWarning("A test warning");
        msg.addError("A test error");

        DummyValidator v = new DummyValidator();
        v.addInfo("some info");
        msg.addValidatorInfo(v);

        v = new DummyValidator();
        v.addWarning("a warning");
        msg.addValidatorInfo(v);

        v = new DummyValidator();
        for (int i = 0; i < 20; i++) {
            v.addError("error " + i);
        }
        v.setValid(false);
        msg.addValidatorInfo(v);

        v = new DummyValidator();
        msg.addValidatorException(v, new Exception("an exception"));

//		System.out.println("--- start");
//		System.out.println(msg.getPlainText());
//		System.out.println("--- end");
        sn.messageChannel(msg);

        runInsightScript();

        slackFileUpload();
    }

    private static void runExecCommand(String command) {
        try {
            Process slackFile;
            BufferedReader br;
            String s;
            System.out.println(command);
            slackFile = Runtime.getRuntime().exec(command);
            br = new BufferedReader(new InputStreamReader(slackFile.getInputStream()));
            while ((s = br.readLine()) != null) {
                //    System.out.println("line: " + s);
            }
            slackFile.waitFor();
            System.out.println("exit: " + slackFile.exitValue());
            slackFile.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void slackFileUpload() {
            String uploadFileToSlackCmd = String.format(BASE_CURL_STRING, SLACK_FILE_TO_UPLOAD, SLACK_EXPERIMENT_NAME, SLACK_CHANNEL, SLACK_BOT_TOKEN);
            runExecCommand(uploadFileToSlackCmd);
    }

    private static void runInsightScript() {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            String testFilePath = classloader.getResource(SLACK_TEST_FILE_LOCATION).getPath().replaceFirst("/", "").replace("/", "\\");
            String insightFilePath = new File(INSIGHT_FILE_LOCATION).getAbsolutePath();
            String pythonExecutable = RegressionUtilities.getPythonExecutable();
            String pythonCmd = String.format(BASE_PYTHON_STRING, pythonExecutable, insightFilePath, testFilePath);
            runExecCommand(pythonCmd);
    }

    private static RegressionConfig getRegConfig() {
        RegressionConfig reg = new RegressionConfig();
        reg.setName("Reg config name");
        SlackConfig slackConfig = new SlackConfig();
        slackConfig.setNotifyUserIds(Arrays.asList("UA5K2LZ1D"));
        reg.setSlack(slackConfig);
        return reg;
    }

    private static TestConfig getTestConfig() {
        TestConfig test = new TestConfig();
        test.setName("Test name");
        test.setDuration(60);
        return test;
    }

    private static File loadSlackPdfAttachment(String filePath) throws URISyntaxException, IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URI slackAttachmentLocation = classloader.getResource(filePath).toURI();

        File slackAttachment = new File(slackAttachmentLocation);
        return slackAttachment;
    }
}
