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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogReaderTest {

	@Test
	void readLogFile() throws IOException {
		LogReader logReader = LogReader.createReader(1,
				LogReaderTest.class.getClassLoader().getResourceAsStream("swirlds.log"));

		LogEntry firstEntry = logReader.nextEntry();
		assertEquals(618, firstEntry.getThreadId());
		assertEquals(LogMarkerInfo.STARTUP, firstEntry.getMarker());
		assertEquals("main Browser main() started", firstEntry.getLogEntry());

		LogEntry fallenBehindEntry = logReader.nextEntryContaining("has fallen behind");
		assertEquals(120825, fallenBehindEntry.getThreadId());
		assertEquals(LogMarkerInfo.RECONNECT, fallenBehindEntry.getMarker());
		assertEquals("3 has fallen behind, will stop and clear EventFlow and Hashgraph",
				fallenBehindEntry.getLogEntry());

		List<LogEntry> exceptions = logReader.getExceptions();
		assertEquals(8, exceptions.size());

		LogEntry exception = exceptions.get(0);
		assertEquals(LogMarkerInfo.SOCKET_EXCEPTIONS, exception.getMarker());
		assertEquals(true, exception.isException());
		assertEquals("3 didn't receive anything from 1 for 5005 ms. Disconnecting...", exception.getLogEntry());
	}

}