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

package com.swirlds.regression.logs;

import com.swirlds.regression.RegressionUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LogReader<T extends LogEntry> {
	private BufferedReader fileReader;
	private LogParser<T> parser;

	List<T> exceptions = new LinkedList<>();
	long exceptionCount = 0;

	T firstEntry = null;
	T lastEntryRead = null;

	private LogReader(LogParser<T> parser, InputStream fileStream) {
		this.fileReader = new BufferedReader(new InputStreamReader(fileStream));
		this.parser = parser;
	}

	public static <T extends LogEntry> LogReader<T> createReader(LogParser<T> parser, InputStream fileStream) {
		return new LogReader<T>(parser, fileStream);
	}

	public String nextLine() throws IOException {
		return fileReader.readLine();
	}

	public T nextEntry() throws IOException {
		while (true) {
			String line = nextLine();
			if (line == null) {
				return null;
			}
			//skip empty lines
			if (line.strip().isEmpty()){
				continue;
			}
			T entry = parser.parse(line);
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

	public T nextEntryContaining(String s) throws IOException {
		return nextEntryContaining(Arrays.asList(s));
	}

	public T nextEntryContaining(List<String> strings) throws IOException {
		while (true) {
			T entry = nextEntry();
			if (entry == null) {
				return null;
			}
			boolean result = strings.stream().anyMatch(x -> entry.getLogEntry().contains(x));
			if (result) {
				return entry;
			}
		}
	}

	public void readFully() throws IOException {
		while (nextEntry() != null) ;
	}

	public long getExceptionCount() {
		return exceptionCount;
	}

	public List<T> getExceptions() {
		return exceptions;
	}

	public T getFirstEntry() {
		return firstEntry;
	}

	public T getLastEntryRead() {
		return lastEntryRead;
	}
}
