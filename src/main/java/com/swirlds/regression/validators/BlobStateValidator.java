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

import com.swirlds.blob.BinaryObjectStore;
import com.swirlds.blob.internal.db.BlobStoragePipeline;
import com.swirlds.blob.internal.db.DbManager;
import com.swirlds.blob.internal.db.SnapshotManager;
import com.swirlds.common.crypto.Hash;
import com.swirlds.demo.platform.PlatformTestingDemoMain;
import com.swirlds.demo.platform.PlatformTestingDemoState;
import com.swirlds.demo.platform.fcm.MapKey;
import com.swirlds.demo.platform.fcm.MapValueBlob;
import com.swirlds.fcmap.FCMap;
import com.swirlds.platform.SignedStateFileManager;
import com.swirlds.platform.state.SignedState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.DEFAULT_SETTINGS_DIR;

public class BlobStateValidator extends NodeValidator {
	private boolean isValidated = false;
	private boolean isValid = false;

	private File experimentFolder;
	private File dataFolder;

	public BlobStateValidator(List<NodeData> nodeData, String experimentPath) {
		super(nodeData);
		this.experimentFolder = new File(experimentPath);
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

	final static String pgFileName = "PostgresBackup.tar";
	final static String stateFileName = "SignedState.swh";
	final static String swirldNum = "123";

	private FCMap recoverStorageMapFromSavedState(File file) {
		PlatformTestingDemoState ptdState = new PlatformTestingDemoState();
		SignedState signedState = new SignedState(ptdState);
		try {
			BinaryObjectStore bos = BinaryObjectStore.getInstance();
			bos.startInit();
			SignedStateFileManager.readObjectFromFile(file, signedState);
			bos.stopInit();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ptdState.getStateMap().getStorageMap();
	}

	private FCMap getStorageMapFromSavedState(File file) {
		PlatformTestingDemoState ptdState = new PlatformTestingDemoState();
		SignedState signedState = new SignedState(ptdState);
		try {
			SignedStateFileManager.readObjectFromFile(file, signedState);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ptdState.getStateMap().getStorageMap();
	}

	private boolean checkState(FCMap<MapKey, MapValueBlob> map) {
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

	private boolean checkForDanglingLOs() {
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

	public boolean checkStateMatchesDb(File roundDir) {
		boolean retVal = true;
		//restore database
		try {
			SnapshotManager.restoreSnapshotFromFile(DEFAULT_SETTINGS_DIR + "data", roundDir);
		} catch (FileNotFoundException e) {
			addError("restore failed for: " + roundDir.getAbsolutePath());
			return false;
		}

		//deserialize state and get storage map
		File signedStateFile = new File(roundDir, stateFileName);
		FCMap<MapKey, MapValueBlob> fcMap = recoverStorageMapFromSavedState(signedStateFile);

		if (checkState(fcMap)) {
			addInfo("state file check succeeded for: " + roundDir.getAbsolutePath());
		} else {
			addError("state file check failed for: " + roundDir.getAbsolutePath());
			retVal = false;
		}

		if (checkForDanglingLOs()) {
			addInfo("no danglingLOs found for: " + roundDir.getAbsolutePath());
		} else {
			addError("danglingLOs found for: " + roundDir.getAbsolutePath());
			retVal = false;
		}

		if (retVal) {
			addInfo("checkStateMatchesDb succeeded for: " + roundDir.getAbsolutePath());
		} else {
			addError("checkStateMatchesDb failed for: " + roundDir.getAbsolutePath());
		}
		return retVal;
	}

	public boolean checkStateFiles(File[] files) {
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
		System.out.println("checkStateFiles succeeded for round: " + files[0].getParentFile().getName());
		return true;
	}

	public boolean checkStateForResultsPath(String path) {
		File nodeContainingDir = new File(path);
		boolean retVal = true;

		if (!nodeContainingDir.exists()) {
			addError("results path invalid");
			return false;
		}

		File[] nodes = nodeContainingDir.listFiles(File::isDirectory);

		List<List<File>> nodesByRound = new ArrayList<>();

		for (File node : nodes) {
			File descendedFolder = new File(node, "data/saved/" + PlatformTestingDemoMain.class.getName());

			if (!descendedFolder.exists()) {
				addError("folder containing nodes did not exist: " + descendedFolder.getPath());
				return false;
			}

			File[] nodeList = descendedFolder.listFiles((dir, name) -> name.matches("[0-9]+"));

			if (nodeList.length != 1) {
				addError("expected 1 node folder: " + descendedFolder.getPath() + " in results for node, " +
						"found: " + nodeList.length);
				return false;
			}

			File swirldFolder = new File(nodeList[0], swirldNum);

			if (!swirldFolder.exists()) {
				addError("no swirld folder in node " + node.getAbsolutePath());
				return false;
			}

			File[] rounds = swirldFolder.listFiles(File::isDirectory);

			for (int i = 0; i < rounds.length; i++) {
				if (nodesByRound.size() < rounds.length) {
					nodesByRound.add(new ArrayList<>());
				}
				nodesByRound.get(i).add(rounds[i]);
			}
		}

		for (List<File> nodeList : nodesByRound) {
			boolean success = true;
			if (nodeList.size() != nodes.length) {
				addError("not all nodes saved in each round");
				return false;
			}

			File[] stateFiles = new File[nodeList.size()];

			for (int i = 0; i < nodeList.size(); i++) {
				stateFiles[i] = new File(nodeList.get(i), stateFileName);
				if (stateFiles[i].exists() == false) {
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

			retVal &= success;
		}
		return retVal;
	}
}
