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
import com.swirlds.regression.logs.PlatformLogEntry;
import com.swirlds.regression.logs.StdoutLogEntry;

public class NodeData {
	private LogReader<PlatformLogEntry> logReader;
	private CsvReader csvReader;
	private LogReader<StdoutLogEntry> stdoutReader;

	public NodeData(LogReader<PlatformLogEntry> logReader, CsvReader csvReader) {
		this(logReader, csvReader, null);
	}

	public NodeData(LogReader<PlatformLogEntry> logReader) {
		this(logReader, null, null);
	}

	public NodeData(LogReader<PlatformLogEntry> logReader, CsvReader csvReader,
			LogReader<StdoutLogEntry> stdoutReader) {
		this.logReader = logReader;
		this.csvReader = csvReader;
		this.stdoutReader = stdoutReader;
	}

	public LogReader<PlatformLogEntry> getLogReader() {
		return logReader;
	}

	public CsvReader getCsvReader() {
		return csvReader;
	}

	public LogReader<StdoutLogEntry> getStdoutReader() {
		return stdoutReader;
	}
}
