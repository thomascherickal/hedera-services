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

import java.util.Set;

/**
 * When MemoryLeakCheckConfig is provided in TestConfig, GC logs of all nodes would be
 * generated and downloaded;
 * GC logs of certain nodes which are specified in this config would be sent to GCEasy API
 * for analyzing
 */
public class MemoryLeakCheckConfig {

	/**
	 * an set of nodeIds we want to check GC logs
	 */
	private Set<Integer> nodesToCheck;

	/**
	 * specifies which group of nodes we want to check GC logs;
	 * it would not be used if {@link MemoryLeakCheckConfig#nodesToCheck} is provided
	 */
	private NodeGroupIdentifier nodeGroupIdentifier;

	public Set<Integer> getNodesToCheck() {
		return nodesToCheck;
	}

	public void setNodesToCheck(Set<Integer> nodesToCheck) {
		this.nodesToCheck = nodesToCheck;
	}

	public NodeGroupIdentifier getNodeGroupIdentifier() {
		return nodeGroupIdentifier;
	}

	public void setNodeGroupIdentifier(NodeGroupIdentifier nodeGroupIdentifier) {
		this.nodeGroupIdentifier = nodeGroupIdentifier;
	}

	/**
	 * examine whether the nodeId is in {@link MemoryLeakCheckConfig#nodesToCheck}, or {@link
	 * MemoryLeakCheckConfig#nodeGroupIdentifier}
	 *
	 * @param nodeId
	 * @return whether we need to check GC log of this node
	 */
	public boolean shouldCheck(final int nodeId, final int totalNum, final int lastStakedNode) {
		if (nodesToCheck != null && !nodesToCheck.isEmpty()) {
			return nodesToCheck.contains(nodeId);
		} else if (nodeGroupIdentifier != null) {
			return nodeGroupIdentifier.isNodeInGroup(nodeId, totalNum, lastStakedNode);
		}
		return false;
	}
}
