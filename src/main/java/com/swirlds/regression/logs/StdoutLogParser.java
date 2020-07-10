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

public class StdoutLogParser implements LogParser<StdoutLogEntry> {
	@Override
	public StdoutLogEntry parse(String line) {
		return new StdoutLogEntry(line, isError(line));
	}

	private static boolean isError(String s) {
		s = s.toLowerCase();
		//TODO that message occurs with an attempt to load the GPU processing,
		// but since we removed it we are no longer uploading the file to remoteExperiment.
		// This will be removed once ticket in regression is addressed.
		if(s.contains("could not load libopencl.so, error libopencl.so: " +
				"cannot open shared object file: no such file or directory")){
			return false;
		}
		return s.contains("exception") || (s.contains("error"));
	}
}
