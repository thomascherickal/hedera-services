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

import com.swirlds.common.PlatformLogMarker;
import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static com.swirlds.common.PlatformLogMessages.FINISHED_RECONNECT;
import static com.swirlds.common.PlatformLogMessages.RECV_STATE_ERROR;
import static com.swirlds.common.PlatformLogMessages.RECV_STATE_HASH_MISMATCH;
import static com.swirlds.common.PlatformLogMessages.START_RECONNECT;
import static com.swirlds.common.PlatformStatNames.CREATION_TO_CONSENSUS_SEC;
import static com.swirlds.common.PlatformStatNames.FREE_MEMORY;
import static com.swirlds.common.PlatformStatNames.TOTAL_MEMORY_USED;
import static com.swirlds.common.PlatformStatNames.TRANSACTIONS_HANDLED_PER_SECOND;
import static com.swirlds.regression.RegressionUtilities.EVENT_MATCH_MSG;
import static com.swirlds.regression.RegressionUtilities.OLD_EVENT_PARENT;
import static com.swirlds.regression.RegressionUtilities.PTD_LOG_FINISHED_MESSAGES;

public class RecoverStateValidator extends NodeValidator {

	public RecoverStateValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	private final static String STATE_SAVED_MSG = "Last recovered signed state has been saved in state recover mode";
	boolean isValidated = false;
	boolean isValid = true;

	/**
	 * An passing test case should have following TWO TEST_SUCCESS key messages in order
	 *
	 * TEST_SUCCESS
	 *
	 * STATE_SAVED_MSG
	 *
	 * TEST_SUCCESS
	 *
	 * @throws IOException
	 */
	@Override
	public void validate() throws IOException {
		int nodeNum = nodeData.size();

		for (int i = 0; i < nodeNum; i++) {
			LogReader nodeLog = nodeData.get(i).getLogReader();

			LogEntry end = nodeLog.nextEntryContaining(PTD_LOG_FINISHED_MESSAGES);
			if (end == null) {
				addError("Node " + i + " did not finish first run!");
				isValid = false;
				continue; //check next node
			}

			end = nodeLog.nextEntryContaining(STATE_SAVED_MSG);
			if (end == null) {
				addError("Node " + i + " did not save recover state as expected !");
				isValid = false;
				continue; //check next node
			}

			end = nodeLog.nextEntryContaining(EVENT_MATCH_MSG);
			if (end == null) {
				addError("Node " + i + " recovered event file does not match original ones !");
				isValid = false;
				continue; //check next node
			}

			end = nodeLog.nextEntryContaining(PTD_LOG_FINISHED_MESSAGES);
			if (end == null) {
				addError("Node " + i + " did not resume run!");
				isValid = false;
				continue; //check next node
			}
		}

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
