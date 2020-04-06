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

package com.swirlds.regression.jsonConfigs.runTypeConfigs;

import com.swirlds.regression.jsonConfigs.AppConfig;
import com.swirlds.regression.jsonConfigs.FileRequirement;

import java.util.List;

public class FreezeConfig implements FileRequirement {
	// regression test would sleep for (EXPERIMENT_START_DELAY + freezeTiming) mins before checking if
	// the node has entered Maintenance status;
	// if all nodes has entered Maintenance status, regression test would stop all swirlds;
	// else would log an error and stop the test;
	private int freezeTiming = 0;
	private int retries = 2;
	private int freezeIterations = 1;
	private AppConfig postFreezeApp;

	public int getFreezeTiming() {
		return freezeTiming;
	}

	public void setFreezeTiming(int freezeTiming) {
		this.freezeTiming = freezeTiming;
	}

	public int getFreezeIterations() {
		return freezeIterations;
	}

	public void setFreezeIterations(int freezeIterations) {
		this.freezeIterations = freezeIterations;
	}

	public AppConfig getPostFreezeApp() {
		return postFreezeApp;
	}

	public void setPostFreezeApp(AppConfig postFreezeApp) {
		this.postFreezeApp = postFreezeApp;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	@Override
	public List<String> getFilesNeeded() {
		return postFreezeApp.getFilesNeeded();
	}
}
