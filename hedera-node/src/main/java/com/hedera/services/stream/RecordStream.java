package com.hedera.services.stream;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.swirlds.common.crypto.Hash;
import com.swirlds.common.io.SerializableDataInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class RecordStream {


	private static final Logger log = LogManager.getLogger(RecordStream.class);

	private SerializableDataInputStream stream;

	/**
	 * stream is closed or stream is null
	 */
	private boolean streamClosed;

	/**
	 * Read the previous file hash from file system
	 * @param directory path to the directory that contains the record stream files
	 * @return running hash of the last record stream file in the given directory
	 */

	private Hash readPrevFileHash(String directory) throws IOException {
		File dir = new File(directory);
		File[] files = dir.listFiles();
		Optional<File> lastSigFileOptional = Arrays.stream(files).filter(file -> isRecordStreamFile(file))
				.max(Comparator.comparing(File::getName));
		if (lastSigFileOptional.isPresent()) {
			File lastSigFile = lastSigFileOptional.get();
			return getFileHashFromSigFile(lastSigFile);
		}
		return null;
	}

	/**
	 * Read the FileHash from the record stream signature file
	 *
	 * @param file
	 * @return
	 */
	public Hash getFileHashFromSigFile(File file) throws IOException {
		return parseSigFile(file);
	}

	/**
	 * Parse this file
	 * @param file
	 * @return
	 */
	private Hash parseSigFile(File file) throws IOException {
		stream = new SerializableDataInputStream(
				new BufferedInputStream(new FileInputStream(file)));

		log.info("reading file: {}", file::getName);

		// read file version
		int fileVersion = stream.readInt();
		log.info("read file version: {}", () -> fileVersion);

		if(stream.available() == 0 || streamClosed) {
			return null;
		}
		return (Hash) stream.readSerializable();
	}

	/**
	 * close current stream
	 */
	void closeStream() {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			log.error("got IOException when closing stream. ", e);
		}
		streamClosed = true;
	}

	/**
	 * Check if a file is a RecordStream signature file
	 *
	 * @param file
	 * @return
	 */
	public static boolean isRecordStreamFile(File file) {
		return file.getName().endsWith(".soc");
	}
}
