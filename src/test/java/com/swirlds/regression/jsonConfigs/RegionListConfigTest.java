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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegionListConfigTest {

	public static RegionList loadRegionListConfig(String filePath) throws URISyntaxException, IOException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URI regressionConfigFileLocation = classloader.getResource(filePath).toURI();

		byte[] jsonData = Files.readAllBytes(Paths.get(regressionConfigFileLocation));
		ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);

		RegionList regCofig = objectMapper.readValue(jsonData, RegionList.class);

		return regCofig;

	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegionListTest/singleRegion4Instance6Node.json",
			"configs/RegionListTest/singleRegion4Node.json",
			"configs/RegionListTest/singleRegion4Instance.json"
	})
	@DisplayName("Test generic config json file parsing")
	public void testConfigParsing(String configFile) throws URISyntaxException, IOException {
		RegionList rl = loadRegionListConfig(configFile);
		assertNotNull(rl.getRegion());
		assertNotEquals("", rl.getRegion());
		assertNotEquals(-1, rl.getNumberOfNodes());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegionListTest/NoEOF.json",
			"configs/RegionListTest/NoBraces.json",
			"configs/RegionListTest/NoCommas.json",
			"configs/RegionListTest/NoQuotes.json",
	})
	@DisplayName("Test Malformed config json file parsing")
	public void testMalformedParsing(String configFile) throws URISyntaxException {
		assertThrows(IOException.class, () -> {
			RegionList rl = loadRegionListConfig(configFile);
		});
	}

}
