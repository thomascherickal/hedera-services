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

public interface PlatformLogParser extends LogParser<PlatformLogEntry> {
	String DATETIME_REGEX = "(^[0-9\\-]+ [0-9:.]+)";
	String OBJ_MILLISECOND_REGEX = "([0-9]+)";
	String THREAD_REGEX = "(< +[a-zA-Z_0-9: ]+ *>|\\S+)";
	String TILL_END_REGEX = "(.+)";
	String MARKER_REGEX = "(\\S+)";
	String SPACE_BETWEEN_REGEX = " +";

	static LogParser<PlatformLogEntry> createParser(int version){
		switch (version){
			case 1:
				return new LogParserV1();
			case 2:
				return new LogParserV2();
			default:
				throw new IllegalArgumentException("Unsupported version");
		}
	}
}
