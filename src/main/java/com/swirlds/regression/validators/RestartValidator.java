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
import com.swirlds.regression.logs.LogEntry;
import com.swirlds.regression.logs.LogReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.swirlds.regression.RegressionUtilities.SIGNED_STATE_DELETE_QUEUE_TOO_BIG;

public class RestartValidator extends NodeValidator {

	public RestartValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	/* String Literals used in this class */
	private static String SUCCESSFUL_RESTART_MESSAGE = "Node %d froze and restarted with %s events";
	private static String UNSUCCESSFUL_RESTART_MESSAGE = "Node %d didFreeze:%s	didUnfreeze:%s	were matched:%s";
	private static String EXCEPTIONS_FOUND_MESSAGE = "Node %d has %d exceptions";

	private static String START_RESTART_MESSAGE = "Freeze state is about to be saved to disk";
	private static String LOAD_RESTART_MESSAGE = "Platform %d has loaded a saved state for round";


	boolean isValidated = false;
	boolean isValid = true;
	private static String FREEZE_START_MESSAGE = "Event creation frozen";
	private static String FREEZE_END_MESSAGE = "Event creation unfrozen";

	/**
	 * Gets each swirlds.log files from each node and scans if for the START_RESTART_MESSAGE and LOAD_RESTART_MESSAGE
	 * If put each into a hashmap with the node number as the key. After checking that the same number of events were
	 * loaded in each node the functions adds appropriate messages about the number of exceptions and is restart was
	 * successful.
	 *
	 * The hashmaps are keyed to node so that if a load from old data on startup occurs then that information will be
	 * overwritten by the latest load this will prevent issues testing the actual restart. It does introduce the
	 * issue of
	 * only being able to test the latest restart of each log though.
	 *
	 * @throws IOException
	 */
	@Override
	public void validate() throws IOException {
		int nodeNum = nodeData.size();

		boolean isNodeRestarted;

		/* look through each line of each log to find evidence of successful/unsuccessful restart */
		HashMap<Long, ArrayList<String>> freezeRounds = new HashMap<>();
		HashMap<Long, ArrayList<String>> unfreezeRounds = new HashMap<>();
		for (int i = 0; i < nodeNum; i++) {
			LogReader nodeLog = nodeData.get(i).getLogReader();
			LogEntry currentEntry;
			/* build node specific search phrase for loading after freeze */
			String loadRestartMessageForNode = String.format(LOAD_RESTART_MESSAGE, i);
			ArrayList<String> keyedFreeze = new ArrayList<>();
			ArrayList<String> keyedUnfreeze = new ArrayList<>();
			boolean freezeStarted = false;
			while (true) {
				currentEntry = nodeLog.nextEntry();
				if (currentEntry == null) {
					break;
				}
				String freezeChk = checkEntryForSearchString(currentEntry, i, START_RESTART_MESSAGE);
				if(!freezeChk.isEmpty()) {
					keyedFreeze.add(freezeChk);
					log.trace(MARKER, "Added entry to freeze rounds: {}",
							freezeChk);
					addInfo(String.format("Node %d start freeze at rounds %s", i, freezeChk));
					freezeStarted = true;
				}
				String unfreezeChk = checkEntryForSearchString(currentEntry, i, loadRestartMessageForNode);
				if(!unfreezeChk.isEmpty()){
					keyedUnfreeze.add(unfreezeChk);
					log.trace(MARKER, "Added entry to unfreeze rounds: {}", unfreezeChk);
				}
				if (currentEntry.getLogEntry().contains(FREEZE_END_MESSAGE)){
					addInfo(String.format("Node %d end freeze",i ));
				}
			}
			if (!freezeStarted){
				addError(String.format("Node %d never started freeze stage",i ));
				isValid = false; // report error if freeze not started
			}
			freezeRounds.put(Long.valueOf(i),keyedFreeze);
			unfreezeRounds.put(Long.valueOf(i), keyedUnfreeze);

			int socketExceptions = 0;
			int unexpectedErrors = 0;
			for (LogEntry e : nodeLog.getExceptions()) {
				if (e.getMarker() == PlatformLogMarker.SOCKET_EXCEPTIONS) {
					socketExceptions++;
				} else if (e.getLogEntry().contains(SIGNED_STATE_DELETE_QUEUE_TOO_BIG)) {
					addWarning(String.format("Node %d has exception:[ %s ]", i, e.getLogEntry()));
				} else {
					unexpectedErrors++;
					isValid = false;
				}
			}
			if (socketExceptions > 0) {
				addInfo(String.format("Node %d has %d socket exceptions. Some are expected for Restart/Freeze.", i,
						socketExceptions));
			}
			if (unexpectedErrors > 0) {
				addError(String.format("Node %d has %d unexpected errors!", i, unexpectedErrors));
			}
		}
		isNodeRestarted = compareRounds(freezeRounds, unfreezeRounds);
		/* chose to do a single for loop and and if/else inside instead of if/else each with a for loop. Mainly to allow
		grouping number of exceptions and the freeze/unfreeze information by node. Exceptions and freeze/unfreeze may
		need to be ungrouped in the future
		 */
		for (int i = 0; i < nodeNum; i++) {
			if (isNodeRestarted) {
				addInfo(String.format(SUCCESSFUL_RESTART_MESSAGE, i,
						freezeRounds.get(Long.valueOf(Integer.valueOf(i).longValue()))));
				isValidated = true;
			} else {
				boolean didFreeze, didUnfreeze, didEventsMatched;
				didFreeze = didUnfreeze = didEventsMatched = true;
				if (freezeRounds.size() == 0) {
					didFreeze = false;
				}
				if (unfreezeRounds.size() == 0) {
					didUnfreeze = false;
				}
				if (didFreeze && didUnfreeze) {
					for (Long key : freezeRounds.keySet()) {
						if (!freezeRounds.get(key).equals(unfreezeRounds.get(key))) {
							didEventsMatched = false;
						}
					}
				}
				/* this message may hold more information than it needs */
				addInfo(String.format(UNSUCCESSFUL_RESTART_MESSAGE, i,
						Boolean.toString(didFreeze), Boolean.toString(didUnfreeze),
						Boolean.toString(didEventsMatched)));
				isValidated = false;
			}
			/* addInfo(String.format(EXCEPTIONS_FOUND_MESSAGE, i,
					nodeData.get(i).getLogReader().getExceptionCount())); */
		}

	}

