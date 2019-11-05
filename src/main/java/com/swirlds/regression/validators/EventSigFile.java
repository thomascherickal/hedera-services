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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EventSigFile implements Comparable<EventSigFile>{

	private static final String EXTENSION = ".ets_sig";
	private static final long DEFAULT_CREATION = 0;
	private static final Date DEFAULT_CREATION_TIME = new Date(DEFAULT_CREATION);
	private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH_mm_ss.SSSSSS'Z'";
	private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);

	private final String filename;
	private Date creationTime;

	public EventSigFile(final String filename) {
		this.filename = filename;
		try {
			final String dateInfo = this.filename.substring(0, this.filename.length() - EXTENSION.length());
			this.creationTime  = FILE_DATE_FORMAT.parse(dateInfo);
		} catch (final ParseException ex) {
			this.creationTime = DEFAULT_CREATION_TIME;
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

		if (!(o instanceof EventSigFile)) {
			return false;
		}

		final EventSigFile other = (EventSigFile) o;
		return this.filename.equals(other.filename);
	}

	@Override
	public int compareTo(final EventSigFile o) {
		return this.creationTime.compareTo(o.creationTime);
	}
}
