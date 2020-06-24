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

import com.swirlds.regression.TarGzFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {
	private static final Logger log = LogManager.getLogger(FileUtils.class);
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");
	private static final int MAX_BUFFER_LENGTH = 1024;

	/**
	 * return an array of files whose name matches given regex in the given folder
	 */
	public static File[] getFilesMatchRegex(final String folderPath, final String regex) {
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			return null;
		}

		return folder.listFiles((dir, name) -> name.toLowerCase().matches(regex));
	}

	/**
	 * Generate a .tar.gz File for given files
	 * @param tarGZFile
	 * @param files an collection of files to be put into the tarFile
	 */
	public static boolean generateTarGZFile(final File tarGZFile,
			final Collection<File> files) {
		try (TarGzFile archive = new TarGzFile(Paths.get(tarGZFile.toURI()))) {
			for (File file : files) {
				if (!file.exists()) {
					continue;
				}
				if (file.isFile()) {
					archive.bundleFile(Paths.get(file.toURI()));
				} else if (file.isDirectory()) {
					archive.bundleDirectory(Paths.get(file.toURI()));
				}
			}
		} catch (IOException e) {
			log.info("could not create tarball {} for files {}", tarGZFile, files);
			return false;
		}
		return true;
	}

	/**
	 * zip files into a file
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

	public static InputStream getInputStream(String filename) {
		InputStream inputStream = null;
		if (!new File(filename).exists()) {
			return null;
		}
		try {
			inputStream = new FileInputStream(filename);
		} catch (IOException e) {
			log.error(ERROR, "Could not open file {} for validation", filename, e);
		}
		return inputStream;
	}
}
