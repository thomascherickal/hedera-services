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

import com.swirlds.blob.BinaryObjectStore;
import com.swirlds.blob.internal.db.BlobStoragePipeline;
import com.swirlds.blob.internal.db.DbManager;
import com.swirlds.blob.internal.db.SnapshotManager;
import com.swirlds.common.crypto.Hash;
import com.swirlds.demo.platform.PlatformTestingDemoMain;
import com.swirlds.demo.platform.PlatformTestingDemoState;
import com.swirlds.demo.platform.fcm.MapValueBlob;
import com.swirlds.fcmap.FCMap;
import com.swirlds.fcmap.test.pta.MapKey;
import com.swirlds.platform.SignedStateFileManager;
import com.swirlds.platform.state.SignedState;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.DEFAULT_SETTINGS_DIR;
import static com.swirlds.regression.RegressionUtilities.REMOTE_SAVED_FOLDER;

public class BlobStateValidator extends Validator {
	private boolean isValidated = false;
	private boolean isValid = false;

	private File experimentFolder;

	private final static String STATE_FILE_NAME = "SignedState.swh";
	private final static String SWIRLD_NUM = "123";

	public void setExperimentFolder(String experimentFolder) {
		this.experimentFolder = new File(experimentFolder);
		if (!this.experimentFolder.exists()) {
			addError("experiment path did not exist");
		}
	}

	@Override
	public void validate() {
		isValid = checkStateForResultsPath(experimentFolder.getPath());
		isValidated = true;
	}

	@Override
	public boolean isValid() {
		return isValidated && isValid;
	}

	/**
	 * get fcMap containing blobs from saved state file
	 *
	 * @param file
	 * 	file is saved state
	 * @return map from deserialized saved state file
	 */
	FCMap recoverStorageMapFromSavedState(File file) {
		FCMap storageMap = null;

		BinaryObjectStore bos = BinaryObjectStore.getInstance();
		bos.startInit();
		storageMap = getStorageMapFromSavedState(file);
		bos.stopInit();

		return storageMap;
	}

	FCMap getStorageMapFromSavedState(File file) {
		PlatformTestingDemoState ptdState = new PlatformTestingDemoState();
		SignedState signedState = new SignedState(ptdState);
		try {
			signedState = SignedStateFileManager.readSignedStateFromFile(file, signedState);
		} catch (IOException e) {
			addError("could not read signed state");
			return null;
		}

		return ptdState.getStateMap().getStorageMap();
	}

	/**
	 * check for consistency between postgres backup and recovered saved state file
	 * @param map
	 * 	FCMap containing values to verify the blob content of
	 * @return true=all values consistent, false=fail
	 */
	boolean checkState(FCMap<MapKey, MapValueBlob> map) {
		boolean retVal = true;
		BinaryObjectStore bos = BinaryObjectStore.getInstance();
		for (MapValueBlob blob : map.values()) {
			//success of getBlobContent confirms presence of hash and successful recovery of binaryObject
			byte[] content = blob.getBlobContent();
			Hash hashOfContent = bos.hashOf(content);

			if (blob.getHash().compareTo(hashOfContent) != 0) {
				addError("hash of content did not match hash in MapValueBlob:" +
						"\n\texpected:" + blob.getHash().toString() +
						"\n\tactual:" + hashOfContent.toString());
				retVal = false;
			}
		}

		return retVal;
	}

	/**
	 * check whether postgres backup is consistent between large object table and binary_object table
	 *
	 * @return true=pass, false=fail
	 */
	boolean checkForDanglingLOs() {
		boolean retVal = true;
		try (BlobStoragePipeline pipeline = DbManager.getInstance().blob()) {
			HashSet<Long> loIds = new HashSet(pipeline.retrieveLoIDs());
			HashSet<Long> binaryObjectOIds = new HashSet(pipeline.retrieveOidsBinaryObjectTable());

			for (Long id : loIds) {
				if (!binaryObjectOIds.contains(id)) {
					addError("Dangling LOID found in large object table for id: " + id);
					retVal = false;
				}
			}
		} catch (SQLException e) {
			addError("sql exception retrieving loIds");
			retVal = false;
		}
		return retVal;
	}

	/**
	 * check whether recovery from a saved state and postgres backup is successful and
	 * whether there are any database inconsistencies or dangling objects
	 *
	 * @param roundDir
	 * 	directory containing a saved state and a postgres backup for a round
	 * @return true=recovery succeeded and no dangling objects found, false=error found
	 */
	boolean checkStateMatchesDb(File roundDir) {
		boolean retVal = true;
		File signedStateFile = new File(roundDir, STATE_FILE_NAME);

		//restore database
		try {
			SnapshotManager.restoreSnapshotFromFile(DEFAULT_SETTINGS_DIR + "data", roundDir);
		} catch (IllegalArgumentException e) {
			addError("restore failed for: " + roundDir.getAbsolutePath());
			return false;
		}

		//deserialize state and get storage map
		FCMap<MapKey, MapValueBlob> fcMap = recoverStorageMapFromSavedState(signedStateFile);

		if (fcMap != null) {
			if (!checkState(fcMap)) {
				addError("state file check failed for: " + roundDir.getAbsolutePath());
				retVal = false;
			}

			if (!checkForDanglingLOs()) {
				addError("danglingLOs found for: " + roundDir.getAbsolutePath());
				retVal = false;
			}
		} else {
			//failed to retrieve fcMap
			retVal = false;
		}

		return retVal;
	}

