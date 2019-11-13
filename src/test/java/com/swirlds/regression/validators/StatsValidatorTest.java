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

package com.swirlds.regression.validators;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.jsonConfigs.TestConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatsValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/FCM1KThrottling-1",
			"logs/FCM1KThrottling-2",
			"logs/FCM1KThrottling-3"
	})
	void statsValidatorTest(String testDir) throws IOException {
		List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "PlatformTesting", 1);

		Path testConfigFileLocation = Paths.get("configs/testFcm1kThrottlingCfg.json");
		byte[] jsonData = Files.readAllBytes(testConfigFileLocation);
		ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		TestConfig testConfig = objectMapper.readValue(jsonData, TestConfig.class);

		StatsValidator validator = new StatsValidator(nodeData, testConfig);
		validator.PTDJsonConfigFilePath = "src/test/resources/" + testDir + "/";
		validator.validate();
		System.out.println("LOGS: " + testDir);
		System.out.println(validator.concatAllMessages());
		assertEquals(true, validator.isValid());
	}
}
