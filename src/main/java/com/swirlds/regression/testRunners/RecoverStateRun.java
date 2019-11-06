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

package com.swirlds.regression.testRunners;

import com.swirlds.regression.Experiment;
import com.swirlds.regression.SettingsBuilder;
import com.swirlds.regression.jsonConfigs.TestConfig;

import java.util.ArrayList;

import static com.swirlds.regression.RegressionUtilities.MILLIS;

/**
 * Recover signed state from event stream file
 */
public class RecoverStateRun implements TestRun {
	private ArrayList<String> oldParams;

	@Override
	public void preRun(TestConfig testConfig, Experiment experiment) {

	}

	@Override
	public void runTest(TestConfig testConfig, Experiment experiment) {
		SettingsBuilder settingsBuilder = experiment.getSettingsFile();

		/**************************
		 Stage 1 normal run
		 **************************/
		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		long testDuration = testConfig.getDuration() * MILLIS;
		experiment.sleepThroughExperiment(testDuration);

		/**************************
		 Stage 2 recover run
		 **************************/
		settingsBuilder.addSetting("enableStateRecovery", "true");

		// save more states on disk so we can test with deleting more old signed state
		settingsBuilder.addSetting("signedStateDisk", "100");

		// delete old states
		experiment.deleteSignedState();

		// change config.txt PlatformTestingDemo.jar, TEST_PAUSE_NOCHECK.json
		oldParams = testConfig.getApp().getParameterList();
		String newParams = testConfig.getRecoverConfig().getJson();
		experiment.getTestConfig().getApp().setParameterList(new ArrayList<>() {{
			add(newParams);
		}});

		// enable recover mode
		settingsBuilder.addSetting("enableStateRecovery", "true");
		settingsBuilder.addSetting("playbackStreamFileDirectory", "data/eventStream ");

		// save event to different directory
		settingsBuilder.addSetting("eventsLogDir", "data/eventStreamRecover");

		experiment.sendSettingFileToNodes();
		experiment.sendConfigToNodes();

		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		experiment.sleepThroughExperiment(testDuration);

		experiment.displaySignedState("AFTER recover");

		// cannot compare recovered event files with original event files since
		// recovered event files do not have any system transactions

		/**************************
		 Stage 3 resume run
		 **************************/
		// change config.txt 'PlatformTestingDemo.jar, TEST_STATE_RECOVER.json '
		experiment.getTestConfig().getApp().setParameterList(oldParams);

		settingsBuilder.addSetting("enableStateRecovery", "false");

		// save event to different directory
		settingsBuilder.addSetting("eventsLogDir", "data/eventStreamResume");

		experiment.sendSettingFileToNodes();
		experiment.sendConfigToNodes();

		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		experiment.sleepThroughExperiment(testDuration);
	}

	@Override
	public void postRun(TestConfig testConfig, Experiment experiment) {

	}
}