	/**
	 * check for consistency between saved state files for a round
	 *
	 * @param files
	 * saved state files for each node of a round
	 * @return true=all states consistent, fail=at least 1 node inconsistent
	 */

	boolean checkStateFiles(File[] files) {
		if (files.length < 2) {
			addError("less than two state files provided to checkStateFiles");
			return false;
		}
		FCMap<MapKey, MapValueBlob> fcMap = getStorageMapFromSavedState(files[0]);
		Hash fcMapHash = new Hash(fcMap.getRootHash());

		for (int i = 1; i < files.length; i++) {
			FCMap<MapKey, MapValueBlob> fcMapCompare = getStorageMapFromSavedState(files[i]);
			Hash fcMapCompareHash = new Hash(fcMapCompare.getRootHash());

			if (!fcMapHash.equals(fcMapCompareHash)) {
				addError("Hash doesn't match every state");
				return false;
			}
		}
		return true;
	}

	/**
	 * traverse node directories and get list of directories for equivalent rounds for each node
	 *
	 * @param nodes
	 *   directories containing data for nodes
	 * @return list of node directories for a round, listed by round
	 */
	List<List<File>> getNodesByRound(File[] nodes) {
		List<List<File>> nodesByRound = new ArrayList<>();

		for (File node : nodes) {
			File descendedFolder = new File(node, REMOTE_SAVED_FOLDER + "/" + PlatformTestingDemoMain.class.getName());

			if (!descendedFolder.exists()) {
				addError("folder containing nodes did not exist: " + descendedFolder.getPath());
				return null;
			}

			File[] nodeList = descendedFolder.listFiles((dir, name) -> name.matches("[0-9]+"));

			if (nodeList.length != 1) {
				addError("expected 1 node folder: " + descendedFolder.getPath() + " in results for node, " +
						"found: " + nodeList.length);
				return null;
			}

			File swirldFolder = new File(nodeList[0], SWIRLD_NUM);

			if (!swirldFolder.exists()) {
				addError("no swirld folder in node " + node.getAbsolutePath());
				return null;
			}

			File[] rounds = swirldFolder.listFiles(File::isDirectory);

			for (int i = 0; i < rounds.length; i++) {
				if (nodesByRound.size() < rounds.length) {
					nodesByRound.add(new ArrayList<>());
				}
				nodesByRound.get(i).add(rounds[i]);
			}
		}

		return nodesByRound;
	}

	/**
	 * verify saved state and postgres backup from experiment is consistent across all nodes and rounds
	 *
	 * @param path
	 * path to folder containing data copied from nodes
	 *
	 * @return pass=true, fail=false
	 */

	boolean checkStateForResultsPath(String path) {
		File nodeContainingDir = new File(path);
		boolean retVal = true;

		if (!nodeContainingDir.exists()) {
			addError("results path invalid");
			return false;
		}

		File[] nodes = nodeContainingDir.listFiles(File::isDirectory);

		if (nodes.length == 0) {
			addError("no node folders found");
			return false;
		}

		List<List<File>> nodesByRound = getNodesByRound(nodes);

		if (nodesByRound == null) {
			return false;
		}

		for (List<File> nodeList : nodesByRound) {
			boolean success = true;
			if (nodeList.size() != nodes.length) {
				addError("not all nodes saved in each round");
				return false;
			}

			File[] stateFiles = new File[nodeList.size()];

			for (int i = 0; i < nodeList.size(); i++) {
				stateFiles[i] = new File(nodeList.get(i), STATE_FILE_NAME);
				if (!stateFiles[i].exists()) {
					addError("State file not found: " + stateFiles[i].getAbsolutePath());
					success = false;
				}
			}

			if (!success) {
				retVal = false;
				continue;
			}

			//check for consensus between all state files for round
			if (!checkStateFiles(stateFiles)) {
				success = false;
			}

			//check each node vs PostgresBackup
			for (File node : nodeList) {
				if (!checkStateMatchesDb(node)) {
					addError("state did not match db for: " + node.getAbsolutePath());
					success = false;
				}
			}

			if (success) {
				addInfo("All checks succeeded for round " + nodeList.get(0).getName());
			}

			retVal &= success;
		}
		return retVal;
	}

}
