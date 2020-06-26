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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static com.swirlds.regression.RegressionUtilities.EMPTY_HASH;
import static com.swirlds.regression.RegressionUtilities.EVENT_MATCH_MSG;

/**
 * the files for validation are generate by SSHService.makeSha1sumOfStreamedEvents(int, StreamType)}
 */
public class StreamingServerValidator extends Validator {
	/**
	 * name of the file which contains sha1sum of the file whose name is
	 * {@link StreamingServerValidator#SHA_LIST_FILE_NAME}
	 */
	public static final String FINAL_HASH_FILE_NAME = "sha1sum_total_%s.log";
	/**
	 * name of the file which contains a list of sha1sum of each stream file
	 */
	public static final String SHA_LIST_FILE_NAME = "sha1sum_%s.log";
	/**
	 * name of the file which contains a list of file name and byte size in stream file directory
	 */
	public static final String FILE_LIST_FILE_NAME = "%s_list.log";
	/**
	 * name of the file which contains a list of stream signature file name
	 */
	public static final String SIG_LIST_FILE_NAME = "%s_sig_list.log";

	private final List<StreamingServerData> ssData;

	private boolean valid = false;
	private boolean stateRecoverMode = false;
	// for reconnect test, we only validate the nodes that did not restart, i.e. all nodes except the last one
	private boolean reconnect = false;

	/**
	 * stream file type: EVENT or RECORD
	 */
	private StreamType streamType;

	public StreamingServerValidator(final List<StreamingServerData> ssData,
			final boolean reconnect, final StreamType streamType) {
		this.ssData = ssData;
		this.reconnect = reconnect;
		this.streamType = streamType;
	}

	@Override
	public void validate() {
		if (ssData.size() == 0) {
			addError("No data to validate!");
			return;
		}

		// for reconnect test, we only validate the nodes that did not restart, i.e. all nodes except the last one
		final int validateNodesNum = reconnect ? getLastStakedNode() : ssData.size();

		final List<String> sha1sumList = new ArrayList<>(validateNodesNum);
		for (int i = 0; i < validateNodesNum; i++) {
			sha1sumList.add(ssData.get(i).getSha1SumData());
		}

		if (!isAnySha1SumMissing(sha1sumList)) {
			isChecksumListValid(sha1sumList);
		}

		validateEvtSigFiles(validateNodesNum);

		if (stateRecoverMode) {
			for (int i = 0; i < ssData.size(); i++) {
				if (!checkRecoverEventMatchLog(ssData.get(i).getRecoverEventMatchLog())) {
					addError("Node " + i + " recovered event file does not match original ones !");
					this.valid = false;
				} else {
					addInfo("Node " + i + " recovered event file match original ones");
				}
			}
		}
	}

	/**
	 * check if there is any sha1sum missing for any nodes
	 *
	 * @param sha1sumList
	 * 		one sha1sum for one node
	 */
	private boolean isAnySha1SumMissing(final List<String> sha1sumList) {
		boolean someEmpty = false;
		for (int i = 0; i < sha1sumList.size(); i++) {
			final String sha1sum = sha1sumList.get(i);
			if (sha1sum == null || sha1sum.length() == 0) {
				/* did were there events, but the sumsha1 failed to execute for some reason? */
				if (ssData.get(i).getNumberOfStreamFiles() > 0) {
					addError(String.format("No sha1sum found for server %d, " +
									"but there were %d %s recorded!",
							i, ssData.get(i).getNumberOfStreamFiles(), streamType.getDescription()));
				} else {
					addError(String.format("No sha1sum found for server %d, " +
									"and no %s were recorded!",
							i, streamType.getDescription()));
				}
				someEmpty = true;
			}
		}

		return someEmpty;
	}

	private void isChecksumListValid(final List<String> sha1sumList) {
		if (sha1sumList.size() < 1) {
			addInfo(String.format("No problems found with the events recorded by the first %d servers", ssData.size()));
			this.valid = true;
			return;
		}

		boolean someEmpty = false;
		boolean mismatch = false;
		String prev = null;
		for (int i = 0; i < sha1sumList.size(); i++) {
			final String curr = sha1sumList.get(i);
			if (curr.startsWith(EMPTY_HASH)) {
				addError(String.format("Server %d had no evt files.", i));
				someEmpty = true;
			}

			if (prev != null && !prev.equals(curr)) {
				if (isMismatchOnlyLastEvent(i)) {
					addInfo(String.format(
							"Server %d and %d do servers have the same hashes, except the last evt. This is common " +
									"in apps that do not have a set ending.", i - 1, i));
				} else {
					addInfo(String.format("Server %d and %d do not have the same sha1sum file %s", i - 1, i,
							buildFinalHashFileName(streamType.getExtension())));
					mismatch = true;
				}
			}
			prev = curr;
		}

		if (!mismatch && !someEmpty) {
			addInfo(String.format("The events saved by the first %d servers have the same hashes.",
					sha1sumList.size()));
			valid = true;
		}
	}

