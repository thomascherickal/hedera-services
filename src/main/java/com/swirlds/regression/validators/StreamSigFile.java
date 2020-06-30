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

import java.time.Instant;

public class StreamSigFile implements Comparable<StreamSigFile> {
	private final String filename;
	private Instant creationTime;

	public StreamSigFile(final String filename, final StreamType streamType) {
		this.filename = filename;
		try {
			String dateInfo = this.filename.substring(0,
					this.filename.length() - streamType.getSigExtension().length());
			dateInfo = dateInfo.replace("_", ":");
			this.creationTime = Instant.parse(dateInfo);
		} catch (final Exception ex) {
			throw new RuntimeException("Invalid filename: " + filename, ex);
		}
	}

	public String getFilename() {
		return this.filename;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof StreamSigFile)) {
			return false;
		}

		final StreamSigFile other = (StreamSigFile) o;
		return this.filename.equals(other.filename);
	}

	@Override
	public int compareTo(final StreamSigFile o) {
		return this.creationTime.compareTo(o.creationTime);
	}
}
