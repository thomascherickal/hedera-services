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

public class StreamSigsInANode implements Iterable<StreamSigFile> {

	private final List<StreamSigFile> streamSigFiles;

	public StreamSigsInANode(final List<String> streamSigFiles, final StreamType streamType) {
		this.streamSigFiles = new ArrayList<>();
		for (final String sigFileName : streamSigFiles) {
			if (sigFileName.trim().isEmpty()) {
				continue;
			}

			this.streamSigFiles.add(new StreamSigFile(sigFileName, streamType));
		}

		Collections.sort(this.streamSigFiles);
	}

	public int size() {
		return this.streamSigFiles.size();
	}

	@Override
	public String toString() {
		final StringBuilder description = new StringBuilder();
		for (final StreamSigFile sigFile : this) {
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

		if (!(o instanceof StreamSigsInANode)) {
			return false;
		}

		final StreamSigsInANode other = (StreamSigsInANode) o;
		if (this.size() == other.size()) {
			return this.streamSigFiles.equals(other.streamSigFiles);
		}

		final int diffInSize = Math.abs(this.streamSigFiles.size() - other.streamSigFiles.size());
		/*
		Because some nodes may be killed while streams are still being written, the last stream file or two may
		mismatch. The difference is not allowed to be greater than 2.
		*/

		if (diffInSize > 2) {
			return false;
		}

		return equalsWithAllButTheLastTwo(other);
	}

	private boolean equalsWithAllButTheLastTwo(final StreamSigsInANode other) {
		final int minSize = this.streamSigFiles.size() > other.streamSigFiles.size() ? other.streamSigFiles.size() : this.streamSigFiles.size();
		for (int index = 0; index < minSize - 1; index++) {
			if (!this.streamSigFiles.get(index).equals(other.streamSigFiles.get(index))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Iterator<StreamSigFile> iterator() {
		return this.streamSigFiles.iterator();
	}
}
