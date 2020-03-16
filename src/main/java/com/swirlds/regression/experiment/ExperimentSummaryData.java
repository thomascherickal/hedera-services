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

package com.swirlds.regression.experiment;

import java.util.Objects;

public class ExperimentSummaryData implements ExperimentSummary {
	private boolean hasWarnings;
	private boolean hasErrors;
	private boolean hasExceptions;
	private String name;
	private String uniqueId;

	public ExperimentSummaryData() {
	}

	public ExperimentSummaryData(boolean hasWarnings, boolean hasErrors, boolean hasExceptions, String name,
			String uniqueId) {
		this.hasWarnings = hasWarnings;
		this.hasErrors = hasErrors;
		this.hasExceptions = hasExceptions;
		this.name = name;
		this.uniqueId = uniqueId;
	}

	public void setHasWarnings(boolean hasWarnings) {
		this.hasWarnings = hasWarnings;
	}

	public void setHasErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}

	public void setHasExceptions(boolean hasExceptions) {
		this.hasExceptions = hasExceptions;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	@Override
	public boolean hasWarnings() {
		return hasWarnings;
	}

	@Override
	public boolean hasErrors() {
		return hasErrors;
	}

	@Override
	public boolean hasExceptions() {
		return hasExceptions;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getUniqueId() {
		return uniqueId;
	}

	@Override
	public String toString() {
		return "ExperimentSummaryData{" +
				"hasWarnings=" + hasWarnings +
				", hasErrors=" + hasErrors +
				", hasExceptions=" + hasExceptions +
				", name='" + name + '\'' +
				", uniqueId='" + uniqueId + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExperimentSummaryData that = (ExperimentSummaryData) o;
		return hasWarnings == that.hasWarnings &&
				hasErrors == that.hasErrors &&
				hasExceptions == that.hasExceptions &&
				Objects.equals(name, that.name) &&
				Objects.equals(uniqueId, that.uniqueId);
	}
}
