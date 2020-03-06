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

package com.swirlds.regression.jsonConfigs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CloudConfigTest {
	public static CloudConfig loadCloudConfig(String filePath) throws URISyntaxException, IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URI regressionConfigFileLocation = classloader.getResource(filePath).toURI();

		byte[] jsonData = Files.readAllBytes(Paths.get(regressionConfigFileLocation));
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		objectMapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES,false);
		//objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

		CloudConfig regCofig = objectMapper.readValue(jsonData, CloudConfig.class);

		return regCofig;
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/CloudConfigTest/BaseCloud.json",
	})
	@DisplayName("Test generic config json file parsing")
	public void testConfigParsing(String configFile) throws URISyntaxException, IOException {
		CloudConfig cloud = loadCloudConfig(configFile);
		assertNotNull(cloud.getRegionList());
		List<RegionList> listOfRegions = cloud.getRegionList();
		for(RegionList rl : listOfRegions) {
			assertNotNull(rl.getRegion());
			assertNotEquals("", rl.getRegion());
			assertNotEquals(-1, rl.getNumberOfNodes());
		}
		assertNotNull(cloud.getKeyLocation());
		assertNotNull(cloud.getInstanceKey());
		assertNotNull(cloud.getInstanceName());
		assertNotNull(cloud.getInstanceType());
		assertNotNull(cloud.getLogin());
		assertNotNull(cloud.getSecurityGroup());
//		assertNotNull(cloud.getSecurityGroupID());
		assertNotNull(cloud.getService());

		assertNotEquals("", cloud.getKeyLocation());
		assertNotEquals("", cloud.getInstanceKey());
		assertNotEquals("", cloud.getInstanceName());
		assertNotEquals("", cloud.getInstanceType());
		assertNotEquals("", cloud.getLogin());
		assertNotEquals("", cloud.getSecurityGroup());
//		assertNotEquals("", cloud.getSecurityGroupID());
		assertNotEquals("", cloud.getService());
	}

	@Test
	@DisplayName("Test BaseCloud.json imported all values properly")
	public void testBaseConfigParsing() throws IOException, URISyntaxException {
		String configFile = "configs/CloudConfigTest/BaseCloud.json";

		CloudConfig cloud = loadCloudConfig(configFile);
		assertNotNull(cloud.getRegionList());
		List<RegionList> listOfRegions = cloud.getRegionList();
		for(RegionList rl : listOfRegions) {
			assertNotNull(rl.getRegion());
			assertNotEquals("", rl.getRegion());
			assertNotEquals(-1, rl.getNumberOfNodes());
		}
		assertNotNull(cloud.getKeyLocation());
		assertNotNull(cloud.getInstanceKey());
		assertNotNull(cloud.getInstanceName());
		assertNotNull(cloud.getInstanceType());
		assertNotNull(cloud.getLogin());
		assertNotNull(cloud.getSecurityGroup());
//		assertNotNull(cloud.getSecurityGroupID());
		assertNotNull(cloud.getService());

		assertEquals("aws", cloud.getService());
		assertEquals("SwirldsSecGroup", cloud.getSecurityGroup());
//		assertEquals("sg-7214c20b", cloud.getSecurityGroupID());
		assertEquals("ATF-U18.04-OJDK12.0.1-PSQL10.9-BADGERIZE-V9", cloud.getInstanceName());
		assertEquals("T2Micro",cloud.getInstanceType());
		assertEquals("regression-key", cloud.getInstanceKey());
		assertEquals("./testing/my-key", cloud.getKeyLocation());
		assertEquals("ubuntu", cloud.getLogin());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/CloudConfigTest/NoEOF.json",
			"configs/CloudConfigTest/NoBraces.json",
			"configs/CloudConfigTest/NoCommas.json",
			"configs/CloudConfigTest/NoQuotes.json",
	})
	@DisplayName("Test Malformed config json file parsing")
	public void testMalformedParsing(String configFile) throws URISyntaxException {
		assertThrows(IOException.class, () -> {
			CloudConfig rl = loadCloudConfig(configFile);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/CloudConfigTest/CloudMissingInstanceKey.json",
			"configs/CloudConfigTest/CloudMissingInstanceName.json",
			"configs/CloudConfigTest/CloudMissingInstanceType.json",
			"configs/CloudConfigTest/CloudMissingKeyLocation.json",
			"configs/CloudConfigTest/CloudMissingLogin.json",
			"configs/CloudConfigTest/CloudMissingRegionList.json",
			"configs/CloudConfigTest/CloudMissingSecurityGroup.json",
			"configs/CloudConfigTest/CloudMissingSecurityGroupID.json",
			"configs/CloudConfigTest/CloudMissingService.json"
	})
	@DisplayName("Test Malformed config json file parsing")
	public void testMissingParamsParsing(String configFile) throws URISyntaxException, IOException {
		CloudConfig cloud = loadCloudConfig(configFile);
		assertFalse(cloud.isValidConfig());
	}
}
