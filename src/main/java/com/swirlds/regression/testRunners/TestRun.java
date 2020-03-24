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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public interface TestRun {
	static final Logger log = LogManager.getLogger(TestRun.class);
	static final Marker MARKER = MarkerManager.getMarker("TEST_RUN");

	static final int EXPERIMENT_RESTART_DELAY = 5;
	static final int EXPERIMENT_START_DELAY = 2;
	static final int FREEZE_WAIT_MILLIS = 30000;

	default void preRun(TestConfig testConfig, Experiment experiment) {
	}

	void runTest(TestConfig testConfig, Experiment experiment);

	default void postRun(TestConfig testConfig, Experiment experiment) {
	}
}
