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

import com.hubspot.slack.client.models.response.chat.ChatPostMessageResponse;
import com.swirlds.regression.ExecStreamReader;
import com.swirlds.regression.GitInfo;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.experiment.ExperimentSummaryData;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.SlackConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.validators.DummyValidator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static com.swirlds.regression.slack.SlackNotifier.createSlackNotifier;

public class SlackNotifierTester {
	private static final String SLACK_TOKEN = "xoxp-344480056389-344925970834-610132896599" +
            "-fb69be9200db37ce0b0d55a852b2a5dc";
	private static final String SLACK_BOT_TOKEN = "xoxb-344480056389-723753217792-D5RXu4lKOPt3mDFyLTqtSHKo";
	private static final String SLACK_CHANNEL = "regression-test";
	private static final String SLACK_FILE_TO_UPLOAD = "./regression/multipage_pdf.pdf";
	private static final String SLACK_EXPERIMENT_NAME = "SlackUnitTestForFileUpload";

	private static final String SLACK_TEST_FILE_LOCATION = "logs/PTD-FCM1K-success/";
	private static final String INSIGHT_FILE_LOCATION = "regression/insight.py";
	private static final String SLACK_REGRESSION_NAME = "Slack Regression Unit Test";

	public static void main(String[] args) throws IOException {

		SlackNotifier sn = createSlackNotifier(
				SLACK_TOKEN);

		//testNoExperiment(sn);
		//testAllFeatures(sn);
		testSummaryMsg(sn);
		testAttachmentSplittingSummaryMsg(sn);
		//testFailedExperiment(sn);
		//testSendFile(sn);
	}

