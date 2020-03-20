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

package com.swirlds.regression.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentSummaryStorage {
	// date time format used for file names
	private static SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
	private static final String SUMMARY_DIR = "./summaryHistory/";
	private static final String FILE_NAME_PATTERN =
			"([0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3})_(.*?)\\.json";

	public static void storeSummary(ExperimentSummary summary, Date timeDate) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		String name = summary.getName();
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Summary must have a name");
		}
		name = name.trim();

		File dir = new File(SUMMARY_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		// Java object to JSON file
		mapper.writeValue(
				new File(
						String.format("%s%s_%s.json",
								SUMMARY_DIR,
								dt.format(timeDate),
								name
						)
				),
				summary);

	}

	public static List<ExperimentSummary> readSummaries(String name, int number) throws IOException {
		List<ExperimentSummary> list = new ArrayList<>(number);
		ObjectMapper mapper = new ObjectMapper();

		for (SummaryFileInfo testFile : getTestFiles(name)) {
			if (number <= 0) {
				break;
			}

			ExperimentSummaryData obj = mapper.readValue(testFile.getFile(), ExperimentSummaryData.class);
			list.add(obj);
			number--;
		}

		return list;
	}

	public static void deleteOldSummaries(String name, int keepLatest) throws IOException {
		for (SummaryFileInfo testFile : getTestFiles(name)) {
			if (keepLatest > 0) {
				keepLatest--;
				continue;
			}
			testFile.file.delete();
		}
	}

	private static List<SummaryFileInfo> getTestFiles(String name) {
		List<SummaryFileInfo> list = new LinkedList<>();
		File dir = new File(SUMMARY_DIR);
		if (!dir.exists()) {
			return list;
		}
		for (File file : Objects.requireNonNull(dir.listFiles())) {
			Pattern r = Pattern.compile(FILE_NAME_PATTERN);
			Matcher m = r.matcher(file.getName());
			if (!m.find()) {
				continue;
			}

			String dateTime = m.group(1);
			String testName = m.group(2);

			if (name.equals(testName)) {
				list.add(new SummaryFileInfo(file, dateTime, testName));
			}
		}
		list.sort(Comparator.comparing(SummaryFileInfo::getDateTime));
		return list;
	}

	private static class SummaryFileInfo {
		private File file;
		private String dateTime;
		private String name;

		public SummaryFileInfo(File file, String dateTime, String name) {
			this.file = file;
			this.dateTime = dateTime;
			this.name = name;
		}

		public File getFile() {
			return file;
		}

		public String getDateTime() {
			return dateTime;
		}

		public String getName() {
			return name;
		}
	}
}
