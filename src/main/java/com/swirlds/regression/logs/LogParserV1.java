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

import com.swirlds.common.logging.LogMarkerInfo;
import com.swirlds.common.logging.LogMarkerType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParserV1 implements LogParser {
	/* matches the following style line:
	  "2019-03-14 15:52:04.152 RECONNECT 134128 <      syncCaller  3  0> 3 sent commStateRequest to 1"
	  match(1) = DATETIME_REGEX = 			2019-04-09 19:07:00.269
	  match(2) = MARKER_REGEX =  			RECONNECT
	  match(3) = OBJ_MILLISECOND_REGEX = 	134128
	  match(4) = THREAD_REGEX = 			syncCaller  3  0
	  match(5) = TILL_END_REGEX = 			3 sent commStateRequest to 1
	*/

	private static String regex =
			DATETIME_REGEX + SPACE_BETWEEN_REGEX + MARKER_REGEX + SPACE_BETWEEN_REGEX + OBJ_MILLISECOND_REGEX +
					SPACE_BETWEEN_REGEX + THREAD_REGEX + SPACE_BETWEEN_REGEX + TILL_END_REGEX;
	private static Pattern pattern = Pattern.compile(regex);

	private DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public LogParserV1() {
		timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public LogEntry parse(String line) {
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			Instant timestamp;
			try {
				timestamp = timestampFormat.parse(matcher.group(1)).toInstant();
			} catch (ParseException e) {
				return null;
			}
			LogMarkerInfo marker = null;
			try {
				marker = LogMarkerInfo.valueOf(matcher.group(2));
			} catch (IllegalArgumentException e) {
			}
			long threadId;
			try {
				threadId = Long.parseLong(matcher.group(3));
			} catch (NumberFormatException e) {
				return null;
			}
			String threadName = matcher.group(4);
			String entry = matcher.group(5);

			return new LogEntry(
					timestamp,
					marker,
					threadId,
					threadName,
					entry,
					isException(marker, entry)
			);
		}
		return null;
	}

	private static boolean isException(LogMarkerInfo marker, String s) {
		s = s.toLowerCase();
		return (marker != null &&
				(marker.getType() == LogMarkerType.EXCEPTION ||
						marker.getType() == LogMarkerType.ERROR))
				|| s.contains("exception") || s.contains("error");
	}
}
