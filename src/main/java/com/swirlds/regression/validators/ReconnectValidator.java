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
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.PlatformLogEntry;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.swirlds.common.PlatformStatNames.ROUND_SUPER_MAJORITY;
import static com.swirlds.common.PlatformStatNames.TRANSACTIONS_HANDLED_PER_SECOND;
import static com.swirlds.common.logging.PlatformLogMessages.CHANGED_TO_ACTIVE;
import static com.swirlds.common.logging.PlatformLogMessages.FINISHED_RECONNECT;
import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_ERROR;
import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_HASH_MISMATCH;
import static com.swirlds.common.logging.PlatformLogMessages.RECV_STATE_IO_EXCEPTION;
import static com.swirlds.common.logging.PlatformLogMessages.START_RECONNECT;
import static com.swirlds.common.logging.PlatformLogMessages.SYNC_STALE_COMPENSATION_FAILURE;
import static com.swirlds.common.logging.PlatformLogMessages.SYNC_STALE_COMPENSATION_SUCCESS;

public class ReconnectValidator extends NodeValidator {
	private TestConfig testConfig;

	public ReconnectValidator(List<NodeData> nodeData, TestConfig testConfig) {
		super(nodeData);
		this.testConfig = testConfig;
	}

	// Is used for validating reconnection after running startFromX test, should be the max value of roundNumber of
	// savedState the nodes start from;
	// At the end of the test, if the last entry of roundSup of reconnected node is less than this value, the reconnect
	// is considered to be invalid
	double savedStateStartRoundNumber = 0;

	// we consider the reconnect is valid when the difference between the last entry of roundSup of reconnected node
	// and other nodes is not
	// greater than this value
	double lastRoundSupDiffLimit = 10;

	boolean isValidated = false;
	boolean isValid = true;

	public void setSavedStateStartRoundNumber(double savedStateStartRoundNumber) {
		this.savedStateStartRoundNumber = savedStateStartRoundNumber;
	}

