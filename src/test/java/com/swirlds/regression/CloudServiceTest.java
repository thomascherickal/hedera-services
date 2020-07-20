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


import com.swirlds.regression.jsonConfigs.CloudConfig;
import com.swirlds.regression.jsonConfigs.CloudConfigTest;
import com.swirlds.regression.jsonConfigs.RegionList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.swirlds.regression.jsonConfigs.RegionListConfigTest.loadRegionListConfig;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CloudServiceTest {

	public void runCloudTests() {
		try {
			testConfigParsing("configs/CloudConfigTest/BaseCloud.json");
		} catch (URISyntaxException | IOException e){
			e.printStackTrace();
		}
	}

	public void testConfigParsing(String configFile) throws URISyntaxException, IOException {
		CloudConfig config = CloudConfigTest.loadCloudConfig(configFile);
		CloudService cloud = new CloudService(config);

		assertNotNull(cloud.cloudConfig);
		assertNotNull(cloud.ec2List);
		assertNotNull(cloud.instances);
		assertNotNull(cloud.instancesRunning);

		cloud.startService("CloudServiceTest");
		try {
			sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(cloud.isInstanceReady());

		cloud.destroyInstances();

	}
}
