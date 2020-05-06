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

public class LogParserV2 implements PlatformLogParser {
	/* matches the following style line:
	  2019-04-09 19:07:00.269  83789 <       stateHash  0   > Freeze state is about to be saved to disk, round is 486
	  match(1) = datetimeRegex = 2019-04-09 19:07:00.269
	  match(2) = objMillisRegex = 83789
	  match(3) = threadRegex = <       stateHash  0   >
	  match(4) = tilEndRegex = Freeze state is about to be saved to disk, round is 486
	*/

	private static String regex =
			DATETIME_REGEX + SPACE_BETWEEN_REGEX + OBJ_MILLISECOND_REGEX + SPACE_BETWEEN_REGEX + THREAD_REGEX +
					SPACE_BETWEEN_REGEX + TILL_END_REGEX;
	private static Pattern pattern = Pattern.compile(regex);

	private DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public LogParserV2() {
		timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public PlatformLogEntry parse(String line) {
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			Instant timestamp;
			try {
				timestamp = timestampFormat.parse(matcher.group(1)).toInstant();
			} catch (ParseException e) {
				return null;
			}
			LogMarkerInfo marker = null;
			long threadId;
			try {
				threadId = Long.parseLong(matcher.group(2));
			} catch (NumberFormatException e) {
				return null;
			}
			String threadName = (matcher.group(3).trim());
			String entry = matcher.group(4);
			return new PlatformLogEntry(
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
				|| s.contains("exception")
				|| (s.contains("error") && !s.contains("error=false") && !s.toLowerCase().contains("iserrored"));
	}
}