	/**
	 * Check log and csv of reconnect test results
	 * An passing test case should have following key messages in order
	 *
	 * START_RECONNECT
	 * FINISHED_RECONNECT
	 *
	 * Failed test A) multiple repetition of following messages
	 *
	 * START_RECONNECT
	 * FINISHED_RECONNECT
	 * RECV_STATE_HASH_MISMATCH
	 *
	 * START_RECONNECT
	 * FINISHED_RECONNECT
	 * RECV_STATE_HASH_MISMATCH
	 *
	 * START_RECONNECT
	 * FINISHED_RECONNECT
	 * RECV_STATE_HASH_MISMATCH
	 *
	 *
	 * Failed test B) multiple repetition of following messages
	 *
	 * START_RECONNECT
	 * RECV_STATE_ERROR
	 *
	 * START_RECONNECT
	 * RECV_STATE_ERROR
	 *
	 * START_RECONNECT
	 * RECV_STATE_ERROR
	 *
	 * @throws IOException
	 */
	@Override
	public void validate() throws IOException {
		int nodeNum = nodeData.size();
		final int reconnectNodeId = getLastStakedNode();

		double minLastRoundSup = 0; //minimum last entry of roundSup among all node except the reconnected node
		double maxLastRoundSup = 0; //maximum last entry of roundSup among all node except the reconnected node

		for (int i = 0; i < reconnectNodeId; i++) {
			LogReader nodeLog = nodeData.get(i).getLogReader();
			CsvReader nodeCsv = nodeData.get(i).getCsvReader();

			if (nodeLogIsNull(nodeLog, i)) {
				isValid = false;
				continue;
			}

			nodeLog.readFully();

			isValid = checkExceptions(nodeLog, i);

			if (nodeCsvIsNull(nodeCsv, i)) {
				isValid = false;
				continue;
			}

			log.info(MARKER, "nodecsv is not null");
			if (nodeCsv.getColumn(TRANSACTIONS_HANDLED_PER_SECOND) == null) {
				addError(TRANSACTIONS_HANDLED_PER_SECOND + " is returning null");
			}

			if (i == 0) {
				minLastRoundSup = nodeCsv.getColumn(ROUND_SUPER_MAJORITY).getLastEntryAsDouble();
				maxLastRoundSup = minLastRoundSup;
			} else {
				double thisLastRoundSup = nodeCsv.getColumn(ROUND_SUPER_MAJORITY).getLastEntryAsDouble();
				minLastRoundSup = Math.min(minLastRoundSup, thisLastRoundSup);
				maxLastRoundSup = Math.max(maxLastRoundSup, thisLastRoundSup);
			}
		}

		// check the reconnect node's log and csv
		LogReader<PlatformLogEntry> nodeLog = nodeData.get(reconnectNodeId).getLogReader();
		CsvReader nodeCsv = nodeData.get(reconnectNodeId).getCsvReader();

		if (nodeLogIsNull(nodeLog, reconnectNodeId) || nodeCsvIsNull(nodeCsv, reconnectNodeId)) {
			isValid = false;
			return;
		}

		boolean nodeReconnected = false;
		Instant active = null;
		while (true) {
			//PlatformLogEntry start = nodeLog.nextEntryContaining(Arrays.asList(START_RECONNECT,
			// RECV_STATE_HASH_MISMATCH));
			PlatformLogEntry start = nodeLog.nextEntryContaining(
					Arrays.asList(CHANGED_TO_ACTIVE, START_RECONNECT, RECV_STATE_HASH_MISMATCH,
							SYNC_STALE_COMPENSATION_SUCCESS, SYNC_STALE_COMPENSATION_FAILURE));
			if (start == null) {
				break;
			} else if (start.getLogEntry().contains(RECV_STATE_HASH_MISMATCH)) {
				addError(String.format("Node %d hash mismatch of received hash", reconnectNodeId));
				continue; // try to find next START_RECONNECT
			} else if (start.getLogEntry().contains(CHANGED_TO_ACTIVE)) {
				active = start.getTime();
				continue; // Record the time when platform status changes to ACTIVE
			} else if (start.getLogEntry().contains(SYNC_STALE_COMPENSATION_SUCCESS)) {
				addWarning("SYNC: Compensated for stale events during gossip.");
				continue;
			} else if (start.getLogEntry().contains(SYNC_STALE_COMPENSATION_FAILURE)) {
				addError("SYNC: Failed to compensate for stale events during gossip due to threshold limits.");
				continue;
			}
			// we have a START_RECONNECT now, try to find FINISHED_RECONNECT or RECV_STATE_ERROR or
			// RECV_STATE_IO_EXCEPTION

			PlatformLogEntry end = nodeLog.nextEntryContaining(
					Arrays.asList(FINISHED_RECONNECT, RECV_STATE_ERROR, RECV_STATE_IO_EXCEPTION));
			if (end == null) {
				addError(String.format("Node %d started a reconnect, but did not finish!", reconnectNodeId));
				isValid = false;
				break;
			}

			if (end.getLogEntry().contains(FINISHED_RECONNECT)) {
				nodeReconnected = true;
				long time = start.getTime().until(end.getTime(), ChronoUnit.MILLIS);
				addInfo(String.format("Node %d reconnected, time taken %dms", reconnectNodeId, time));
			} else if (end.getLogEntry().contains(RECV_STATE_ERROR)) {
				// for killNetworkReconnect test this error is allowed only if it happens within
				// 30 seconds after platform status becomes ACTIVE. This can happen because the IP tables might be
				// getting rebuilt on the reconnected node.
				// This error should not be allowed on killNodeReconnect test.
				if (testConfig != null && testConfig.getReconnectConfig().isKillNetworkReconnect() &&
						active != null && Duration.between(active, end.getTime()).getSeconds() < 30) {
					addInfo(String.format("Node %d error during receiving SignedState. It can be ignored, " +
							"as it occurred within 30 seconds of platform becoming ACTIVE", reconnectNodeId));
				} else {
					addError(String.format("Node %d error during receiving SignedState", reconnectNodeId));
					isValid = false;
				}
			}
		}

		if (!nodeReconnected) {
			addError(String.format("Node didn't reconnect"));
			isValid = false;
		} else {
			double roundSup = nodeCsv.getColumn(ROUND_SUPER_MAJORITY).getLastEntryAsDouble();

			if (roundSup < savedStateStartRoundNumber) {
				addError(String.format("Node %d 's last Entry of roundSup %f is less than " +
								"savedStateStartRoundNumber %f",
						reconnectNodeId, roundSup, savedStateStartRoundNumber));
				isValid = false;
			}

			if (minLastRoundSup - roundSup > lastRoundSupDiffLimit) {
				isValid = false;
				addError(String.format(
						"Difference of last roundSup between reconnected Node %d with other nodes exceeds " +
								"lastRoundSupDiffLimit %.0f. maxLastRoundSup: %.0f; minLastRoundSup: %.0f; " +
								"reconnected node's last roundSup: %.0f.",
						reconnectNodeId, lastRoundSupDiffLimit, maxLastRoundSup, minLastRoundSup, roundSup));
			}
		}
		// check last node's exceptions
		isValidated = checkExceptions(nodeLog, reconnectNodeId);
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}

