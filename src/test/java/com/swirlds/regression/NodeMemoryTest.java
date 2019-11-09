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

package com.swirlds.regression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class NodeMemoryTest {


	@ParameterizedTest
	@ValueSource(strings = {
			"8GB",
			"140MB",
			"12304KB",
			"8 GB", // Space in between size and memory type
			"8,192MB", // comma added
			"8_192MB" // underscore added
	})
	@DisplayName("Test generic config json file parsing")
	public void testNodeMemoryConstructor(String totalMemory) throws URISyntaxException, IOException {

		//TODO should be helper function, or should be static function in MemoryAllocation
		totalMemory = totalMemory.replaceAll("[^a-zA-Z0-9]", "");
		String[] seperatedMemStr = totalMemory.split("(?<=\\d)(?=\\D)");
		NodeMemory testNM = new NodeMemory(totalMemory);
		int amount = Integer.valueOf(seperatedMemStr[0]);
		String size = seperatedMemStr[1];
		assertEquals(amount, testNM.totalMemory.getRawMemoryAmount());
		assertEquals(size, testNM.totalMemory.getMemoryType().getMemoryIdent());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"GB8", // wrong order
			"140BM", // incorrect memory type
			"8MB extra stuff", // correct allocation string, but with extra
			"123 Hello world", // spaces and incorrect memory
			"L3t's @dd s0m3 tru1y cr@zy sTuff h3r3!#." // test the extreme case
	})
	@DisplayName("Test generic config json file parsing")
	public void testMalformedNodeMemoryConstructor(String totalMemory) throws NumberFormatException {
		assertThrows(IllegalArgumentException.class, () -> {
			NodeMemory testNM = new NodeMemory(totalMemory);
		});

	}

}
