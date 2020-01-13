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
    public static void main(String[] args) throws IOException {
        String slackToken = "xoxp-344480056389-344925970834-610132896599-fb69be9200db37ce0b0d55a852b2a5dc";
        String slackChannel = "UA5K2LZ1D";

        SlackNotifier sn = createSlackNotifier(
                slackToken,
                slackChannel);

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

        String s;
        Process slackFile;
        try {
            slackFile = Runtime.getRuntime().exec("pwd");
            BufferedReader br = new BufferedReader(new InputStreamReader(slackFile.getInputStream()));
            while ((s = br.readLine()) != null) {
                System.out.println("line: " + s);
            }
            slackFile.waitFor();
            System.out.println("exit: " + slackFile.exitValue());
            slackFile.destroy();
            slackFile = Runtime.getRuntime().exec("python3 ./regression/insight.py -p -dresults/20200109-0017-regression-TestMultiRegionIssues -g -cPlatformTesting");
            br = new BufferedReader(new InputStreamReader(slackFile.getErrorStream()));
            while ((s = br.readLine()) != null) {
                System.out.println("line: " + s);
            }
            slackFile.waitFor();
            System.out.println("exit: " + slackFile.exitValue());
            slackFile.destroy();

            slackFile = Runtime.getRuntime().exec("curl -F file=@./regression/multipage_pdf.pdf -F \"initial_comment=Stats graph\" -F \"channels=UA5K2LZ1D\" -H \"Authorization: Bearer xoxp-344480056389-344925970834-610132896599-fb69be9200db37ce0b0d55a852b2a5dc\" https://slack.com/api/files.upload");
            br = new BufferedReader(new InputStreamReader(slackFile.getErrorStream()));
            while ((s = br.readLine()) != null) {
                System.out.println("line: " + s);
            }
            slackFile.waitFor();
            System.out.println("exit: " + slackFile.exitValue());
            slackFile.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


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
