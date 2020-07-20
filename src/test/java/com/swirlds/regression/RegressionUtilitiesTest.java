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

import com.swirlds.regression.jsonConfigs.JvmOptionParametersConfig;
import com.swirlds.regression.jsonConfigs.MemoryLeakCheckConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.test.framework.TestComponentTags;
import com.swirlds.test.framework.TestTypeTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static com.swirlds.regression.jsonConfigs.NodeGroupIdentifier.FIRST_AND_LAST_STAKED_NODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegressionUtilitiesTest {
	String ExpectedString = "-Xmx%dg -Xms%dg -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
			"-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=%dg";

	@ParameterizedTest
	@CsvSource({
			/* int maxMemory = 32; int minMemory = 4; int maxDirectMemory = 8;*/
			"32,4,8",
			"1,1,1",
			"10, 1, 2"
	})
	@DisplayName("Test building jvm options string based on parameters")
	public void testBuildParameterStringWithParameters(int maxMemory, int minMemory, int maxDirectMemory) {
		String currentExpectedString = String.format(ExpectedString, maxMemory, minMemory, maxDirectMemory);

		assertTrue(currentExpectedString.equals(
				RegressionUtilities.buildParameterString(maxMemory, minMemory, maxDirectMemory)));
	}

	@DisplayName("build options string with bad params")
	@ParameterizedTest
	@CsvSource({
			"0,-2,-32",
			"32,4,-1",
			"32,-1,8",
			"-1,4,8" })
	public void testBuildParameterStringWithBadParameters(int maxMemory, int minMemory, int maxDirectMemory) {
		assertTrue(RegressionUtilities.JVM_OPTIONS_DEFAULT.equals(
				RegressionUtilities.buildParameterString(maxMemory, minMemory, maxDirectMemory)));
	}

	@ParameterizedTest
	@CsvSource({
			/* int maxMemory = 32; int minMemory = 4; int maxDirectMemory = 8;*/
			"32,4,8",
			"1,1,1",
			"10, 1, 2"
	})
	@DisplayName("Test building jvm options string based on json config")
	public void testBuildParametersStringWithConfig(int maxMemory, int minMemory, int maxDirectMemory) {
		JvmOptionParametersConfig config = new JvmOptionParametersConfig();
		config.setMaxMemory(maxMemory);
		config.setMinMemory(minMemory);
		config.setMaxDirectMemory(maxDirectMemory);

		String currentExpectedString = String.format(ExpectedString, maxMemory, minMemory, maxDirectMemory);

		assertTrue(currentExpectedString.equals(RegressionUtilities.buildParameterString(config)));
	}

	@Test
	@DisplayName("build JVM options string with Null JSON config")
	public void testBuildParametersStringWithNullConfig() {
		assertTrue(RegressionUtilities.JVM_OPTIONS_DEFAULT.equals(RegressionUtilities.buildParameterString(null)));
	}

	@ParameterizedTest
	@DisplayName("build options string with bad params")
	@CsvSource({
			"0,-2,-32",
			"32,4,-1",
			"32,-1,8",
			"-1,4,8" })
	public void testBuildParameterStringWithBadJson(int maxMemory, int minMemory, int maxDirectMemory) {
		JvmOptionParametersConfig config = new JvmOptionParametersConfig();
		config.setMaxMemory(maxMemory);
		config.setMinMemory(minMemory);
		config.setMaxDirectMemory(maxDirectMemory);
		assertTrue(RegressionUtilities.JVM_OPTIONS_DEFAULT.equals(
				RegressionUtilities.buildParameterString(maxMemory, minMemory, maxDirectMemory)));
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Load a MemoryLeakCheckConfig which specifies nodesToCheck")
	public void loadMemoryLeakCheckConfig_Array_Test() throws Exception {
		// "nodesToCheck": [0, 3]
		TestConfig testConfig =  TestUtils.loadTestConfigFromTestResource("logs/MemoryLeak/testCfg-NodesToCheck.json");
		MemoryLeakCheckConfig memoryLeakCheckConfig = testConfig.getMemoryLeakCheckConfig();
		assertNotNull(memoryLeakCheckConfig);
		assertNull(memoryLeakCheckConfig.getNodeGroupIdentifier());
		Set<Integer> nodesToCheck = memoryLeakCheckConfig.getNodesToCheck();
		assertEquals(2, nodesToCheck.size());
	}

	@Test
	@Tag(TestTypeTags.FUNCTIONAL)
	@Tag(TestComponentTags.VALIDATOR)
	@DisplayName("Load a MemoryLeakCheckConfig which specifies nodeGroupIdentifier")
	public void loadMemoryLeakCheckConfig_NodeGroup_Test() throws Exception {
		TestConfig testConfig = TestUtils.loadTestConfigFromTestResource("logs/MemoryLeak/testCfg-NodeGroup.json");
		MemoryLeakCheckConfig memoryLeakCheckConfig = testConfig.getMemoryLeakCheckConfig();
		assertTrue(memoryLeakCheckConfig != null);
		assertEquals(FIRST_AND_LAST_STAKED_NODE, memoryLeakCheckConfig.getNodeGroupIdentifier());
		assertNull(memoryLeakCheckConfig.getNodesToCheck());
	}
}
