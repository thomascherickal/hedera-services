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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JVMConfigTest {


	@ParameterizedTest
	@ValueSource(strings = {
			"-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages "
					+ "-Xmx100g -Xms8g -XX:ZMarkStackSpaceLimit=16g -XX:MaxDirectMemorySize=32g"
	})
	@DisplayName("Test default constructor of JVM Config class")
	public void testJVMConfigDefualtConstructor(String compareTo){
		JVMConfig jvmConfig = new JVMConfig();
		assertEquals(compareTo,jvmConfig.getJVMOptionsString());
	}

	@ParameterizedTest
	@CsvSource({
//			// MaxJVM, expected String
			"16GB, -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx16g -Xms2g -XX:ZMarkStackSpaceLimit=16g -XX:MaxDirectMemorySize=12g",
			"12GB, -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx12g -Xms2g -XX:ZMarkStackSpaceLimit=9g -XX:MaxDirectMemorySize=9g",
			"4GB, -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx4g -Xms1g -XX:ZMarkStackSpaceLimit=3g -XX:MaxDirectMemorySize=3g",
			"4MB, -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx4m -Xms1m -XX:ZMarkStackSpaceLimit=3m -XX:MaxDirectMemorySize=3m",
			"4096KB, -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx4096k -Xms410k -XX:ZMarkStackSpaceLimit=3072k -XX:MaxDirectMemorySize=3072k",
	})
	@DisplayName("Test constructor using default values, but to little maximum memory")
	public void testJVMConfigConstructorDefaultValues(String maxMemory, String expectedJVMOptions) throws URISyntaxException, IOException {

		JVMConfig testJVM = new JVMConfig(new MemoryAllocation(maxMemory));
		assertEquals(expectedJVMOptions, testJVM.getJVMOptionsString());
	}

	@ParameterizedTest
	@CsvSource({
//			// MaxJVM, expected String
			"-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx8g -Xms2g -XX:ZMarkStackSpaceLimit=6g -XX:MaxDirectMemorySize=6g",
			"-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx12g -Xms2g -XX:ZMarkStackSpaceLimit=9g -XX:MaxDirectMemorySize=9g",
			"-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx4g -Xms1g -XX:ZMarkStackSpaceLimit=3g -XX:MaxDirectMemorySize=3g",
			"-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx4m -Xms1m -XX:ZMarkStackSpaceLimit=3m -XX:MaxDirectMemorySize=3m",
			"-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:ConcGCThreads=14 -XX:+UseLargePages -Xmx4096k -Xms410k -XX:ZMarkStackSpaceLimit=3072k -XX:MaxDirectMemorySize=3072k",
	})
	@DisplayName("Test constructor that accepts string of parameters")
	public void testJVMConfigParameterConstructorValues( String expectedJVMOptions) throws URISyntaxException, IOException {

		JVMConfig testJVM = new JVMConfig(expectedJVMOptions);
		assertEquals(expectedJVMOptions, testJVM.getJVMOptionsString());
	}

//	@ParameterizedTest
//	@ValueSource(strings = {
//			"GB8", // wrong order
//			"140BM", // incorrect memory type
//			"8MB extra stuff", // correct allocation string, but with extra
//			"123 Hello world", // spaces and incorrect memory
//			"L3t's @dd s0m3 tru1y cr@zy sTuff h3r3!#." // test the extreme case
//	})
//	@DisplayName("Test generic config json file parsing")
//	public void testMalformedNodeMemoryConstructor(String totalMemory) throws NumberFormatException {
//		assertThrows(IllegalArgumentException.class, () -> {
//			NodeMemory testNM = new NodeMemory(totalMemory);
//		});
//	}

//	@ParameterizedTest
//	@CsvSource({
//			// totalmemory, hugepage number, hugepage memory, JVM Memory
//			"32GB, 15360, 30720, 28GB",
//			"64GB, 31744, 63488, 60GB"
//	})
//	@DisplayName("Test Calculations made by constructor are correct")
//	public void testNodeMemoryContructorCalculations(String totalMemory, int hugePageNumber, int hugePageKBMemory,
//			String jvmMemory) throws NumberFormatException {
//		NodeMemory testNM = new NodeMemory(totalMemory);
//		assertEquals(hugePageNumber, testNM.getHugePagesNumber());
//		assertEquals(hugePageKBMemory, (int) testNM.getHugePagesMemory().getAdjustedMemoryAmount(MemoryType.MB));
//		assertEquals(RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS,
//				testNM.getPostgresMaxPreparedTransaction());
//		assertEquals(new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS),
//				testNM.getPostgresTempBuffers());
//		assertEquals(new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM), testNM.getPostgresWorkMem());
//		assertEquals(new MemoryAllocation(jvmMemory), testNM.getJvmMemory());
//		/* TODO test postgres Shared_buffer */
//	}

}
