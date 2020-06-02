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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CsvStat {
	private static final Logger log = LogManager.getLogger(CsvStat.class);

	private String category;
	private String name;
	private List<String> data;

	public CsvStat(String category, String name) {
		this.category = category;
		this.name = name;
		data = new ArrayList<>();
	}

	public String getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public void addData(String data) {
		this.data.add(data);
	}

	public int dataSize() {
		return data.size();
	}

	public String getDataElement(int index) {

		if (index < data.size()) return data.get(index);
		else return "";
	}

	public double asDouble(String s) {
		double d = Double.NaN;
		try {
			d = Double.parseDouble(s);
		} catch (NumberFormatException e) {
			log.debug("Cannot parse stat:'{}' value:'{}'", name, s);
		} catch (NullPointerException e) {
			log.debug("stat:'{}' value:'{}' is null", name, s);
			d = 0.0;
		}
		return d;
	}

	public double getAverage() {
		double sum = 0;
		for (String s : data) {
			sum += Double.parseDouble(s);
		}
		return sum / data.size();
	}

	public double getLastEntryAsDouble() {
		if (data.size() == 0) {
			return Double.NaN;
		}
		return Double.parseDouble(data.get(data.size() - 1));
	}

	public double getMax() {
		Double max = null;
		for (String s : data) {
			//System.out.println("getMax s: "+s);
			try {
				double value = Double.parseDouble(s);
				//System.out.println("getMax v: "+value);
				if (max == null) {
					max = value;
				} else {
					max = Math.max(max, value);
				}
				//System.out.println("getMax m: "+max);
			} catch (NumberFormatException e) {
				log.debug("Cannot parse stat:'{}' value:'{}'", name, s);
			}
		}
		//System.out.println("getMax m: "+max);
		return max == null ? Double.NaN : max;
	}

	public double getMinNot0() {
		Double min = null;
		for (String s : data) {
			try {
				double value = Double.parseDouble(s);
				if (value == 0) {
					continue;
				}
				if (min == null) {
					min = value;
				} else {
					min = Math.min(min, value);
				}
			} catch (NumberFormatException e) {
				log.debug("Cannot parse stat:'{}' value:'{}'", name, s);
			}
		}
		return min == null ? Double.NaN : min;
	}

	@Override
	public String toString() {
		return "CsvStat{" +
				"category='" + category + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
