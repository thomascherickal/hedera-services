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

package com.swirlds.regression.validators;

import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.StdoutLogEntry;

import java.io.IOException;
import java.util.List;

public class StdoutValidator extends NodeValidator {

	private static final String KEY_ERROR_MSG = "ERROR: creating all new keys";

	private boolean isValidated = false;
	private boolean isValid = true;

	public StdoutValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	@Override
	public void validate() throws IOException {
		int nodeNum = nodeData.size();
		for (int i = 0; i < nodeNum; i++) {
			LogReader<StdoutLogEntry> stdoutReader = nodeData.get(i).getStdoutReader();
			stdoutReader.readFully();

			long exceptionCount = stdoutReader.getExceptionCount();

			for (final StdoutLogEntry ex : stdoutReader.getExceptions()) {
				if (ex.getLogEntry() != null && ex.getLogEntry().contains(KEY_ERROR_MSG)) {
					exceptionCount--;
				}
			}

			if (exceptionCount > 0) {
				addError(String.format("Node %d had %d errors in stdout/stderr", i, stdoutReader.getExceptionCount()));
				isValid = false;
			}
		}
		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
