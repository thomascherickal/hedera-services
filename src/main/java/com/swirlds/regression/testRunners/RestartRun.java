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

package com.swirlds.regression.testRunners;

import com.swirlds.regression.Experiment;
import com.swirlds.regression.jsonConfigs.TestConfig;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.swirlds.regression.RegressionUtilities.MILLIS;

public class RestartRun implements TestRun {

	@Override
	public void preRun(TestConfig testConfig, Experiment experiment) {
		// decide the freeze time
		ZonedDateTime experimentTime = ZonedDateTime.now(ZoneOffset.ofHours(0))
				.plusMinutes(testConfig.getRestartConfig().getRestartTiming());
		ZonedDateTime freezeTime = experimentTime.plusMinutes(
				testConfig.getExperimentConfig().getExperimentRestartDelay());

		// set the freeze time in the settings
		experiment.getSettingsFile().setFreezeTime(
				experimentTime.getHour(),
				experimentTime.getMinute(),
				freezeTime.getHour(),
				freezeTime.getMinute()
		);
	}

	@Override
	public void runTest(TestConfig testConfig, Experiment experiment) {

		final int experimentStartDelay = testConfig.getExperimentConfig().getExperimentStartDelay();
		// run the first part
		// if first part fails, stop the test
		if (!FreezeRun.runSingleFreeze(experiment,
				testConfig.getRestartConfig().getRestartTiming(), experimentStartDelay, 0, false)) {
			return;
		}

		log.info(MARKER, "First part of restart test completed.");

		// wait a bit during freeze
		experiment.sleepThroughExperiment(testConfig.getExperimentConfig().getFreezeWaitMillis());

		// start all processes again
		experiment.startAllSwirlds();
		long testDuration = testConfig.getDuration() * MILLIS;
		log.info(MARKER, "kicking off test, duration: {}", testDuration);
		experiment.sleepThroughExperiment(testDuration);
		log.info(MARKER, "Second part of {} test completed", "restart");
	}
}
