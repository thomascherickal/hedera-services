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

import java.util.List;

/**
 * Configuration to run hedera-services regression
 */
public class HederaServicesConfig {
	private boolean performanceRun = false;
	private boolean ciPropsMap = false;
	private List<String> testSuites;
	private int numOfSuiteRunnerProcesses = 1;
	private boolean fixedNode = false;

	public List<String> getTestSuites() {
		return testSuites;
	}

	public boolean hasCiPropsMap() {
		return ciPropsMap;
	}

	public void setCiPropsMap(boolean ciPropsMap) {
		this.ciPropsMap = ciPropsMap;
	}

	public void setTestSuites(List<String> testSuites) {
		this.testSuites = testSuites;
	}


	public int getNumOfSuiteRunnerProcesses() {
		return numOfSuiteRunnerProcesses;
	}

	public void setNumOfSuiteRunnerProcesses(int numOfSuiteRunnerProcesses) {
		this.numOfSuiteRunnerProcesses = numOfSuiteRunnerProcesses;
	}

	public boolean isFixedNode() {
		return fixedNode;
	}

	public void setFixedNode(boolean fixedNode) {
		this.fixedNode = fixedNode;
	}

	public boolean isPerformanceRun() {
		return performanceRun;
	}

	public void setPerformanceRun(boolean performanceRun) {
		this.performanceRun = performanceRun;
	}
}
