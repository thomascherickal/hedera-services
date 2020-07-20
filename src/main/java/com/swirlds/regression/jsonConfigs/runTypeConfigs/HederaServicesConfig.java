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
	/**
	 * Used to run multiple suiteRunners in one testClient. Mainly used to run UmbrellaRedux tests
	 */
	private boolean performanceRun = false;
	/**
	 * Is true if the testSuites mentioned in configuration also has CI properties
	 * after testSuite name, with space delimiter
	 */
	private boolean ciPropsMap = false;
	/**
	 * list of testSuites that should be run in one experiment
	 */
	private List<String> testSuites;
	/**
	 * number of suiteRunner processes that should be run in testClient.
	 * When performanceRun is true, this number is considered
	 */
	private int numOfSuiteRunnerProcesses = 1;
	/**
	 * If false node is fixed, else it is random
	 */
	private boolean isFixedNode = false;

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
		return isFixedNode;
	}

	public void setFixedNode(boolean fixedNode) {
		isFixedNode = fixedNode;
	}

	public boolean isPerformanceRun() {
		return performanceRun;
	}

	public void setPerformanceRun(boolean performanceRun) {
		this.performanceRun = performanceRun;
	}
}
