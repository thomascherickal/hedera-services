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

import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.logs.LogReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestartValidatorTest {

	@Test
	void validateReconnectLogs() throws IOException {
		List<NodeData> nodeData = loadNodeData("results/restart/singleRestart");
		NodeValidator validator = new RestartValidator(nodeData);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}
		for (String msg : validator.getErrorMessages()) {
			System.out.println(msg);
		}
		assertEquals(true, validator.isValid());
	}

	@Test
	void vadidateMultipleRestartLogs() throws IOException {
		List<NodeData> nodeData = loadNodeData("results/restart/multipleRestarts");
		NodeValidator validator = new RestartValidator(nodeData);
		validator.validate();
		for (String msg : validator.getInfoMessages()) {
			System.out.println(msg);
		}
		for (String msg : validator.getErrorMessages()) {
			System.out.println(msg);
		}
		assertEquals(true, validator.isValid());
	}

	public static List<NodeData> loadNodeData(String directory) {
		List<NodeData> nodeData = new ArrayList<>();
		for (int i = 0;i < 4 ; i++) {
			String logFileName = String.format("%s/node%04d/swirlds.log", directory, i);
			InputStream logInput =
					RestartValidatorTest.class.getClassLoader().getResourceAsStream(logFileName);

			String csvFileName = "PlatformTesting" + i + ".csv";
			String csvFilePath = String.format("%s/node%04d/%s", directory, i, csvFileName);
			InputStream csvInput = RestartValidatorTest.class.getClassLoader().getResourceAsStream(csvFilePath);
			if (logInput != null && csvInput != null) {
				nodeData.add(new NodeData(LogReader.createReader(1, logInput), CsvReader.createReader(1,
						csvInput)));
			}
		}
		return nodeData;
	}

}