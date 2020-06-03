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

import java.util.Arrays;

public class MemoryLeakCheckConfig {

	/**
	 * an array of nodeIds we want to check GC logs
	 */
	private int[] nodesToCheck;

	/**
	 * specifies which group of nodes we want to check GC logs;
	 * it would not be used if {@link MemoryLeakCheckConfig#nodesToCheck} is provided
	 */
	private NodeGroupIdentifier nodeGroupIdentifier;

	public int[] getNodesToCheck() {
		return nodesToCheck;
	}

	public void setNodesToCheck(int[] nodesToCheck) {
		this.nodesToCheck = nodesToCheck;
	}

	public NodeGroupIdentifier getNodeGroupIdentifier() {
		return nodeGroupIdentifier;
	}

	public void setNodeGroupIdentifier(NodeGroupIdentifier nodeGroupIdentifier) {
		this.nodeGroupIdentifier = nodeGroupIdentifier;
	}

	/**
	 * examine whether the nodeId is in {@link MemoryLeakCheckConfig#nodesToCheck}, or {@link MemoryLeakCheckConfig#nodeGroupIdentifier}
	 * @param nodeId
	 * @return whether we need to check GC log of this node
	 */
	public boolean shouldCheck(final int nodeId, final int totalNum, final int lastStakedNode) {
		if (nodesToCheck != null || nodesToCheck.length != 0) {
			return Arrays.stream(nodesToCheck).anyMatch(id -> id == nodeId);
		} else if (nodeGroupIdentifier != null){
			return nodeGroupIdentifier.isNodeInGroup(nodeId, totalNum, lastStakedNode);
		}
		return false;
	}
}
