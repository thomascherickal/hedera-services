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

import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.PTD_LOG_FINISHED_MESSAGES;
import static com.swirlds.regression.RegressionUtilities.STATE_SAVED_MSG;

public class RecoverStateValidator extends NodeValidator {


	public final static String EVENT_MATCH_LOG_NAME = "recover_event_match.log";
	public RecoverStateValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	boolean isValidated = false;
	boolean isValid = true;

	/**
	 * An passing test case should have following key messages in order
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

			end = nodeLog.nextEntryContaining(PTD_LOG_FINISHED_MESSAGES);
			if (end == null) {
				addError("Node " + i + " did not resume run!");
				isValid = false;
				continue; //check next node
			}

			addInfo("Node " + i + " finished recover run and resume normally as expected");
		}

		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}

}
