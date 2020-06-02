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

package com.swirlds.regression;

import com.swirlds.regression.testRunners.FreezeRun;
import com.swirlds.regression.testRunners.ReconnectRun;
import com.swirlds.regression.testRunners.RecoverStateRun;
import com.swirlds.regression.testRunners.RestartRun;
import com.swirlds.regression.testRunners.StandardRun;
import com.swirlds.regression.testRunners.TestRun;

public enum RunType {
	STANDARD(new StandardRun()),
	RESTART(new RestartRun()),
	RECONNECT(new ReconnectRun()),
	FREEZE(new FreezeRun()),
	RECOVER(new RecoverStateRun());

	private final TestRun testRun;

	RunType(TestRun testRun) {
		this.testRun = testRun;
	}

	public TestRun getTestRun() {
		return testRun;
	}
}
