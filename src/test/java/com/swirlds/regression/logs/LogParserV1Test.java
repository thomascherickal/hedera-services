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

import com.swirlds.common.logging.PlatformLogMarker;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogParserV1Test {

	@Test
	void parse() {
		String line = "2019-03-14 15:52:04.152 RECONNECT 134128 <      syncCaller  3  0> 3 sent commStateRequest to 1";
		Instant time = Instant.parse("2019-03-14T15:52:04.152Z");
		long millis = 134128;
		String threadName = "<      syncCaller  3  0>";
		String msg = "3 sent commStateRequest to 1";
		LogParserV1 parser = new LogParserV1();
		LogEntry entry = parser.parse(line);

		assertNotNull(entry);
		assertEquals(time, entry.getTime());
		assertEquals(PlatformLogMarker.RECONNECT, entry.getMarker());
		assertEquals(millis, entry.getThreadId());
		assertEquals(threadName, entry.getThreadName());
		assertEquals(msg, entry.getLogEntry());
		assertEquals(false, entry.isException());

	}

	@Test
	void parseNoAngleBrackets() {
		String line = "2019-04-16 13:11:52.298 DEMO_INFO 43 checkThread_node0 TEST SUCCESS: Quit checking";
		Instant time = Instant.parse("2019-04-16T13:11:52.298Z");
		long threadId = 43;
		String threadName = "checkThread_node0";
		String msg = "TEST SUCCESS: Quit checking";
		LogParserV1 parser = new LogParserV1();
		LogEntry entry = parser.parse(line);

		assertNotNull(entry);
		assertEquals(time, entry.getTime());
		assertEquals(PlatformLogMarker.DEMO_INFO, entry.getMarker());
		assertEquals(threadId, entry.getThreadId());
		assertEquals(threadName, entry.getThreadName());
		assertEquals(msg, entry.getLogEntry());
		assertEquals(false, entry.isException());

	}

	@Test
	void parseNewLine(){
		String line = "2019-04-09 19:07:00.269  83789 <       stateHash  0   > Freeze state is about to be saved to disk, round is 486";
		Instant time = Instant.parse("2019-04-09T19:07:00.269Z");
		long millis = 83789;
		String threadName = "<       stateHash  0   >";
		String msg = "Freeze state is about to be saved to disk, round is 486";

		LogParserV2 parser = new LogParserV2();
		LogEntry entry = parser.parse(line);

		assertNotNull(entry);
		assertEquals(time, entry.getTime());
		// assertEquals(PlatformLogMarker.RECONNECT, entry.getMarker());
		assertEquals(millis, entry.getThreadId());
		assertEquals(threadName, entry.getThreadName());
		assertEquals(msg, entry.getLogEntry());
		assertEquals(false, entry.isException());
	}

	@Test
	void parseExceptionLine(){
		String line = "2019-04-02 22:03:34.254  23189 < pollIntakeQueue  0   > Exception: 0 Received invalid state " +
				"signature! round:48 memberId:3 details:";
		Instant time = Instant.parse("2019-04-02T22:03:34.254Z");
		long millis = 23189;
		String threadName = "< pollIntakeQueue  0   >";
		String msg = "Exception: 0 Received invalid state signature! round:48 memberId:3 details:";

		LogParserV2 parser = new LogParserV2();
		LogEntry entry = parser.parse(line);

		assertNotNull(entry);
		assertEquals(time, entry.getTime());
		// assertEquals(PlatformLogMarker.RECONNECT, entry.getMarker());
		assertEquals(millis, entry.getThreadId());
		assertEquals(threadName, entry.getThreadName());
		assertEquals(msg, entry.getLogEntry());
		assertEquals(true, entry.isException());
	}

	@Test
	void parseConnectionExceptionLine(){
		String line = "2019-04-02 22:03:30.806  19741 <       heartbeat  0  2> 0 failed to connect to 2 with error: java.net.ConnectException: Connection refused (Connection refused)";
		Instant time = Instant.parse("2019-04-02T22:03:30.806Z");
		long millis = 19741;
		String threadName = "<       heartbeat  0  2>";
		String msg = "0 failed to connect to 2 with error: java.net.ConnectException: Connection refused (Connection refused)";

		LogParserV2 parser = new LogParserV2();
		LogEntry entry = parser.parse(line);

		assertNotNull(entry);
		assertEquals(time, entry.getTime());
		// assertEquals(PlatformLogMarker.RECONNECT, entry.getMarker());
		assertEquals(millis, entry.getThreadId());
		assertEquals(threadName, entry.getThreadName());
		assertEquals(msg, entry.getLogEntry());
		assertEquals(true, entry.isException());
	}
}