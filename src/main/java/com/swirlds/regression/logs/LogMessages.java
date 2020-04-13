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

package com.swirlds.regression.logs;

public class LogMessages {
	public static final String CHANGED_TO_MAINTENANCE = "Platform status changed to: MAINTENANCE";
	public static final String PTD_SAVE_EXPECTED_MAP = "Start a thread for saving expectedMap while freezing";
	public static final String PTD_SAVE_EXPECTED_MAP_ERROR = "Could not serialize expectedMap";
	public static final String PTD_SAVE_EXPECTED_MAP_SUCCESS = "Platform fslog and map saved";
	public static final String PTD_SAVE_FREEZE_STATE_START = "Freeze state is about to be saved to disk, round is";
	public static final String PTD_SAVE_STATE_FINISH = "Finished writing 'Signed state for round ";
}
