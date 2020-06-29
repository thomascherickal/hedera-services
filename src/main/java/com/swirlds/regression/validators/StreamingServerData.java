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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StreamingServerData {
	/**
	 * the hash value of the list of hash values of stream files
	 */
	private String sha1sum = null;
	/**
	 * an InputSteam from which we read sha1sum
	 */
	private InputStream sha1sumStream;
	/**
	 * have we already read sha1sum
 	 */
	private boolean sha1sumRead = false;
	/**
	 * a list of sha1sum of each stream file
 	 */
	private ArrayList<String> sha1List = null;
	/**
	 * have we already read sha1List
 	 */
	private boolean sha1ListRead = false;
	/**
	 * a list of stream signature file names
	 */
	private final List<String> sigFileNames;
	/**
	 * an InputStream from which we read the result of comparing hashes of recovered event
	 * stream files with previous generated event stream files
	 */
	private InputStream recoverEventMatchLog = null;

	/**
	 *
	 * @param sigFileNameStream
	 * 		The input stream contains the list of file names for signatures of stream file
	 * @param sha1sumStream
	 * 		The input stream contains the hash value of the list of hash values of stream files
	 * @param sha1ListStream
	 * 		The input stream contains the list of hash values of stream files
	 */
	public StreamingServerData(final InputStream sigFileNameStream,
			final InputStream sha1sumStream,
			final InputStream sha1ListStream) {
		sigFileNames = readStreamFileNames(sigFileNameStream);
		this.sha1sumStream = sha1sumStream;
		getSha1Lists(sha1ListStream);
	}

	/**
	 *
	 * @param sigFileNameStream
	 * 		The input stream contains the list of file names for signatures of stream file
	 * @param sha1sumStream
	 * 		The input stream contains the hash value of the list of hash values of stream files
	 * @param sha1ListStream
	 * 		The input stream contains the list of hash values of stream files
	 * @param recoverEventMatchLog
	 * 		The input stream contains the result of comparing hashes of recovered event
	 * 	stream files with previous generated event stream files
	 */
	public StreamingServerData(final InputStream sigFileNameStream,
			final InputStream sha1sumStream,
			final InputStream sha1ListStream,
			final InputStream recoverEventMatchLog) {
		this(sigFileNameStream, sha1sumStream, sha1ListStream);
		this.recoverEventMatchLog = recoverEventMatchLog;
	}

	private void getSha1Lists(InputStream sha1ListStream) {
		sha1List = new ArrayList<>();
		if (!sha1ListRead) {
			sha1List.addAll(readStreamFileNames(sha1ListStream));
			sha1ListRead = true;
		}
	}

	private List<String> readStreamFileNames(final InputStream fileNameStream) {
		final List<String> files = new ArrayList<>();
		if (fileNameStream == null) {
			return files;
		}

		try (final Scanner scanner = new Scanner(fileNameStream)){
			while (scanner.hasNextLine()) {
				files.add(scanner.nextLine());
			}
		}

		return files;
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

	private boolean checkShaListAvailable() {
		return sha1ListRead && sha1List != null;
	}

	public List<String> getSha1ListData() {
		if (!checkShaListAvailable()) {
			return null;
		}
		return sha1List;
	}

	public int getNumberOfStreamFiles() {
		return sha1List.size();
	}

	public StreamSigsInANode getSigFileNames() {
		return new StreamSigsInANode(this.sigFileNames);
	}

	public InputStream getRecoverEventMatchLog() {
		return recoverEventMatchLog;
	}
}
