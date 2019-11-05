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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EventSigEvent implements Iterable<EventSigFile> {

	private final List<EventSigFile> evtsSigEvents;

	public EventSigEvent(final List<String> evtsSigEvents) {
		this.evtsSigEvents = new ArrayList<>();
		for (final String eventSigFileName : evtsSigEvents) {
			if (eventSigFileName.trim().isEmpty()) {
				continue;
			}

			this.evtsSigEvents.add(new EventSigFile((eventSigFileName)));
		}

		Collections.sort(this.evtsSigEvents);
	}

	public int size() {
		return this.evtsSigEvents.size();
	}

	@Override
	public String toString() {
		final StringBuilder description = new StringBuilder();
		for (final EventSigFile sigFile : this) {
			description.append(sigFile.getFilename());
			description.append('\n');
		}

		description.append('\n');
		return description.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof EventSigEvent)) {
			return false;
		}

		final EventSigEvent other = (EventSigEvent) o;
		if (this.size() == other.size()) {
			return this.evtsSigEvents.equals(other.evtsSigEvents);
		}

		final int diffInSize = Math.abs(this.evtsSigEvents.size() - other.evtsSigEvents.size());
		if (diffInSize > 1) {
			return false;
		}

		return equalsWithAllButTheLastEvent(other);
	}

	private boolean equalsWithAllButTheLastEvent(final EventSigEvent other) {
		final int minSize = this.evtsSigEvents.size() > other.evtsSigEvents.size() ? other.evtsSigEvents.size() : this.evtsSigEvents.size();
		for (int index = 0; index < minSize; index++) {
			if (!this.evtsSigEvents.get(index).equals(other.evtsSigEvents.get(index))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Iterator<EventSigFile> iterator() {
		return this.evtsSigEvents.iterator();
	}
}
