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

import com.swirlds.common.logging.LogMarkerInfo;
import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.swirlds.common.PlatformStatNames.CREATION_TO_CONSENSUS_SEC;
import static com.swirlds.common.PlatformStatNames.FREE_MEMORY;
import static com.swirlds.common.PlatformStatNames.TOTAL_MEMORY_USED;
import static com.swirlds.common.PlatformStatNames.TRANSACTIONS_HANDLED_PER_SECOND;
import static com.swirlds.regression.RegressionUtilities.PTD_LOG_FINISHED_MESSAGES;

public class PtdValidator extends NodeValidator {
	private boolean isValidated = false;
	private boolean isValid = false;

	public PtdValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	@Override
	public void validate() throws IOException {
		int nodeNum = nodeData.size();

		Instant startTime = null;
		Instant endTime = null;
		int numFinished = 0;
		int numProblems = 0;
		int socketExceptions = 0;
		String exceptionString = "";
		double transHandleAverage = 0; //transH/sec
		double maxC2C = -1;
		double maxTotalMem = -1;
		double minFreeMem = Double.MAX_VALUE;
		for (int i = 0; i < nodeNum; i++) {
			LogReader nodeLog = nodeData.get(i).getLogReader();
			CsvReader nodeCsv = nodeData.get(i).getCsvReader();

			Instant nodeStart = nodeLog.nextEntry().getTime();
			if (startTime == null || nodeStart.isAfter(startTime)) {
				startTime = nodeStart;
			}

			// this is the last log statement we check, we don't care about any exceptions after it
			LogEntry end = nodeLog.nextEntryContaining(PTD_LOG_FINISHED_MESSAGES);
			if (end == null) {
				addError("Node " + i + " did not finish!");
				/* collect stats even if node did not finish */
				//continue;
			} else {
				addInfo("Node" + i + " Finished");
				numFinished++;
				if (endTime == null || end.getTime().isAfter(endTime)) {
					endTime = end.getTime();
				}
			}

			if (nodeLog.getExceptionCount() > 0) {
				for (LogEntry le : nodeLog.getExceptions()) {
					if (le.getMarker() == LogMarkerInfo.SOCKET_EXCEPTIONS) {
						socketExceptions++;
					} else {
						numProblems++;
						exceptionString += le.getLogEntry() + "\\r";
					}
				}
			}

			transHandleAverage += nodeCsv.getColumn(TRANSACTIONS_HANDLED_PER_SECOND).getAverage();
			maxC2C = Math.max(maxC2C, nodeCsv.getColumn(CREATION_TO_CONSENSUS_SEC).getMax());

			minFreeMem = Math.min(minFreeMem, nodeCsv.getColumn(FREE_MEMORY).getMinNot0());
			maxTotalMem = Math.max(maxTotalMem, nodeCsv.getColumn(TOTAL_MEMORY_USED).getMax());

		}
		transHandleAverage /= nodeNum;

		if (numFinished == nodeNum && numProblems == 0) {
			isValid = true;
		}

		if (numFinished != nodeNum) {
			addError("Not all nodes finished!");
			isValid = false; // report error if not all node have "TEST SUCCESS"
		}
		if (numProblems > 0) {
			addError("Test had " + numProblems + " exceptions!");
		}

		if (startTime != null && endTime != null) {
			Duration time = Duration.between(startTime, endTime);
			addInfo("Test took " + time.toMinutes() + " minutes " + time.toSecondsPart() + " seconds");
		}

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
