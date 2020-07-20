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

import java.time.Duration;

import static com.swirlds.regression.RegressionUtilities.MILLIS;

public class ReconnectRun implements TestRun {
	@Override
	public void runTest(TestConfig testConfig, Experiment experiment) {
		// start all processes
		experiment.startAllSwirlds();

		Duration sleep = Duration.ofSeconds(
				testConfig.getExperimentConfig().getExperimentStartDelay() + testConfig.getReconnectConfig().getReconnectTiming());
		experiment.sleepThroughExperiment(sleep.toMillis());

		// stop the last node process
		experiment.stopLastSwirlds();
		log.info(MARKER, "last node killed for reconnect test waiting {} milliseconds",
				testConfig.getReconnectConfig().getReconnectTiming() * MILLIS);

		experiment.sleepThroughExperiment(testConfig.getReconnectConfig().getReconnectTiming() * MILLIS);

		log.info(MARKER, "Last node being restarted for reconnect test allowing {} milliseconds to reconnect",
				testConfig.getReconnectConfig().getReconnectTiming() * MILLIS);
		experiment.startLastSwirlds();

		log.info(MARKER, "Test will continue to run {} milliseconds after reconnect",
				testConfig.getDuration() * MILLIS);
		experiment.sleepThroughExperiment(testConfig.getDuration() * MILLIS);
		log.info(MARKER, "Reconnect test completed");
	}
}
