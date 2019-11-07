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

import com.swirlds.regression.jsonConfigs.AppConfig;
import com.swirlds.regression.jsonConfigs.FileRequirement;

import java.util.LinkedList;
import java.util.List;

public class RecoverConfig implements FileRequirement {

	/** Application configuration used in recover mode */
	private AppConfig app;

	public AppConfig getApp() {
		return app;
	}

	public void setApp(AppConfig app) {
		this.app = app;
	}

	@Override
	public List<String> getFilesNeeded() {
		return new LinkedList<>(app.getFilesNeeded());
	}
}
