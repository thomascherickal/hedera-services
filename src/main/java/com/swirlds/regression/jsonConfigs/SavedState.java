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

package com.swirlds.regression.jsonConfigs;

public class SavedState {
	private String location;
	private String mainClass;
	private String swirldName;
	private Long round;
	private boolean restoreDb = false;
	private NodeGroupIdentifier nodeIdentifier = NodeGroupIdentifier.ALL;

	public FileLocationType getLocationType() {
		if (location.startsWith("s3")) {
			return FileLocationType.AWS_S3;
		} else {
			return FileLocationType.LOCAL;
		}
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getSwirldName() {
		return swirldName;
	}

	public void setSwirldName(String swirldName) {
		this.swirldName = swirldName;
	}

	public Long getRound() {
		return round;
	}

	public void setRound(Long round) {
		this.round = round;
	}

	public boolean isRestoreDb() {
		return restoreDb;
	}

	public void setRestoreDb(boolean restoreDb) {
		this.restoreDb = restoreDb;
	}

	public NodeGroupIdentifier getNodeIdentifier() {
		return nodeIdentifier;
	}

	public void setNodeIdentifier(NodeGroupIdentifier nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}
}
