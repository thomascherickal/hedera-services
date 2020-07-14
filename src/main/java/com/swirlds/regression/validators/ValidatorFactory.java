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

package com.swirlds.regression.validators;

import com.swirlds.regression.jsonConfigs.TestConfig;
import com.swirlds.regression.validators.services.HAPIClientValidator;
import com.swirlds.regression.validators.services.HGCAAValidator;
import com.swirlds.regression.validators.services.HederaNodeValidator;

import java.util.List;
import java.util.Map;

public class ValidatorFactory {

	public static Validator getValidator(ValidatorType vt, List<NodeData> nodeData,
			TestConfig testConfig) {
		return getValidator(vt, nodeData, testConfig, null, null, null);
	}

	public static Validator getValidator(ValidatorType vt, List<NodeData> nodeData,
			TestConfig testConfig, Map<Integer, String> expectedMapPaths,
			List<NodeData> testClientNodeData, List<NodeData> hederaNodeData) {
		if (vt == null) {
			return null;
		}

		switch (vt) {
			case BLOB_STATE:
				return new BlobStateValidator();
			case RESTART:
				return new RestartValidator(nodeData, testConfig);
			case RECONNECT:
				return new ReconnectValidator(nodeData, testConfig);
			case PLATFORM_TESTING_DEMO:
				return new PtdValidator(nodeData);
			case STATS:
				return new StatsValidator(nodeData, testConfig);
			case RECOVER_STATE:
				return new RecoverStateValidator(nodeData);
			case THROTTLE:
				return new ThrottleValidator(nodeData);
			case PTA_THROTTLE:
				return new PTAThrottleValidator(nodeData, testConfig);
			case MIGRATION:
				return new MigrationValidator(nodeData);
			case STDOUT:
				return new StdoutValidator(nodeData);
			case GOSSIP_COMPENSATION:
				return new GossipCompensationValidator(nodeData);
			case LIFECYCLE:
				return new LifecycleValidator(expectedMapPaths);
			case HAPI_CLIENT:
				return new HAPIClientValidator(testClientNodeData);
			case HGCAA:
				return new HGCAAValidator(hederaNodeData);
			case HEDERA_NODE:
				return new HederaNodeValidator(nodeData);
			default:
				return new StandardValidator(nodeData);
		}
	}
}