	private static void testAttachmentSplittingSummaryMsg(SlackNotifier sn) {
		SlackTestMsg msg = getTestMsg();
		msg.addError("an error to link to");
		ChatPostMessageResponse response = sn.messageChannel(msg, SLACK_CHANNEL);
		String testLink = sn.getLinkTo(response);

		RegressionConfig regConfig = getRegConfig();
		GitInfo gi = new GitInfo();
		gi.gitVersionInfo();
		SlackSummaryMsg summaryMsg = new SlackSummaryMsg( regConfig.getSlack(),	regConfig,	gi,"some-folder");

		ExperimentSummaryData bad01 = new ExperimentSummaryData(true,true,true,"bad test 001","1916540190", testLink);
		ExperimentSummaryData bad02 = new ExperimentSummaryData(false,false,true,"bad test 002","12345111" );
		ExperimentSummaryData bad03 = new ExperimentSummaryData(false,false,true,"bad test 003","12345112" );
		ExperimentSummaryData bad04 = new ExperimentSummaryData(false,false,true,"bad test 004","12345113" );
		ExperimentSummaryData bad05 = new ExperimentSummaryData(false,false,true,"bad test 005","12345114" );
		ExperimentSummaryData bad06 = new ExperimentSummaryData(false,false,true,"bad test 006","12345115" );
		ExperimentSummaryData bad07 = new ExperimentSummaryData(false,false,true,"bad test 007","12345116" );
		ExperimentSummaryData bad08 = new ExperimentSummaryData(false,false,true,"bad test 008","12345117" );
		ExperimentSummaryData bad09 = new ExperimentSummaryData(false,false,true,"bad test 009","12345118" );
		ExperimentSummaryData bad10 = new ExperimentSummaryData(false,false,true,"bad test 010","12345119" );
		ExperimentSummaryData bad11 = new ExperimentSummaryData(false,false,true,"bad test 011","12345120" );
		ExperimentSummaryData bad12 = new ExperimentSummaryData(false,false,true,"bad test 012","12345121" );
		ExperimentSummaryData bad13 = new ExperimentSummaryData(false,false,true,"bad test 013","12345122" );
		ExperimentSummaryData bad14 = new ExperimentSummaryData(false,false,true,"bad test 014","12345123" );
		ExperimentSummaryData bad15 = new ExperimentSummaryData(false,false,true,"bad test 015","12345124" );

		ExperimentSummaryData warn01 = new ExperimentSummaryData(true,false,false,"a warning test 001","123456201");
		ExperimentSummaryData warn02 = new ExperimentSummaryData(true,false,false,"a warning test 003","123456202");
		ExperimentSummaryData warn03 = new ExperimentSummaryData(true,false,false,"a warning test 003","123456203");
		ExperimentSummaryData warn04 = new ExperimentSummaryData(true,false,false,"a warning test 004","123456204");
		ExperimentSummaryData warn05 = new ExperimentSummaryData(true,false,false,"a warning test 005","123456205");
		ExperimentSummaryData warn06 = new ExperimentSummaryData(true,false,false,"a warning test 006","123456206");
		ExperimentSummaryData warn07 = new ExperimentSummaryData(true,false,false,"a warning test 007","123456207");
		ExperimentSummaryData warn08 = new ExperimentSummaryData(true,false,false,"a warning test 008","123456208");
		ExperimentSummaryData warn09 = new ExperimentSummaryData(true,false,false,"a warning test 009","123456209");
		ExperimentSummaryData warn10 = new ExperimentSummaryData(true,false,false,"a warning test 010","123456210");
		ExperimentSummaryData warn11 = new ExperimentSummaryData(true,false,false,"a warning test 011","123456211");
		ExperimentSummaryData warn12 = new ExperimentSummaryData(true,false,false,"a warning test 012","123456212");
		ExperimentSummaryData warn13 = new ExperimentSummaryData(true,false,false,"a warning test 013","123456213");
		ExperimentSummaryData warn14 = new ExperimentSummaryData(true,false,false,"a warning test 014","123456214");
		ExperimentSummaryData warn15 = new ExperimentSummaryData(true,false,false,"a warning test 015","123456215");


		ExperimentSummaryData good01 = new ExperimentSummaryData(false,false,false,	"passing test 001","1234567301");
		ExperimentSummaryData good02 = new ExperimentSummaryData(false,false,false,	"passing test 002","1234567302");
		ExperimentSummaryData good03 = new ExperimentSummaryData(false,false,false,	"passing test 003","1234567303");
		ExperimentSummaryData good04 = new ExperimentSummaryData(false,false,false,	"passing test 004","1234567304");
		ExperimentSummaryData good05 = new ExperimentSummaryData(false,false,false,	"passing test 005","1234567305");
		ExperimentSummaryData good06 = new ExperimentSummaryData(false,false,false,	"passing test 006","1234567306");
		ExperimentSummaryData good07 = new ExperimentSummaryData(false,false,false,	"passing test 007","1234567307");
		ExperimentSummaryData good08 = new ExperimentSummaryData(false,false,false,	"passing test 008","1234567308");
		ExperimentSummaryData good09 = new ExperimentSummaryData(false,false,false,	"passing test 009","1234567309");
		ExperimentSummaryData good10 = new ExperimentSummaryData(false,false,false,	"passing test 010","1234567310");
		ExperimentSummaryData good11 = new ExperimentSummaryData(false,false,false,	"passing test 011","1234567311");
		ExperimentSummaryData good12 = new ExperimentSummaryData(false,false,false,	"passing test 012","1234567312");
		ExperimentSummaryData good13 = new ExperimentSummaryData(false,false,false,	"passing test 013","1234567313");
		ExperimentSummaryData good14 = new ExperimentSummaryData(false,false,false,	"passing test 014","1234567314");
		ExperimentSummaryData good15 = new ExperimentSummaryData(false,false,false,	"passing test 015","1234567315");


		summaryMsg.addExperiment(bad01, List.of(good01, good02, bad11, warn04, bad05,good06,bad07, warn08, good09,good10));
		summaryMsg.addExperiment(bad02, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(warn01, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));

		summaryMsg.addExperiment(good01, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good02, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good03, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good04, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good05, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good06, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good07, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good08, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good09, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good10, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good11, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good12, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good13, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good14, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));
		summaryMsg.addExperiment(good15, List.of(good01, good02, good03,good04,good05,good06,good07,good08,good09,good10));

