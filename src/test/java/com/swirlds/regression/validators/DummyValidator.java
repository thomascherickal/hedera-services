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

import java.io.IOException;

/**
 * Used for testing purposes
 */
public class DummyValidator extends Validator {
	private boolean valid = true;

	@Override
	public void validate() throws IOException {

	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public void addInfo(String msg) {
		super.addInfo(msg);
	}

	@Override
	public void addWarning(String msg) {
		super.addWarning(msg);
	}

	@Override
	public void addError(String msg) {
		super.addError(msg);
	}
}
