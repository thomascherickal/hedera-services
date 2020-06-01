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

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class StreamingServerData {
	private InputStream sha1sumStream;
	private String sha1sum = null;
	private boolean sha1sumRead = false;
	private ArrayList<String> sha1Events = null;
	private boolean sha1EventsRead = false;
	private final List<String> evtsSigEvents;
	private InputStream recoverEventMatchLog = null;

	public StreamingServerData(InputStream sha1sumStream) {
		this(null, sha1sumStream, null);
	}

	public StreamingServerData(InputStream sha1sumStream, InputStream sha1EventStream) {
		this(null, sha1sumStream, sha1EventStream);
	}

	/**
	 *
	 * @param evtsSigStream
	 * 		The input stream contains the list of file names for signatures of event stream file
	 * @param sha1sumStream
	 * 		The input stream contains the hash value of the list of hash values of event stream files
	 * @param sha1EventStream
	 * 		The input stream contains the list of hash values of event stream files
	 * 	stream files a with previous generated event stream files
	 */
	public StreamingServerData(final InputStream evtsSigStream, final InputStream sha1sumStream,
			final InputStream sha1EventStream) {
		evtsSigEvents = readEventsFile(evtsSigStream);
		this.sha1sumStream = sha1sumStream;
		getSha1Events(sha1EventStream);
	}

	/**
	 *
	 * @param evtsSigStream
	 * 		The input stream contains the list of file names for signatures of event stream file
	 * @param sha1sumStream
	 * 		The input stream contains the hash value of the list of hash values of event stream files
	 * @param sha1EventStream
	 * 		The input stream contains the list of hash values of event stream files
	 * @param recoverEventMatchLog
	 * 		The input stream contains the comparison result of comparing hashes of recovered event
	 * 	stream files a with previous generated event stream files
	 */
	public StreamingServerData(final InputStream evtsSigStream, final InputStream sha1sumStream,
			final InputStream sha1EventStream, InputStream recoverEventMatchLog) {
		evtsSigEvents = readEventsFile(evtsSigStream);
		this.sha1sumStream = sha1sumStream;
		getSha1Events(sha1EventStream);
		this.recoverEventMatchLog = recoverEventMatchLog;
	}

	private void getSha1Events(InputStream sha1EventStream) {
		sha1Events = new ArrayList<>();
		if (!sha1EventsRead) {
			sha1Events.addAll(readEventsFile(sha1EventStream));
			sha1EventsRead = true;
		}
	}

	private List<String> readEventsFile(final InputStream eventStream) {
		final List<String> events = new ArrayList<>();
		if (eventStream == null) {
			return events;
		}

		try (final Scanner eventScanner = new Scanner(eventStream)){
			while (eventScanner.hasNextLine()) {
				events.add(eventScanner.nextLine());
			}
		}

		return events;
	}

	public String getSha1SumData() {
		if (!sha1sumRead) {
			if (sha1sumStream != null) {
				Scanner eventScanner = new Scanner(sha1sumStream).useDelimiter("\\Z");
				if (eventScanner.hasNext()) {
					sha1sum = eventScanner.next();
				}
			}
			sha1sumRead = true;
		}
		return sha1sum;
	}

	private boolean checkShaEventAvailable() {
		return sha1EventsRead && sha1Events != null;
	}

	public List<String> getSha1EventData() {
		if (!checkShaEventAvailable()) {
			return null;
		}
		return sha1Events;
	}

	public int getNumberOfEvents() {
		return sha1Events.size();
	}


	public String getLastEvent() {
		if (checkShaEventAvailable()) {
			return null;
		}
		return sha1Events.get(sha1Events.size() - 1);
	}

	public EventSigEvent getEvtsSigEvents() {
		return new EventSigEvent(this.evtsSigEvents);
	}

	public InputStream getRecoverEventMatchLog() {
		return recoverEventMatchLog;
	}
}
