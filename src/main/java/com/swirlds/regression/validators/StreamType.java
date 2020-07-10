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

public enum StreamType {
	RECORD("records", "rcd"),
	EVENT("events", "evts");

	/**
	 * description of the streamType, used for logging
	 */
	private String description;
	/**
	 * file name extension
	 */
	private String extension;

	StreamType(final String description, final String extension) {
		this.description = description;
		this.extension = extension;
	}

	public String getDescription() {
		return description;
	}

	public String getExtension() {
		return extension;
	}

	public String getSigExtension() {
		return "." + getExtension() + "_sig";
	}
}
