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

import com.swirlds.regression.jsonConfigs.TestConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ValidatorFactory {


	public static NodeValidator getValidator(ValidatorType vt, List<NodeData> nodeData, TestConfig testConfig) {
		if (vt == null) {
			return null;
		}

		switch (vt) {
/*			case FCFS_CSV:
				return new FCFSValidator();
				break;
			case FCM_CSV:
				return new FCMValidator();
				break;
			case RESTART:
				return new RestartValidator();
				break;
			case PLATFORM_CSV:
				break; */
			case RESTART:
				return new RestartValidator(nodeData);
			case RECONNECT:
				return new ReconnectValidator(nodeData);
			case PLATFORM_TESTING_DEMO:
				return new PtdValidator(nodeData);
			case STATS:
				return new StatsValidator(nodeData);
			case RECOVER_STATE:
				return new RecoverStateValidator(nodeData);
			case THROTTLE:
				return new ThrottleValidator(nodeData);
			case PTA_THROTTLE:
				return new PTAThrottleValidator(nodeData, testConfig);
			case LIFECYCLE:
				return new PTALifecycleValidator(nodeData);
			default:
				return new StandardValidator(nodeData);
		}
	}
}
