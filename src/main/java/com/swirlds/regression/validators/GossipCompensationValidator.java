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
import com.swirlds.regression.logs.PlatformLogEntry;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.swirlds.common.logging.PlatformLogMessages.SYNC_STALE_COMPENSATION_FAILURE;
import static com.swirlds.common.logging.PlatformLogMessages.SYNC_STALE_COMPENSATION_SUCCESS;

public class GossipCompensationValidator extends NodeValidator {

	private boolean valid;

	public GossipCompensationValidator(final List<NodeData> nodeData) {
		super(nodeData);
		this.valid = true;
	}

	@Override
	public void validate() throws IOException {
		for (int i = 0; i < nodeData.size(); i++) {
			final LogReader<PlatformLogEntry> nodeLog = nodeData.get(i).getLogReader();

			if (nodeLogIsNull(nodeLog, i)) {
				valid = false;
				continue;
			}

			while (true) {
				final PlatformLogEntry logEntry = nodeLog.nextEntryContaining(
						Arrays.asList(SYNC_STALE_COMPENSATION_SUCCESS, SYNC_STALE_COMPENSATION_FAILURE));

				if (logEntry == null) {
					break;
				}

				if (logEntryContains(logEntry, SYNC_STALE_COMPENSATION_SUCCESS)) {
					reportLogEntry(this::addWarning, logEntry, i);
				} else if (logEntryContains(logEntry, SYNC_STALE_COMPENSATION_FAILURE)) {
					reportLogEntry(this::addError, logEntry, i);
					valid = false;
				}
			}
		}
	}

	@Override
	public boolean isValid() {
		return valid;
	}

}
