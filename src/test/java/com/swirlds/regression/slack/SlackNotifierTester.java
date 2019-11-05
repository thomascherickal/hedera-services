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

import java.io.IOException;
import java.util.Arrays;

import static com.swirlds.regression.slack.SlackNotifier.createSlackNotifier;

public class SlackNotifierTester {
	public static void main(String[] args) throws IOException {
		String slackToken = "insert token here";
		String slackChannel = "regression-test";

		SlackNotifier sn = createSlackNotifier(
				slackToken,
				slackChannel);

		testNoExperiment(sn);
		//testAllFeatures(sn);
		//testFailedExperiment(sn);
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
				"folder-name",
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
	}

	private static RegressionConfig getRegConfig() {
		RegressionConfig reg = new RegressionConfig();
		reg.setName("Reg config name");
		SlackConfig slackConfig = new SlackConfig();
		slackConfig.setNotifyUserIds(Arrays.asList("UA4T7UJQJ"));
		reg.setSlack(slackConfig);
		return reg;
	}

	private static TestConfig getTestConfig() {
		TestConfig test = new TestConfig();
		test.setName("Test name");
		test.setDuration(60);
		return test;
	}
}
