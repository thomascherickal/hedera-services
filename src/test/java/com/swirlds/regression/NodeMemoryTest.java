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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		String[] separatedMemStr = totalMemory.split("(?<=\\d)(?=\\D)");
		NodeMemory testNM = new NodeMemory(totalMemory);
		int amount = Integer.parseInt(separatedMemStr[0]);
		String size = separatedMemStr[1];
		assertEquals(amount, testNM.getTotalMemory().getRawMemoryAmount());
		assertEquals(size, testNM.getTotalMemory().getMemoryType().getMemoryIdent());
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

	@ParameterizedTest
	@CsvSource({
			/* totalmemory, hugepage number, hugepage memory, JVM Memory, temp buffers, prep transactions, work memory
			   maint work memory, autov work memory*/
			"4GB, 1024, 2048, 1GB, 64MB, 100, 64MB, 32MB, 32MB, 512MB",
			"6GB, 1024, 2048, 1GB, 64MB, 100, 64MB, 32MB, 32MB, 512MB",
			"8GB, 3604, 7208, 5GB, 256MB, 100, 256MB, 128MB, 128MB, 1024MB",
			"16GB, 7208, 14416, 10GB, 256MB, 100, 256MB, 128MB, 128MB, 2GB",
			"32GB, 14417, 28834, 20GB, 256MB, 100, 256MB, 128MB, 128MB, 4GB",
			"64GB, 28835, 57670, 40GB, 256MB, 100, 256MB, 128MB, 128MB, 8GB",
			"128GB, 57671, 115342, 80GB, 4GB, 500, 4GB, 2GB, 2GB, 16GB",
			"160GB, 72089, 144178, 100GB, 4GB, 500, 4GB, 2GB, 2GB, 20GB",
			"192GB, 72089, 144178, 100GB, 4GB, 500, 4GB, 2GB, 2GB, 20GB"
	})
	@DisplayName("Test Calculations made by constructor are correct")
	public void testNodeMemoryConstructorCalculations(String totalMemory, int hugePageNumber, int hugePageKBMemory,
			String jvmMemory, String tempBuffers, int prepTransactions, String workMemory, String maintWorkMemory,
		    String autovWorkMemory, String sharedBuffers) throws NumberFormatException {
		NodeMemory testNM = new NodeMemory(totalMemory);
		assertEquals(hugePageNumber, testNM.getHugePagesNumber());
		assertEquals(hugePageKBMemory, (int) testNM.getHugePagesMemory().getAdjustedMemoryAmount(MemoryType.MB));
		assertEquals(prepTransactions, testNM.getPostgresMaxPreparedTransaction());
		assertEquals(new MemoryAllocation(tempBuffers), testNM.getPostgresTempBuffers());
		assertEquals(new MemoryAllocation(workMemory), testNM.getPostgresWorkMem());
		assertEquals(new MemoryAllocation(jvmMemory), testNM.getJvmMemory());
		assertEquals(new MemoryAllocation(maintWorkMemory), testNM.getPostgresMaintWorkMem());
		assertEquals(new MemoryAllocation(autovWorkMemory), testNM.getPostgresAutovWorkMem());
		assertEquals(new MemoryAllocation(sharedBuffers), testNM.getPostgresSharedBuffers());
	}

}
