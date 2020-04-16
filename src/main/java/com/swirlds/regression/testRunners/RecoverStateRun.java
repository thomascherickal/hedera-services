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

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static com.swirlds.regression.RegressionUtilities.MILLIS;
import static com.swirlds.regression.RegressionUtilities.PTD_LOG_SUCCESS_OR_FAIL_MESSAGES;
import static com.swirlds.regression.RegressionUtilities.REMOTE_SWIRLDS_LOG;

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

		// sleep through the rest of the test
		List<BooleanSupplier> checkerList = new LinkedList<>();
		checkerList.add(experiment::isProcessFinished);
		experiment.sleepThroughExperimentWithCheckerList(testDuration,
				checkerList);

		String oldEventsLogDir = settingsBuilder.getSettingValue("eventsLogDir");
		String originalStreamFileDir = oldEventsLogDir + "/*/";

		/**************************
		 Stage 2 recover run
		 **************************/
		// delete old states
		if (!experiment.randomDeleteLastNSignedStates()) {
			return;
		}

		// enable recover mode
		settingsBuilder.addSetting("enableStateRecovery", "true");
		settingsBuilder.addSetting("playbackStreamFileDirectory", oldEventsLogDir);

		// save event to different directory so later we can compare event file created during
		// recover mode with event files created by the original run
		settingsBuilder.addSetting("eventsLogDir", testConfig.getRecoverConfig().getEventDir());

		experiment.sendSettingFileToNodes();
		experiment.sendConfigToNodes();

		// unzip database backup file and restore
		experiment.recoverDatabase();

		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		checkerList.clear();
		checkerList.add(experiment::isFoundStateRecoverDoneMessage);
		experiment.sleepThroughExperimentWithCheckerList(testDuration,
				checkerList);


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

		checkerList.clear();
		checkerList.add(experiment::isFoundTwoPTDFinishMessage);
		checkerList.add(experiment::isAnyNodeFoundFallBehindMessage);
		experiment.sleepThroughExperimentWithCheckerList(testDuration,
				checkerList);
	}

	boolean compareStateVSDatabase(Experiment experiment, long testDuration, List<BooleanSupplier> checkerList) {
		// delete last states
		experiment.deleteLastNSignedStates(1);

		// unzip database backup file and restore
		experiment.recoverDatabase();

		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		experiment.sleepThroughExperimentWithCheckerList(testDuration,
				checkerList);

		return true;
	}

	void checkStateVSDatabase(Experiment experiment, List<BooleanSupplier> checkerList) {

		SettingsBuilder settingsBuilder = experiment.getSettingsFile();
		settingsBuilder.addSetting("saveStatePeriod", "12000"); //avoid saving new signed state
		experiment.sendSettingFileToNodes();
		experiment.sendConfigToNodes();


		int number = experiment.getNumberOfSignedStates();
		for (int i = 0; i < number-1; i++) {
			compareStateVSDatabase(experiment, 61_000L, checkerList);
		}
	}
}
