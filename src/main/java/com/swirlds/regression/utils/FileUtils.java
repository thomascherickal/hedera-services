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

package com.swirlds.regression.utils;

import com.swirlds.regression.Experiment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {
	private static final Logger log = LogManager.getLogger(FileUtils.class);
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");
	private static final int MAX_BUFFER_LENGTH = 1024;

	public static final String GC_LOG_FILES_REGEX = "gc(.*).log";


	/**
	 * get GC log files in the given folder
	 *
	 * @param folderPath
	 * @return
	 */
	public static File[] getGCLogs(final String folderPath) {
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			return null;
		}

		return folder.listFiles((dir, name) -> name.toLowerCase().matches(GC_LOG_FILES_REGEX));
	}

	/**
	 * zip files
	 *
	 * @param files
	 * 		files to be zipped
	 * @param zipFile
	 * 		result zip file
	 * @return
	 */
	public static void zip(final File[] files, final File zipFile) throws IOException {
		if (files == null || files.length == 0) {
			log.error(ERROR, "Files is empty. Fail to zip files as {}", zipFile.getName());
			return;
		}
		try (ZipOutputStream zipOut = new ZipOutputStream(
				new FileOutputStream(zipFile))) {
			for (File file : files) {
				// add an ZipEntry with current file name
				zipOut.putNextEntry(new ZipEntry(file.getName()));
				// read file content and write to zipOut
				try (FileInputStream input = new FileInputStream(file)) {
					byte[] bytes = new byte[MAX_BUFFER_LENGTH];
					int length;
					while ((length = input.read(bytes)) > 0) {
						zipOut.write(bytes, 0, length);
					}
					// close current ZipEntry after finish processing current file
					zipOut.closeEntry();
				}
			}
		}
	}
}
