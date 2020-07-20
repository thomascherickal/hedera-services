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

package com.swirlds.regression;

public class SavedStatePathInfo {
	private String fullPath;
	private long roundNumber;
	private long nodeId;
	private String swirldsName;
	private String mainClassName;

	public SavedStatePathInfo(String fullPath, long roundNumber, long nodeId, String swirldsName,
			String mainClassName) {
		this.fullPath = fullPath;
		this.roundNumber = roundNumber;
		this.nodeId = nodeId;
		this.swirldsName = swirldsName;
		this.mainClassName = mainClassName;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public long getRoundNumber() {
		return roundNumber;
	}

	public void setRoundNumber(long roundNumber) {
		this.roundNumber = roundNumber;
	}

	public long getNodeId() {
		return nodeId;
	}

	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	public String getSwirldsName() {
		return swirldsName;
	}

	public void setSwirldsName(String swirldsName) {
		this.swirldsName = swirldsName;
	}

	public String getMainClassName() {
		return mainClassName;
	}

	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}

	@Override
	public String toString() {
		return "SavedStatePathInfo{" +
				"fullPath='" + fullPath + '\'' +
				", roundNumber=" + roundNumber +
				", nodeId=" + nodeId +
				", swirldsName='" + swirldsName + '\'' +
				", mainClassName='" + mainClassName + '\'' +
				'}';
	}
}
