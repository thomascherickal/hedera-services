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

package com.swirlds.regression.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CsvReader {
	private static final String categoryNameSeparator = "@";
	private CsvStat columns[];
	private Map<String, CsvStat> columnMap;

	private CsvReader(InputStream fileStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));
		CsvParser parser = new CsvParserV1();

		Supplier<String> lineSupplier = () -> {
			try {
				return reader.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
		columns = parser.getColumns(lineSupplier);
		parser.addAllData(lineSupplier, columns);

		columnMap = new HashMap<>();

		for (CsvStat column : columns) {
			columnMap.put(fullStatName(column), column);
		}
	}

	public static CsvReader createReader(int csvVersion, InputStream fileStream) {
		return new CsvReader(fileStream);
	}

	public CsvStat[] getColumns() {
		return columns;
	}

	/**
	 * Gets the stat by name from one of the default categories (platform, internal)
	 *
	 * @param statName
	 * @return the stat object
	 */
	public CsvStat getColumn(String statName) {
		CsvStat stat = columnMap.get(fullStatName("platform", statName));
		if (stat != null) {
			return stat;
		}
		stat = columnMap.get(fullStatName("internal", statName));
		return stat;
	}

	private String fullStatName(CsvStat column) {
		return column.getCategory() + categoryNameSeparator + column.getName();
	}

	private String fullStatName(String category, String name) {
		return category + categoryNameSeparator + name;
	}
}