	/**
	 * Check if the exception contained in a LogEntry is acceptable
	 * In reconnect test, exceptions with TESTING_EXCEPTIONS_ACCEPTABLE_RECONNECT marker are acceptable;
	 *
	 * @param e
	 * @return
	 */
	boolean isAcceptable(final PlatformLogEntry e, final int nodeId) {
		if (e.getMarker() == LogMarkerInfo.TESTING_EXCEPTIONS_ACCEPTABLE_RECONNECT) {
			return true;
		}
		// in Reconnect test, the last node is the reconnect node;
		// only when current node is the reconnect node,
		// exceptions with TESTING_EXCEPTIONS_ACCEPTABLE_RECONNECT_NODE are acceptable
		if (e.getMarker() == LogMarkerInfo.TESTING_EXCEPTIONS_ACCEPTABLE_RECONNECT_NODE) {
			return nodeId == nodeData.size() - 1;    // lastNode
		}
		return false;
	}

	/**
	 * check whether nodeLog is null, if it is, log an error
	 *
	 * @param nodeLog
	 * @param nodeId
	 * @return
	 */
	boolean nodeLogIsNull(final LogReader nodeLog, final int nodeId) {
		if (nodeLog == null) {
			addError("could not load log, exiting validation for node " + nodeId);
			return true;
		}
		return false;
	}

	/**
	 * check whether nodeCsv is null, if it is, log an error
	 *
	 * @param nodeCsv
	 * @param nodeId
	 * @return
	 */
	boolean nodeCsvIsNull(final CsvReader nodeCsv, final int nodeId) {
		if (nodeCsv == null) {
			addError("could not load csv, exiting validation for node " + nodeId);
			return true;
		}
		return false;
	}

	/**
	 * Check exceptions in nodeLog and log the result
	 *
	 * @param nodeLog
	 * @param nodeId
	 * @return
	 */
	boolean checkExceptions(final LogReader<PlatformLogEntry> nodeLog, final int nodeId) {
		int socketExceptions = 0;
		int unexpectedErrors = 0;
		int signedStateErrors = errorMessages.stream().
				filter(e -> e.contains("Node 3 error during receiving SignedState")).
				collect(Collectors.toList()).size();
		for (PlatformLogEntry e : nodeLog.getExceptions()) {
			if (e.getMarker() == LogMarkerInfo.SOCKET_EXCEPTIONS) {
				socketExceptions++;
			} else if (e.getLogEntry().contains(RECV_STATE_ERROR)) {
				// check number of times this error is considered as error. This might can be considered as an INFO
				// if it
				// has happened within 30 seconds of platform status becomes ACTIVE.
				if (signedStateErrors > 0) {
					unexpectedErrors++;
				}
				signedStateErrors--;
			} else if (!isAcceptable(e, nodeId)) {
				unexpectedErrors++;
				isValid = false;
				addError(String.format("Node %d has unexpected error: [%s]", nodeId, e.getLogEntry()));
			}
		}
		if (socketExceptions > 0) {
			addInfo(String.format("Node %d has %d socket exceptions. Some are expected for Reconnect.",
					nodeId,
					socketExceptions));
		}

		if (unexpectedErrors > 0) {
			addError(String.format("Node %d has %d unexpected errors!", nodeId, unexpectedErrors));
			return false;
		}
		return true;
	}
}
