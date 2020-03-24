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

package com.swirlds.regression.jsonConfigs;

public class ExperimentConfig {
	private int experimentRestartDelay = 5;
	private int experimentStartDelay = 2;
	private int freezeWaitMillis = 30000;

	/**
	 * experimentRestartDelay is the amount of time in minutes that the experiment freezes before it restarts
	 * That is, it freezes from start=now()+RestartConfig.getRestartTiming() to now()+start+experimentRestartDelay
	 *
	 * @return int
	 * 		experimentRestartDelay
	 */
	public int getExperimentRestartDelay() {
		return experimentRestartDelay;
	}

	/**
	 * experimentStartDelay is used to compute the sleep time by adding to RestartConfig.getRestartTiming()
	 *
	 * @return int
	 * 		experimentStartDelay
	 */
	public int getExperimentStartDelay() {
		return experimentStartDelay;
	}

	/**
	 * An amount of time to sleep after stopping the platforms during the freeze period and before restarting
	 * the platforms
	 *
	 * @return int
	 * 		freezeWaitMillis
	 */
	public int getFreezeWaitMillis() {
		return freezeWaitMillis;
	}
}
