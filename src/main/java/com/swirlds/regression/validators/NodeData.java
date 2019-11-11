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

import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.logs.LogReader;

import java.io.InputStream;

public class NodeData {
	private LogReader logReader;
	private CsvReader csvReader;
	private InputStream recoverEventMatchLog = null;

	public NodeData(LogReader logReader, CsvReader csvReader, InputStream recoverEventMatchLog) {
		this.logReader = logReader;
		this.csvReader = csvReader;
		this.recoverEventMatchLog = recoverEventMatchLog;
	}

	public NodeData(LogReader logReader, CsvReader csvReader) {
		this.logReader = logReader;
		this.csvReader = csvReader;
	}

	public LogReader getLogReader() {
		return logReader;
	}

	public CsvReader getCsvReader() {
		return csvReader;
	}

	public InputStream getRecoverEventMatchLog() {
		return recoverEventMatchLog;
	}
}
