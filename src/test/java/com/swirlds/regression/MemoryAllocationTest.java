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

package com.swirlds.regression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MemoryAllocationTest {


	@ParameterizedTest
	@ValueSource(strings = {
			"8GB",
			"140MB",
			"12304KB",
			"8 GB", // Space in between size and memory type
			"8,192MB", // comma added
			"8_192MB" // underscore added
	})
	@DisplayName("Test Memory Allocation Constructor with good inputs")
	public void testNodeMemoryConstructor(String totalMemory) throws URISyntaxException, IOException {

		//TODO should be helper function, or should be static function in MemoryAllocation
		totalMemory = totalMemory.replaceAll("[^a-zA-Z0-9]", "");
		String[] seperatedMemStr = totalMemory.split("(?<=\\d)(?=\\D)");
		MemoryAllocation testNM = new MemoryAllocation(totalMemory);
		int amount = Integer.valueOf(seperatedMemStr[0]);
		String size = seperatedMemStr[1];
		assertEquals(amount, testNM.getRawMemoryAmount());
		assertEquals(size, testNM.getMemoryType().getMemoryIdent());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"GB8", // wrong order
			"140BM", // incorrect memory type
			"8MB extra stuff", // correct allocation string, but with extra
			"123 Hello world", // spaces and incorrect memory
			"L3t's @dd s0m3 tru1y cr@zy sTuff h3r3!#." // test the extreme case
	})
	@DisplayName("Test Memory Allocation Constructor with malformed inputs")
	public void testMalformedNodeMemoryConstructor(String totalMemory) throws NumberFormatException {
		assertThrows(IllegalArgumentException.class, () -> {
			MemoryAllocation testNM = new MemoryAllocation(totalMemory);
		});

	}

	@ParameterizedTest(name = "{index} => cuurent=''{0}'' new=''{1}''")
	@MethodSource("memoryTypeProvider")
	//@Test
	@DisplayName("Test Calculate multiplier function ")
	public void testCalculateMultiplier(MemoryType currentMemType, MemoryType newMemType) {
		//public void testCalculateMultiplier()  {
		MemoryAllocation testNM = new MemoryAllocation(64, currentMemType);
		double returnedValue = testNM.CalculateMultiplier(newMemType, currentMemType);
		System.out.println(returnedValue);
		assertEquals(Math.pow(1024, currentMemType.ordinal() - newMemType.ordinal()), returnedValue);
	}

	@ParameterizedTest(name = "{index} => cuurent=''{0}'' new=''{1}''")
	@MethodSource("memoryTypeProvider")
	//@Test
	@DisplayName("Test Calculate multiplier function ")
	public void testGetAdjustedMemoryAmount(MemoryType currentMemType, MemoryType newMemType) {
		//public void testCalculateMultiplier()  {
		MemoryAllocation testNM = new MemoryAllocation(64, currentMemType);
		double returnedValue = testNM.getAdjustedMemoryAmount(newMemType);
		System.out.println(returnedValue);
		assertEquals(Math.pow(1024, currentMemType.ordinal() - newMemType.ordinal()) * testNM.getRawMemoryAmount(), returnedValue);
	}

	static Stream<Arguments> memoryTypeProvider() {
		Stream.Builder<Arguments> argumentBuilder = Stream.builder();
		for (MemoryType memType1 : MemoryType.values()) {
			for (MemoryType memType2 : MemoryType.values()) {
				argumentBuilder.add(Arguments.of(memType1, memType2));
			}
		}
		return argumentBuilder.build();
	}

}
