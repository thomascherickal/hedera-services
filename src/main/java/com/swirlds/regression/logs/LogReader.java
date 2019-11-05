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

package com.swirlds.regression.logs;

import com.swirlds.regression.RegressionUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LogReader {
	private BufferedReader fileReader;
	private LogParser parser;

	List<LogEntry> exceptions = new LinkedList<>();
	long exceptionCount = 0;

	LogEntry firstEntry = null;
	LogEntry lastEntryRead = null;

	private LogReader(BufferedReader fileReader, LogParser parser) {
		this.fileReader = fileReader;
		this.parser = parser;
	}

	public static LogReader createReader(int logVersion, InputStream fileStream) {
		return new LogReader(new BufferedReader(new InputStreamReader(fileStream)), new LogParserV1());
	}

	public String nextLine() throws IOException {
		return fileReader.readLine();
	}

	public LogEntry nextEntry() throws IOException {
		while (true) {
			String line = nextLine();
			if (line == null) {
				return null;
			}
			LogEntry entry = parser.parse(line);
			if (entry != null) {
				if (firstEntry == null) {
					firstEntry = entry;
				}
				if (entry.isException()) {
					if (exceptions.size() < RegressionUtilities.EXCEPTIONS_SIZE) {
						exceptions.add(entry);
					}
					exceptionCount++;
				}
				lastEntryRead = entry;
				return entry;
			}
		}
	}

	public LogEntry nextEntryContaining(String s) throws IOException {
		return nextEntryContaining(Arrays.asList(s));
	}

	public LogEntry nextEntryContaining(List<String> strings) throws IOException {
		while (true) {
			LogEntry entry = nextEntry();
			if (entry == null) {
				return null;
			}
			boolean result = strings.stream().anyMatch(x -> entry.getLogEntry().contains(x) );
			if (result){
				return  entry;
			}
		}
	}

	public void readFully() throws IOException {
		while (nextEntry() != null) ;
	}

	public long getExceptionCount() {
		return exceptionCount;
	}

	public List<LogEntry> getExceptions() {
		return exceptions;
	}

	public LogEntry getFirstEntry() {
		return firstEntry;
	}

	public LogEntry getLastEntryRead() {
		return lastEntryRead;
	}
}
