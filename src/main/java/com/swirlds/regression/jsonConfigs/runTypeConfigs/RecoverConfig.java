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

package com.swirlds.regression.jsonConfigs.runTypeConfigs;

import com.swirlds.regression.jsonConfigs.FileRequirement;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.PTD_CONFIG_DIR;

public class RecoverConfig implements FileRequirement {
	private String eventDir;
	private String sqlFile;

	public String getEventDir() {
		return eventDir;
	}

	public void setEventDir(String eventDir) {
		this.eventDir = eventDir;
	}

	public String getSqlFile() {
		return sqlFile;
	}

	public void setSqlFile(String sqlFile) {
		this.sqlFile = sqlFile;
	}

	@Override
	public List<String> getFilesNeeded() {
		List<String> returnList = new LinkedList<>();
		String path = PTD_CONFIG_DIR + sqlFile;
		System.out.println("Get file needed " + path);
		File tempFile = new File(path);
		if (tempFile.exists()) {
			returnList.add(path);
		}

		return returnList;
	}
}
