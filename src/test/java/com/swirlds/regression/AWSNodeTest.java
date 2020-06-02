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

import com.swirlds.regression.jsonConfigs.RegionList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.swirlds.regression.jsonConfigs.RegionListConfigTest.loadRegionListConfig;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AWSNodeTest {


	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegionListTest/singleRegion4Instance6Node.json",
			"configs/RegionListTest/singleRegion4Node.json",
			"configs/RegionListTest/singleRegion4Instance.json"
	})
	@DisplayName("Test generic config json file parsing")
	public void testConfigParsing(String configFile) throws URISyntaxException, IOException {
		RegionList rl = loadRegionListConfig(configFile);
		AWSNode node = new AWSNode(rl);

		assertNotNull(node.getRegion());
		assertNotNull(node.getEc2());
		assertNotEquals("", node.getRegion());
		assertNotEquals(-1, node.getTotalNodes());
		if (node.isExistingInstance()) {
			assertNotNull(node.getInstanceIDs());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"configs/RegionListTest/NoEOF.json",
			"configs/RegionListTest/NoBraces.json",
			"configs/RegionListTest/NoCommas.json",
			"configs/RegionListTest/NoQuotes.json",
	})
	@DisplayName("Test malformed config json file parsing")
	public void testMalformedParsing(String configFile) {
		final RegionList[] rl = new RegionList[1];
		assertThrows(IOException.class, () -> {
			rl[0] = loadRegionListConfig(configFile);
		});
		assertThrows(NullPointerException.class, () -> {
			AWSNode node = new AWSNode(rl[0]);
		}, "RegionList was null");
	}

	@Test
	@DisplayName("Test null config json file parsing")
	public void tesNullParsing() {

		assertThrows(NullPointerException.class, () -> {
			AWSNode node = new AWSNode(null);
		}, "RegionList was null");
	}

}
