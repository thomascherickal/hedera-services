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
import com.swirlds.regression.logs.services.HAPIClientLogEntry;

public class HapiClientData {
	private LogReader<HAPIClientLogEntry> hapiClientLogReader;

	/**
	 * Used for services regression to validate test client node logs
	 *
	 * @param logReader
	 */
	public HapiClientData(LogReader<HAPIClientLogEntry> logReader) {
		this.hapiClientLogReader = logReader;
	}

	public LogReader<HAPIClientLogEntry> getHapiClientLogReader() {
		return hapiClientLogReader;
	}
}