	/*
		Because some nodes may be killed while events are still being written, the last event file (or two) may
		mismatch. For that reason we check from the second to last common last event back. If a difference is found
		then an error existing in the streaming data. There is a small statistical chance the lastCommonEvent is not
		mismatched, but that a new evt file was started after the last line of the leastCommonEvt was written. This is
		a small likelihood and it is assumed that in this case if all other events are equal the lastCommonEvent
		would be equal as well.
	 */
	private boolean isMismatchOnlyLastEvent(final int node) {
		/* grab streaming server data and make sure it's not null */
		final StreamingServerData currNode = ssData.get(node);
		final StreamingServerData prevNode = ssData.get(node - 1);
		if (currNode == null || prevNode == null
				|| currNode.getNumberOfStreamFiles() == 0 || prevNode.getNumberOfStreamFiles() == 0
		) {
			return false;
		}
		/* lastCommonEvent should be an index, but the getNumberOfEvents returns a size */
		final int lastCommonEvent = Math.min(currNode.getNumberOfStreamFiles(), prevNode.getNumberOfStreamFiles()) - 1;
		/* Grab all the events, and start checking from the event BEFORE the lastCommonEvent, if there are any
		   differences return false. If the only difference was the lastCommonEvent return true. this function assumes
		   lastCommonEvent is mismatched because this function should only be called if the sha for the totals is
		   wrong. */
		final List<String> currEventList = currNode.getSha1ListData();
		final List<String> prevEventList = prevNode.getSha1ListData();
		for (int i = lastCommonEvent - 1; i >= 0; i--) {
			if (!currEventList.get(i).equals(prevEventList.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	private void validateEvtSigFiles(final int validateNodesNum) {
		boolean evtsValid = true;
		final List<EventSigEvent> sigEvents = this.ssData.stream()
				.map(StreamingServerData::getSigFileNames)
				.collect(Collectors.toList());

		final EventSigEvent reference = sigEvents.get(0);
		for (int index = 1; index < validateNodesNum; index++) {
			final EventSigEvent event = sigEvents.get(index);
			if (!reference.equals(event)) {
				String description = "The contents of two nodes don't match:\n\n" +
						"Reference Node 0: \n" +
						reference.toString() +
						"Validating node " +
						index +
						": \n" +
						event.toString() +
						"--- End of diff\n" +
						'\n';
				addError(description);
				evtsValid = false;
			}
		}

		this.valid &= evtsValid;
		addInfo(String.format("Are evts_sig files valid: %s for %d files", evtsValid, reference.size()));
	}

	private boolean checkRecoverEventMatchLog(InputStream input) {
		if (input != null) {
			Scanner eventScanner = new Scanner(input);
			if (eventScanner.hasNextLine()) {
				String entry = eventScanner.nextLine();
				log.info(MARKER, "Read match log entry = {}", entry);
				if (entry.contains(EVENT_MATCH_MSG)) {
					return true;
				}
			}
		} else {
			log.error(ERROR, "Input stream of event match log after state recover is null");
			return false;
		}
		return false;
	}

	public boolean isStateRecoverMode() {
		return stateRecoverMode;
	}

	public void setStateRecoverMode(boolean stateRecoverMode) {
		this.stateRecoverMode = stateRecoverMode;
	}

	/**
	 * build name of the file which contains sha1sum of the file whose name is {@link
	 * StreamingServerValidator#buildShaListFileName}
	 *
	 * @param extension
	 * @return
	 */
	public static String buildFinalHashFileName(final String extension) {
		return String.format(FINAL_HASH_FILE_NAME, extension);
	}

	/**
	 * build name of the file which contains a list of sha1sum of each stream file
	 */
	public static String buildShaListFileName(final String extension) {
		return String.format(SHA_LIST_FILE_NAME, extension);
	}

	/**
	 * build name of the file which contains a list of file name and byte size in stream file directory
	 */
	public static String buildFileListFileName(final String extension) {
		return String.format(FILE_LIST_FILE_NAME, extension);
	}

	/**
	 * build name of the file which contains a list of stream signature file name
	 */
	public static String buildSigListFileName(final String extension) {
		return String.format(SIG_LIST_FILE_NAME, extension);
	}
}
