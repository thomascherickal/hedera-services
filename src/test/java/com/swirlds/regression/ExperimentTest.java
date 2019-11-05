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

package com.swirlds.regression;

import com.swirlds.regression.jsonConfigs.DBConfig;
import com.swirlds.regression.jsonConfigs.RegressionConfig;
import com.swirlds.regression.jsonConfigs.TestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ExperimentTest {
	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	static Experiment exp;
	static ArrayList<String> logs = new ArrayList<String>();
	static String[] searchPhrases = {"Freeze state is about to be saved to disk","has loaded a saved state for round"};

	@BeforeAll
	public static void init() {
		exp = new Experiment(RegressionUtilities.importRegressionConfig(), RegressionUtilities.TEST_CONFIG);

		logs.add("swirlds.013012.log");
		logs.add("swirlds.012711.log");
	}

	/*@Test
	@DisplayName("Import Node config JSON Test")
	public void testImportNodeConfig(){

		TestConfig nc = exp.importExperimentConfig();
	}*/

	//@Test
/*	@DisplayName("Find String in files")
	public void testPrintFoundLines(){
		ArrayList<String>firstSearch = exp.scanLogs(searchPhrases[0],logs);
		ArrayList<String>secondSearch = exp.scanLogs(searchPhrases[1],logs);

		assertEquals(firstSearch.get(0), "2019-02-19 19:26:00.247 227136 <       stateHash  3   > Freeze state is about to be saved to disk, round is 2011");
		assertEquals(secondSearch.get(0), "2019-02-19 19:27:20.307   8138 <     platformRun  2  2> Platform 2 has loaded a saved state for round 2011. Will freeze until: 2019-02-19T19:27:30.296169");
	}

	//@Test
	@DisplayName("Test getRoundNumbers")
	public void testgetRoundNumbers(){
		ArrayList<String>firstSearch = exp.scanLogs(searchPhrases[0],logs);
		ArrayList<String>secondSearch = exp.scanLogs(searchPhrases[1],logs);

		HashMap<Integer, String> savingRoundNum = exp.getRoundNumbers(searchPhrases[new Integer(0)], firstSearch);
		assertEquals(savingRoundNum.get(0), "2011");
		HashMap<Integer, String> loadingRoundNum = exp.getRoundNumbers(searchPhrases[new Integer(0)], secondSearch);
		assertEquals(loadingRoundNum.get(0), "2011");
	}*/


}
