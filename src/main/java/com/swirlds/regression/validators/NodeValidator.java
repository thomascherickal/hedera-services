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

package com.swirlds.regression.validators;

import com.swirlds.regression.csv.CsvReader;

import java.util.LinkedList;
import java.util.List;

import static com.swirlds.common.PlatformStatNames.CONSENSUS_QUEUE_SIZE;
import static com.swirlds.common.PlatformStatNames.CREATION_TO_CONSENSUS_SEC;
import static com.swirlds.common.PlatformStatNames.DISK_SPACE_FREE;
import static com.swirlds.common.PlatformStatNames.FREE_MEMORY;
import static com.swirlds.common.PlatformStatNames.MAXIMUM_MEMORY;
import static com.swirlds.common.PlatformStatNames.SIGNED_STATE_HASHING_TIME;
import static com.swirlds.common.PlatformStatNames.ROUNDS_PER_SEC;
import static com.swirlds.common.PlatformStatNames.TOTAL_MEMORY_USED;
import static com.swirlds.regression.RegressionUtilities.MB;

public abstract class NodeValidator extends Validator {
	private static final double C2C_VARIATION_LIMIT = 4.0;
	private static final double Q2_SIZE_LIMIT = 450;
	private static final double MEMFREE_SIZE_MIN = 52_428_800;
	private static final double DISKFREE_SIZE_MIN = 52_428_800;
	private static final double MEMTOT_MAX_PERCENT_OF_MEMMAX = 0.95;

	List<NodeData> nodeData;

	public NodeValidator(List<NodeData> nodeData) {
		this.nodeData = nodeData;
	}

	public double getMaxCsvValue(String csvStat) {
		Double max = null;
		for (int i = 0; i < nodeData.size(); i++) {
			CsvReader nodeCsv = nodeData.get(i).getCsvReader();
			double value = nodeCsv.getColumn(csvStat).getMax();
			if (max == null) {
				max = value;
			} else {
				max = Math.max(max, value);
			}
		}
		return max != null ? max : Double.NaN;
	}

	public double getAvgCsvValue(String csvStat) {
		double sum = 0;
		for (int i = 0; i < nodeData.size(); i++) {
			CsvReader nodeCsv = nodeData.get(i).getCsvReader();
			double value = nodeCsv.getColumn(csvStat).getAverage();
			sum += value;
		}
		return sum / nodeData.size();
	}



	public void checkC2CVariation() {
		checkAllNodes((nodeData, nodeId) -> {
			CsvReader nodeCsv = nodeData.getCsvReader();
			double min = nodeCsv.getColumn(CREATION_TO_CONSENSUS_SEC).getMinNot0();
			double max = nodeCsv.getColumn(CREATION_TO_CONSENSUS_SEC).getMax();
			double var = max / min;

			if (var > C2C_VARIATION_LIMIT) {
				return String.format(
						"[Node:%d min:%.3f max:%.3f var:%.3f]",
						nodeId, min, max, var
				);
			} else {
				return null;
			}
		}, "C2C variation is too high!");
	}

	public void checkConsensusQueue() {
		checkAllNodes((nodeData, nodeId) -> {
			CsvReader nodeCsv = nodeData.getCsvReader();
			double max = nodeCsv.getColumn(CONSENSUS_QUEUE_SIZE).getMax();

			if (max > Q2_SIZE_LIMIT) {
				return String.format(
						"[Node:%d maxSize:%.3f]",
						nodeId, max
				);
			} else {
				return null;
			}
		}, "Consensus queue size is too high!");
	}

	public void checkStateHashingTime() {
		checkAllNodes((nodeData, nodeId) -> {
			CsvReader nodeCsv = nodeData.getCsvReader();
			double maxRoundsSec = nodeCsv.getColumn(ROUNDS_PER_SEC).getMax();
			double maxHash = nodeCsv.getColumn(SIGNED_STATE_HASHING_TIME).getMax();

			if (1 / maxRoundsSec < maxHash) {
				return String.format(
						"[Node:%d maxRounds/sec:%.3f maxStateHash:%.3fsec]",
						nodeId, maxRoundsSec, maxHash
				);
			} else {
				return null;
			}
		}, "State hashing is too slow!");
	}

	public void checkDiskspaceFree() {
		checkAllNodes((nodeData, nodeId) -> {
			CsvReader nodeCsv = nodeData.getCsvReader();
			double minMemFree = nodeCsv.getColumn(DISK_SPACE_FREE).getMinNot0();
			if (minMemFree < DISKFREE_SIZE_MIN) {
				return String.format("[Node: %d mindiskspaceFree:%.3fMB]", nodeId, minMemFree / MB);
			} else {
				return null;
			}

		}, "diskspace was too low!");
	}

	public void checkMemFree() {
		checkAllNodes((nodeData, nodeId) -> {
			CsvReader nodeCsv = nodeData.getCsvReader();
			double minMemFree = nodeCsv.getColumn(FREE_MEMORY).getMinNot0();
			if (minMemFree < MEMFREE_SIZE_MIN) {
				return String.format("[Node: %d minMemFree:%.3fMB]", nodeId, minMemFree / MB);
			} else {
				return null;
			}

		}, "memFree was too low!");
	}

	public void checkTotalMemory() {
		checkAllNodes((nodeData, nodeId) -> {
			CsvReader nodeCsv = nodeData.getCsvReader();
			double maxMemoryUsed = nodeCsv.getColumn(TOTAL_MEMORY_USED).getMax();
			double totalAvailableMemory = nodeCsv.getColumn(MAXIMUM_MEMORY).getLastEntryAsDouble();
			double maxAllowMemoryUsed = totalAvailableMemory * MEMTOT_MAX_PERCENT_OF_MEMMAX;

			if (maxMemoryUsed > maxAllowMemoryUsed) {
				return String.format(
						"[Node: %d maxMemoryUsed: %.3f MB exceeded target of: %.3f MB]", nodeId, maxMemoryUsed / MB,
						maxAllowMemoryUsed / MB);
			} else {
				return null;
			}
		}, "Memory Used was too close to total memory!");
	}

	private void checkAllNodes(NodeStatsCheck check, String warnMsg) {
		List<String> nodeMsgs = new LinkedList<>();
		for (int i = 0; i < nodeData.size(); i++) {
			String msg = check.checkNodeStats(nodeData.get(i), i);

			if (msg != null) {
				nodeMsgs.add(msg);
			}
		}
		if (nodeMsgs.size() > 0) {
			addWarning(
					warnMsg + " " + String.join(" ", nodeMsgs)
			);
		}
	}

	private static interface NodeStatsCheck {
		String checkNodeStats(NodeData nodeData, int nodeId);
	}
}


