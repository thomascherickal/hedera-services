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

package com.swirlds.regression;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class CompressResults implements AutoCloseable {
	private static final PathMatcher ZIP_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.zip");

	private final ArchiveOutputStream out;

	public CompressResults(Path path) throws IOException {
		if (path == null) throw new NullPointerException("path must not be null.");
		if (!ZIP_MATCHER.matches(path)) throw new IllegalArgumentException("path must be a *.zip file.");

		Files.createDirectories(path.getParent());
		out = new ZipArchiveOutputStream((Files.newOutputStream(path)));
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	public void bundleFile(Path path) throws IOException {
		if (path == null) throw new NullPointerException("path must not be null.");
		if (!Files.isRegularFile(path)) throw new IllegalArgumentException("path must be an existing file.");
		String filename = path.getFileName().toString();

		doBundleFile(path, filename);
	}

	private void doBundleFile(Path path, String fileName) throws IOException {
		ArchiveEntry entry = new ZipArchiveEntry(path.toFile(), fileName);
		out.putArchiveEntry(entry);
		try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
			IOUtils.copy(in, out);
		}
		out.closeArchiveEntry();
	}

	public void bundleDirectory(Path path) throws IOException {
		if (path == null) throw new NullPointerException("path must not be null.");
		if (!Files.isDirectory(path)) throw new IllegalArgumentException("path must be an existing directory.");

		FileVisitor<Path> visitor = new CompressResults.DirectoryBundler(path);
		Files.walkFileTree(path, visitor);
	}

	private final class DirectoryBundler extends SimpleFileVisitor<Path> {
		private final Path path, name;

		private DirectoryBundler(Path path) {
			this.path = path;
			name = path.getFileName();
		}

		/**
		 * Prevents Apache compress utility from flattening directory structure of directories that swirlds.jar is
		 * expecting
		 * to be present. Uses RegressionUtilities.DIRECTORIES_TO_INCLUDE as a list of needed directories, and returns
		 * the name of the direcotory if it is found in the path to be prepended to the filename itself.
		 *
		 * @param pathToCheck
		 * 		- Path of file or directory to be added to the tar bundle.
		 * @return String of directory to be prepended to entryname passed to tar bundle utility
		 */
		private String checkForDirectoriesToInclude(Path pathToCheck) {
			for (String dir : RegressionUtilities.DIRECTORIES_TO_INCLUDE) {
				if (pathToCheck.getParent().toString().contains(dir)) {
					return dir + "/";
				}
			}
			return "";
		}

		@Override
		public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
			String fileName = entryName(directory);
			fileName = checkForDirectoriesToInclude(directory) + fileName;

			if (!fileName.equals("")) {
				ArchiveEntry entry = new ZipArchiveEntry(directory.toFile(), fileName);
				out.putArchiveEntry(entry);
				out.closeArchiveEntry();
			}

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
			String fileName = entryName(file);
			fileName = checkForDirectoriesToInclude(file) + fileName;
			doBundleFile(file, fileName);

			return FileVisitResult.CONTINUE;
		}

		private String entryName(Path path) {
			return name.resolve(this.path.relativize(path)).toString();
		}
	}
}
