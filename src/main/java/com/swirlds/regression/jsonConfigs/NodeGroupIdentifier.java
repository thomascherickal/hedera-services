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

public enum NodeGroupIdentifier {
	ALL,
	FIRST,
	LAST,
	ALL_BUT_LAST,
	ALL_BUT_FIRST;

	public boolean isNodeInGroup(int nodeIndex, int total) {
		switch (this) {
			case ALL:
				return true;
			case FIRST:
				return nodeIndex == 0;
			case LAST:
				return nodeIndex == total - 1;
			case ALL_BUT_LAST:
				return nodeIndex < total - 1;
			case ALL_BUT_FIRST:
				return nodeIndex > 0;
			default:
				throw new UnsupportedOperationException("Group not implemented: " + this);
		}
	}
}