		summaryMsg.addExperiment(warn02, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn03, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn04, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn05, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn06, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn07, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn08, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn09, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn10, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn11, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn12, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn13, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn14, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));
		summaryMsg.addExperiment(warn15, List.of(warn01, warn02, warn03,warn04,warn05,warn06,warn07,warn08,warn09,warn10));

		summaryMsg.addExperiment(bad03, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad04, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad05, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad06, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad07, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad08, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad09, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad10, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad11, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad12, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad13, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad14, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));
		summaryMsg.addExperiment(bad15, List.of(good01, good02, bad11, bad04, bad05,bad06,bad07,bad08,bad09,bad10));



		sn.messageChannel(summaryMsg, SLACK_CHANNEL);
	}

	private static void testSummaryMsg(SlackNotifier sn) {
		SlackTestMsg msg = getTestMsg();
		msg.addError("an error to link to");
		ChatPostMessageResponse response = sn.messageChannel(msg, SLACK_CHANNEL);
		String testLink = sn.getLinkTo(response);

		RegressionConfig regConfig = getRegConfig();
		GitInfo gi = new GitInfo();
		gi.gitVersionInfo();
		SlackSummaryMsg summaryMsg = new SlackSummaryMsg(
				regConfig.getSlack(),
				regConfig,
				gi,
				"some-folder"
		);

		ExperimentSummaryData bad1 = new ExperimentSummaryData(
				true,
				true,
				true,
				"FCM-2.5MC-5MU-5MT-250KD-2.5KTPS",
				"1916540190",
				testLink
		);
		ExperimentSummaryData bad2 = new ExperimentSummaryData(
				false,
				false,
				true,
				"another bad test",
				"12345"
		);
        ExperimentSummaryData warn = new ExperimentSummaryData(
                true,
                false,
                false,
                "a warning test",
                "123456"
        );
        ExperimentSummaryData good = new ExperimentSummaryData(
                false,
                false,
				false,
				"passed",
				"1234567"
		);

		summaryMsg.addExperiment(bad1, List.of(good, good, bad1));
		summaryMsg.addExperiment(bad2, List.of(good, good, bad1));
		summaryMsg.addExperiment(warn, List.of(warn, warn, warn));
		summaryMsg.addExperiment(good, List.of(good, good, good));

		sn.messageChannel(summaryMsg, SLACK_CHANNEL);
	}

	private static SlackTestMsg getTestMsg(){
		GitInfo gi = new GitInfo();
		gi.gitVersionInfo();
		return new SlackTestMsg(
				null,
				getRegConfig(),
				getTestConfig(),
				SLACK_TEST_FILE_LOCATION,
				gi
		);
	}

	private static void testAllFeatures(SlackNotifier sn) {
		SlackTestMsg msg = getTestMsg();

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
		sn.messageChannel(msg, SLACK_CHANNEL);

		runInsightScript();

		slackFileUpload();
	}

	private static void runExecCommand(String command) {
		ExecStreamReader.outputProcessStreams(command.split(" "));
	}

	private static void slackFileUpload() {
		String[] uploadFileToSlackCmd = SlackNotifier.buildCurlString(
				new SlackTestMsg(null, getRegConfig(), getTestConfig()),
				SLACK_FILE_TO_UPLOAD, SLACK_EXPERIMENT_NAME);
		ExecStreamReader.outputProcessStreams(uploadFileToSlackCmd);
	}

	private static void runInsightScript() {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String testFilePath = classloader.getResource(SLACK_TEST_FILE_LOCATION).getPath().replaceFirst("/", "").replace(
				"/", "\\");
		String insightFilePath = new File(INSIGHT_FILE_LOCATION).getAbsolutePath();
		String pythonExecutable = RegressionUtilities.getPythonExecutable();
		String pythonCmd = String.format(RegressionUtilities.INSIGHT_CMD, pythonExecutable, insightFilePath,
				testFilePath);
		runExecCommand(pythonCmd);
	}

	private static RegressionConfig getRegConfig() {
		RegressionConfig reg = new RegressionConfig();
		reg.setName(SLACK_REGRESSION_NAME);
		SlackConfig slackConfig = new SlackConfig();
		slackConfig.setNotifyUserIds(Arrays.asList("UA5K2LZ1D"));
		slackConfig.setBotToken(SLACK_BOT_TOKEN);
		slackConfig.setToken(SLACK_TOKEN);
		slackConfig.setChannel(SLACK_CHANNEL);
		reg.setSlack(slackConfig);
		return reg;
	}

	private static TestConfig getTestConfig() {
		TestConfig test = new TestConfig();
		test.setName(SLACK_EXPERIMENT_NAME);
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
