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
import com.swirlds.regression.jsonConfigs.TestConfig;

import java.time.Duration;

import static com.swirlds.regression.RegressionUtilities.MILLIS;

public class FreezeRun implements TestRun {
	@Override
	public void runTest(TestConfig testConfig, Experiment experiment) {
		int iterations = testConfig.getFreezeConfig().getFreezeIterations();

		for (int i = 0; i < iterations; i++) {
			// start all processes
			experiment.startAllSwirlds();

			Duration sleep =
					Duration.ofMinutes(EXPERIMENT_START_DELAY + testConfig.getFreezeConfig().getFreezeTiming());
			experiment.sleepThroughExperiment(sleep.toMillis());

			// check if all nodes has entered Maintenance status at ith iteration
			// if not, log an error and stop the test
			if (!experiment.checkAllNodesFreeze(i)) {
				log.error(ERROR, "Dynamic freeze test failed at {}th iteration. " +
								"Not all nodes entered Maintenance status after {} mins",
						i, testConfig.getFreezeConfig().getFreezeTiming());
				return;
			}

			// check if all nodes finish saving expectedMap if they already started
			if (!experiment.checkAllNodesSavedExpected(i)) {
				// wait for a while
				experiment.sleepThroughExperiment(
						Duration.ofMinutes(SAVE_EXPECTED_WAIT_MINS).toMillis());
				// if any node hasn't finished yet, log an error and stop the test
				if (!experiment.checkAllNodesSavedExpected(i)) {
					log.error(ERROR, "Dynamic freeze test failed at {}th iteration. " +
									"Not all nodes have finished saving ExpectedMap after {} mins",
							i, testConfig.getFreezeConfig().getFreezeTiming() +
									SAVE_EXPECTED_WAIT_MINS);
					return;
				}
			}

			// kill the process during the freeze
			experiment.stopAllSwirlds();

			log.info(MARKER, "{} dynamic freeze test completed.", (i + 1));

			// wait a bit during freeze
			experiment.sleepThroughExperiment(FREEZE_WAIT_MILLIS);
		}

		log.info(MARKER, "Last dynamic freeze test finished. Starting for final time.");

		// set the non-freeze config for the last run
		experiment.getTestConfig().setApp(
				testConfig.getFreezeConfig().getPostFreezeApp()
		);
		// upload the configs
		experiment.sendConfigToNodes();

		// start all processes
		experiment.startAllSwirlds();

		// sleep through the rest of the test
		long testDuration = testConfig.getDuration() * MILLIS;
		log.info(MARKER, "kicking off test, duration: {}", testDuration);
		experiment.sleepThroughExperiment(testDuration);
	}
}
