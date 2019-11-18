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
	private final static String RECOVER_EVENT_DIR = "data/eventStreamRecover/*/";

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
		String originalStreamFileDir = settingsBuilder.getSettingValue("eventsLogDir") + "/*/";

		/**************************
		 Stage 2 recover run
		 **************************/
		// delete old states
		experiment.randomDeleteLastNSignedStates();

		// enable recover mode
		settingsBuilder.addSetting("enableStateRecovery", "true");
		settingsBuilder.addSetting("playbackStreamFileDirectory", "data/eventStream ");

		// save event to different directory so later we can compare event file created during
		// recover mode with event files created by the original run
		String oldEventsLogDir = settingsBuilder.getSettingValue("eventsLogDir");
		settingsBuilder.addSetting("eventsLogDir", "data/eventStreamRecover");

		experiment.sendSettingFileToNodes();
		experiment.sendConfigToNodes();

		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		experiment.sleepThroughExperiment(testDuration);

		experiment.displaySignedStates("AFTER recover");

		experiment.checkRecoveredEventFiles(RECOVER_EVENT_DIR, originalStreamFileDir);

		/**************************
		 Stage 3 resume run
		 **************************/
		settingsBuilder.addSetting("enableStateRecovery", "false");

		// restore event to original directory
		settingsBuilder.addSetting("eventsLogDir", oldEventsLogDir);

		experiment.sendSettingFileToNodes();
		experiment.sendConfigToNodes();

		// start all processes
		experiment.startAllSwirlds();

		// sleep until JAVA process dies, or second test finish message has been found
		experiment.sleepUntilEnoughFinishMessage(testDuration, 2);
	}

}