	/**
	 * Helper function that changes if log entry has the searchPhrase the validator is looking for.
	 *
	 * @param entry
	 * 		- log entry to be checked
	 * @param node
	 * 		- node the current log belongs to
	 * @param searchPhrase
	 * 		- phrase to check
	 * @return - map with a String from the search phrase and a key of the node which this log belongs to
	 */
	private String checkEntryForSearchString(LogEntry entry, int node, String searchPhrase) {
		String returnStr = "";
		if (entry.getLogEntry().contains(searchPhrase)) {
			returnStr = getRoundNumber(searchPhrase, entry.getLogEntry());
		}
		return returnStr;
	}

	/**
	 * compare freeze and unfreeze messages to make sure that the same number of event were saved and loaded.
	 *
	 * @param freezeRounds
	 * 		- information taken from messages output into the logs when freeze started
	 * @param unfreezeRounds
	 * 		= information taken from messages output into the logs when freeze ended
	 * @return - true if all infromation matches for each node, false if any nodes do not have matching information
	 */
	private boolean compareRounds(HashMap<Long, ArrayList<String>> freezeRounds, HashMap<Long, ArrayList<String>> unfreezeRounds) {
		for (Long freezeKey : freezeRounds.keySet()) {
			if (!unfreezeRounds.containsKey(freezeKey)) {
				return false;
			} else if (compareRecordedRestarts(freezeRounds.get(freezeKey), unfreezeRounds.get(freezeKey))) {
				continue;
			}
		}
		return true;
	}

	private boolean compareRecordedRestarts(ArrayList<String> freeze, ArrayList<String> unfreeze){
		if(freeze.size() != unfreeze.size()) {
			return false;
		}
		for(int i = 0; i < freeze.size(); i++){
			if(freeze.get(i) != unfreeze.get(i)){
				return false;
			}
		}
		return true;
	}

	/**
	 * Finds the first pattern match after the searchString and returns that. In this case the pattern is ment to match
	 * the number of events saved or loaded on freeze/unfreeze
	 *
	 * @param searchString
	 * 		- Phrase expected in the entry that preceeds the pattern
	 * @param logLine
	 * 		- log to check for search phrase and pattern
	 * @return - String containing the first occurrence of the pattern after the searchString
	 */
	protected String getRoundNumber(String searchString, String logLine) {
		Pattern pat = Pattern.compile("[0-9]+");
		String roundNumber = "";
		int roundIndex = logLine.indexOf(searchString);
		Matcher match = pat.matcher(logLine.substring(roundIndex + searchString.length()));
		if (match.find()) {
			roundNumber = match.group(0);
		}
		return roundNumber;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}
}
