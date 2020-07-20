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

import com.swirlds.regression.RegressionMain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public abstract class Validator {
	static final Logger log = LogManager.getLogger(RegressionMain.class);
	static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	List<String> infoMessages = new LinkedList<>();
	List<String> warnMessages = new LinkedList<>();
	List<String> errorMessages = new LinkedList<>();

	private int lastStakedNode;

	protected void addInfo(String msg) {
		infoMessages.add(msg);
	}

	protected void addWarning(String msg) {
		warnMessages.add(msg);
	}

	protected void addError(String msg) {
		errorMessages.add(msg);
	}

	public List<String> getInfoMessages() {
		return infoMessages;
	}

	public List<String> getWarningMessages() {
		return warnMessages;
	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public boolean hasErrors() {
		return errorMessages.size() > 0;
	}

	public boolean hasWarnings() {
		return warnMessages.size() > 0;
	}

	public String concatAllMessages() {
		StringBuilder sb = new StringBuilder();
		if (infoMessages.size() > 0) {
			sb.append("<<INFO>>");
			infoMessages.forEach((msg) -> {
				sb.append('\n');
				sb.append(msg);
			});
		}
		if (warnMessages.size() > 0) {
			sb.append('\n');
			sb.append("<<WARNING>>");
			warnMessages.forEach((msg) -> {
				sb.append('\n');
				sb.append(msg);
			});
		}
		if (errorMessages.size() > 0) {
			sb.append('\n');
			sb.append("<<ERROR>>");
			errorMessages.forEach((msg) -> {
				sb.append('\n');
				sb.append(msg);
			});
		}
		return sb.toString();
	}

	// abstract methods

	public abstract void validate() throws IOException;

	public abstract boolean isValid();

	public int getLastStakedNode() {
		return lastStakedNode;
	}

	public void setLastStakedNode(final int lastStakedNode) {
		this.lastStakedNode = lastStakedNode;
	}
}
