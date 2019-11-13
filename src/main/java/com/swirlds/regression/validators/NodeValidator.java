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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.csv.CsvStat;
import com.swirlds.regression.jsonConfigs.TestConfig;
import org.junit.jupiter.api.TestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.swirlds.common.PlatformStatNames.CONSENSUS_QUEUE_SIZE;
import static com.swirlds.common.PlatformStatNames.CREATION_TO_CONSENSUS_SEC;
import static com.swirlds.common.PlatformStatNames.DISK_SPACE_FREE;
import static com.swirlds.common.PlatformStatNames.FREE_MEMORY;
import static com.swirlds.common.PlatformStatNames.MAXIMUM_MEMORY;
import static com.swirlds.common.PlatformStatNames.NEW_SIG_STATE_TIME;
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

	private ArrayList<Double> getAndParsePtdConfig(TestConfig testConfig, String PTDJsonConfigFilePath) {

		// Assumptions: for PTD, there is always only one parameter: the PTD JSON config file
		ArrayList<Double> throttleValues = new ArrayList<>();
		String jarName = testConfig.getApp().getJar();
		if (!jarName.equals("PlatformTestingDemo.jar")) return throttleValues;

		String jsonFileName = testConfig.getApp().getParameterList().get(0);
		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = null;
		// assumed location of PTD json config file
		//String fullPath = "../platform-apps/tests/PlatformTestingDemo/target/classes/" + jsonFileName;
		String fullPath = PTDJsonConfigFilePath + jsonFileName;

		try {
			File jsonFile = new File(fullPath);
			rootNode = mapper.readTree(jsonFile);
			boolean enableThrottle = rootNode.findValue("submitConfig").findValue("enableThrottling").asBoolean();
			if (enableThrottle) {
				JsonNode tpsMap = rootNode.findValue("submitConfig").findValue("tpsMap");
				int index = 0;
				for (JsonNode throttleOp : tpsMap) {
					throttleValues.add(index++, throttleOp.asDouble());
				}
				//System.out.println("Found " + throttleValues.size() + " throttle values");
				addInfo(String.format("Found %d throttle values", throttleValues.size()));
			}
		} catch (IOException | NullPointerException e) {
			System.out.println("Unable to read PTD json file: " + fullPath);
			addWarning("Unable to read PTD json file: " + fullPath);
		}

		return throttleValues;
	}

	public void checkThrottledTransPerSec(TestConfig testConfig, String PTDJsonConfigFilePath) {

		// Compute a moving average (MA) of all the trans/sec data using a window size
		// and tolerance to compare against the set throttle.  Whenever the MA
		// stabilizes, keep track of the MA that minimizes variation with previous MA
		// As soon as MA starts increasing or decreasing, compare the MA that minimized variation
		// against a throttle value parsed in from PTD config

		ArrayList<Double> throttleValues = getAndParsePtdConfig(testConfig, PTDJsonConfigFilePath);
		if (throttleValues.size() == 0) return;
		//double [] throttleValues = {400.0, 100.0, 300.0, 150.0};

		int throttleValuesIndex = 0;
		double sumMa = 0.0;
		int MOVING_AVERAGE_WINDOW = 5; // sumMa computed over this many values
		double TOLERANCE = 0.02; //sumMa is stable if |sumMa-sumPrevMa| < TOLERANCE*sumMa

		int numNodes = nodeData.size();
		//System.out.println("numNodes: " + numNodes);

		CsvReader nodeCsv = nodeData.get(0).getCsvReader();
		CsvStat transSec = nodeCsv.getColumn("trans/sec");

		for (int i = 0; i < MOVING_AVERAGE_WINDOW; i++)
			sumMa += transSec.getDataElement(i); // initialization

		double sumMaPrev = sumMa; // previous value of MA
		double minMaDiff = Double.MAX_VALUE; // the minimium value of the |sumMa - sumMaPrev| once sumMa is stable
		double minVarMa = sumMa; // the sumMa that results in minMaDiff

		int dir = 0;
		int dirPrev = -2;

		for (int i = MOVING_AVERAGE_WINDOW; i < transSec.dataSize(); i++) {
			// generate a moving average and stop when average stabilizes

			sumMa = sumMaPrev - transSec.getDataElement(i - MOVING_AVERAGE_WINDOW) +
					transSec.getDataElement(i);
			//System.out.print("Computed Moving average: " + sumMa / MOVING_AVERAGE_WINDOW / numNodes);
			double maDiff = Math.abs(sumMa - sumMaPrev);
			if (maDiff < TOLERANCE * sumMa) {
				dir = 0; //stable
				if (minMaDiff > maDiff) {
					minMaDiff = maDiff;
					minVarMa = sumMa;
				}
				//System.out.println(", stable.");
			} else {
				dir = 1; //increasing or decreasing
				minMaDiff = Double.MAX_VALUE;
				//System.out.println(", not stable.");
			}

			if (dir - dirPrev != 0) {
				// checking dirPrev so that we are switching from stable to increasing/decreasing
				// this allows us to have the "best" stable value of sumMa, the one that minimized the variation
				if (dirPrev == 0) {
					// check a throttle value in config versus sumMa
					if (throttleValuesIndex >= throttleValues.size()) {
						//addWarning("\tError: more stable moving averages detected than throttle values");
					} else {
						double averageTrans = minVarMa / MOVING_AVERAGE_WINDOW / numNodes;
						if (Math.abs(throttleValues.get(throttleValuesIndex) - averageTrans) > 0.1 * averageTrans) {
							addWarning(String.format("Transactions/sec did not match throttle setting: " +
											"%.3f achieved vs expected value of %.3f",
									averageTrans, throttleValues.get(throttleValuesIndex)));
						}
						addInfo(String.format("Specified throttle value %.3f vs achieved %.3f",
								throttleValues.get(throttleValuesIndex),
								minVarMa / MOVING_AVERAGE_WINDOW / numNodes));
						//System.out.println(
						//		"\tChecking throttle value " + throttleValuesIndex + ": " + throttleValues.get(
						//				throttleValuesIndex));
						//System.out.println("\tMoving average is: " + minVarMa / MOVING_AVERAGE_WINDOW / numNodes);
						throttleValuesIndex++;
					}
				}
			}
			sumMaPrev = sumMa;
			dirPrev = dir;
		}
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
			double maxHash = nodeCsv.getColumn(NEW_SIG_STATE_TIME).getMax();

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


