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

package com.swirlds.regression.jsonConfigs;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.swirlds.regression.RegressionUtilities.PTD_CONFIG_DIR;

public class AppConfig implements FileRequirement {
	private String jar;
	private ArrayList<String> parameterList;

	public String getJar() {
		return jar;
	}

	public void setJar(String jar) {
		this.jar = jar;
	}

	public ArrayList<String> getParameterList() {
		return parameterList;
	}

	public void setParameterList(ArrayList<String> parameterList) {
		this.parameterList = parameterList;
	}

	@Override
	public List<String> getFilesNeeded() {
		List<String> returnList = new LinkedList<>();
		for (String potentialFile : getParameterList()) {
			String path = PTD_CONFIG_DIR + potentialFile;
			File tempFile = new File(path);
			if (tempFile.exists()) {
				returnList.add(path);
			}
		}
		return returnList;
	}
}
