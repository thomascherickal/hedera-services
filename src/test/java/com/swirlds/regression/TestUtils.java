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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.jsonConfigs.TestConfig;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;

public class TestUtils {
	/**
	 * load TestConfig from test/resources
	 *
	 * @throws IOException
	 */
	public static TestConfig loadTestConfigFromTestResource(final String path) throws IOException {
		try (InputStream inputStream = getClassLoader().getResourceAsStream(path)) {
			ObjectMapper objectMapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);

			return objectMapper.readValue(inputStream, TestConfig.class);
		}
	}
}
