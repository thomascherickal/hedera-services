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

import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogEntry;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
					reportLogEntry(this::addInfo, logEntry, i);
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

	/**
	 * check whether nodeLog is null, if it is, log an error
	 *
	 * @param nodeLog
	 * @param nodeId
	 * @return
	 */
	private <T extends LogEntry> boolean nodeLogIsNull(final LogReader<T> nodeLog, final int nodeId) {
		if (nodeLog == null) {
			addError(String.format("Failed to load the swirlds.log, exiting validation for node %d", nodeId));
			return true;
		}

		return false;
	}

	private <T extends LogEntry> boolean logEntryContains(final T entry, final String text) {
		if (entry.getLogEntry() == null || entry.getLogEntry().isEmpty()) {
			return false;
		}

		return entry.getLogEntry().contains(text);
	}

	private <T extends PlatformLogEntry> void reportLogEntry(final Consumer<String> reportingMethod, final T entry,
			final int nodeId) {
		final StringBuilder sb = new StringBuilder();

		final Instant time = entry.getTime();

		if (time != null) {
			sb.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time)).append(" ");
		}

		reportingMethod.accept(sb.append("Node ").append(nodeId).append(": ").append(entry.getLogEntry()).toString());
	}

}
