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

public class RegionList {
	private String region;
	private int numberOfNodes = -1;
	private int numberOfTestClientNodes = -1;
	private String[] instanceList = null;
	private String[] testClientInstanceList = null;
	boolean isValid = false;

	boolean isValidConfig() {
		if (isValid) {
			return isValid;
		}
		if (this.region == null || this.region.isEmpty() || this.numberOfNodes < 0) {
			return isValid;
		}
		isValid = true;
		return isValid;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public void setNumberOfNodes(int numberOfNodes) {
		this.numberOfNodes = numberOfNodes;
	}

	public String[] getInstanceList() {
		return instanceList;
	}

	public void setInstanceList(String[] instanceList) {
		this.instanceList = instanceList;
		if (this.numberOfNodes < instanceList.length) {
			this.numberOfNodes = instanceList.length;
		}
	}

	public int getNumberOfTestClientNodes() {
		return numberOfTestClientNodes;
	}

	public void setNumberOfTestClientNodes(int numberOfTestClientNodes) {
		this.numberOfTestClientNodes = numberOfTestClientNodes;
	}

	public String[] getTestClientInstanceList() {
		return testClientInstanceList;
	}

	public void setTestClientInstanceList(String[] testClientInstanceList) {
		this.testClientInstanceList = testClientInstanceList;
		if (this.numberOfTestClientNodes < testClientInstanceList.length) {
			this.numberOfTestClientNodes = testClientInstanceList.length;
		}
	}
}
