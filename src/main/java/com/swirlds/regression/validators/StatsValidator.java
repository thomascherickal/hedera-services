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
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.swirlds.common.PlatformStatNames.CREATION_TO_CONSENSUS_SEC;
import static com.swirlds.common.PlatformStatNames.DISK_SPACE_FREE;
import static com.swirlds.common.PlatformStatNames.DISK_SPACE_USED;
import static com.swirlds.common.PlatformStatNames.FREE_MEMORY;
import static com.swirlds.common.PlatformStatNames.TOTAL_MEMORY_USED;
import static com.swirlds.common.PlatformStatNames.TRANSACTIONS_HANDLED_PER_SECOND;
import static com.swirlds.regression.RegressionUtilities.MB;

public class StatsValidator extends NodeValidator {
	private boolean isValidated = false;
	private TestConfig testConfig;
	public String PTDJsonConfigFilePath = "../platform-apps/tests/PlatformTestingDemo/target/classes/"; // for throttle validation

	public StatsValidator(List<NodeData> nodeData, TestConfig testConfig) {

		super(nodeData);
		this.testConfig = testConfig;
	}

	@Override
	public void validate() throws IOException {

		int nodeNum = nodeData.size();

		Instant startTime = null;
		Instant endTime = null;

		double transHandleAverage = 0; //transH/sec
		double maxC2C = -1;
		double maxTotalMem = -1;
		double maxDiskSpaceUsed = -1;
		double minFreeMem = Double.MAX_VALUE;
		for (int i = 0; i < nodeNum; i++) {
			LogReader nodeLog = nodeData.get(i).getLogReader();
			CsvReader nodeCsv = nodeData.get(i).getCsvReader();

			Instant nodeStart = nodeLog.nextEntry().getTime();
			if (startTime == null || nodeStart.isAfter(startTime)) {
				startTime = nodeStart;
			}

			// this is the last log statement we check, we dont care about any exceptions after it
			nodeLog.readFully();
			LogEntry end = nodeLog.getLastEntryRead();
			if (endTime == null || end.getTime().isAfter(endTime)) {
				endTime = end.getTime();
			}

			transHandleAverage += nodeCsv.getColumn(TRANSACTIONS_HANDLED_PER_SECOND).getAverage();
			maxC2C = Math.max(maxC2C, nodeCsv.getColumn(CREATION_TO_CONSENSUS_SEC).getMax());

			minFreeMem = Math.min(minFreeMem, nodeCsv.getColumn(FREE_MEMORY).getMinNot0());
			maxTotalMem = Math.max(maxTotalMem, nodeCsv.getColumn(TOTAL_MEMORY_USED).getMax());

			maxDiskSpaceUsed = Math.max(maxDiskSpaceUsed, nodeCsv.getColumn(DISK_SPACE_USED).getMax());

		}
		transHandleAverage /= nodeNum;

		if (startTime != null && endTime != null) {
			Duration time = Duration.between(startTime, endTime);
			addInfo("Test took " + time.toMinutes() + " minutes " + time.toSecondsPart() + " seconds");
		}

		addInfo(String.format("Average number of transactions handled per second is: %.3f", transHandleAverage));
		addInfo(String.format("Max creation to consensus is: %.3f seconds", maxC2C));
		addInfo(String.format("Lowest Free Memory: %.3fMB", minFreeMem / MB));
		addInfo(String.format("Maximum Total Memory Used: %.3fMB", maxTotalMem / MB));
		addInfo(String.format("Maximum Diskspace Used: %.3fMB", maxDiskSpaceUsed / MB));

		checkC2CVariation();
		checkConsensusQueue();
		checkStateHashingTime();
		checkMemFree();
		checkTotalMemory();
		checkDiskspaceFree();
		checkThrottledTransPerSec(testConfig, PTDJsonConfigFilePath);

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated;
	}
}
