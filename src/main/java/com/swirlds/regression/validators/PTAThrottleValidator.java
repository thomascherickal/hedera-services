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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.RegressionUtilities;
import com.swirlds.regression.jsonConfigs.TestConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PTAThrottleValidator is a validator for checking whether trans/sec complied with specified throttle values
 * in the PlatformTestingApp.  It is specified in the test config as PTA_THROTTLE.
 * For throttle values specified elsewhere, use
 * @see ThrottleValidator
 */
public class PTAThrottleValidator extends ThrottleValidator {

	private TestConfig testConfig;

	String PTAJsonConfigFilePath = RegressionUtilities.PTD_CONFIG_DIR;

	public PTAThrottleValidator(List<NodeData> nodeData, TestConfig testConfig) {
		super(nodeData);
		this.testConfig = testConfig;
	}

	@Override
	public void validate() throws IOException {
		ArrayList<Double> throttleValues = getAndParsePtaConfig();
		setThrottleValues(throttleValues);
		super.validate();
	}

	/**
	 * This method gets the throttle values from the PlatformTestingApp's config JSON file
	 *
	 * @return ArrayList of Doubles representing the throttle values in the PTA config
	 */
	private ArrayList<Double> getAndParsePtaConfig() {

		// Assumptions: for PTA, there is always only one parameter: the PTA JSON config file
		ArrayList<Double> throttleValues = new ArrayList<>();
		String jarName = testConfig.getApp().getJar();
		if (!jarName.equals("PlatformTestingApp.jar")) return throttleValues;

		String jsonFileName = testConfig.getApp().getParameterList().get(0);
		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = null;

		String fullPath = PTAJsonConfigFilePath + jsonFileName;

		try {
			File jsonFile = new File(fullPath);
			rootNode = mapper.readTree(jsonFile);
			boolean enableThrottle = rootNode.findValue("submitConfig").findValue("enableThrottling").asBoolean();
			if (enableThrottle) {
				JsonNode tpsMap = rootNode.findValue("submitConfig").findValue("tpsMap");
				int index = 0;
				for (JsonNode throttleOp : tpsMap) {
					throttleValues.add(index++, throttleOp.asDouble());
				}
				addInfo(String.format("Found %d throttle values", throttleValues.size()));
			}
		} catch (IOException | NullPointerException e) {
			addWarning("Unable to read PTA json file: " + fullPath);
		}

		return throttleValues;
	}
}
