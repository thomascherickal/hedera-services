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

import com.swirlds.common.PlatformStatNames;
import com.swirlds.regression.csv.CsvReader;
import com.swirlds.regression.csv.CsvStat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ThrottleValidator is a validator for checking whether trans/sec complied with specified throttle values.
 * It is specifed in the test config as THROTTLE
 * If the validation is for an experiment performed in the PlatformTestingApp, use
 * @see PTAThrottleValidator
 *
 */
public class ThrottleValidator extends NodeValidator {

	private ArrayList<Double> throttleValues;
	private int MOVING_AVERAGE_WINDOW = 5; // sumMa computed over this many values
	private double TOLERANCE = 0.02; //sumMa is stable if |sumMa-sumPrevMa| < TOLERANCE*sumMa
	private double DEVIATION_THRESHOLD = 0.1; //if specified throttle rate is more than DEVIATION_THRESHOLD away from
	// sumMa


	public ThrottleValidator(List<NodeData> nodeData) {
		super(nodeData);
	}

	@Override
	public void validate() throws IOException {
		checkThrottledTransPerSec();
	}

	/**
	 * Currently returns true since this validation is more informational than a
	 * hard pass/fail thing.  It's possible that throttling had no effect since transaction rates
	 * fluctuated wildly.  That doesn't make it "wrong" to say that validation is not achieved.
	 * Like the stats validator, this is also an information provider to see what rates were achieved
	 * versus what was set.  This might change in the future depending on how throttle is used, and then
	 * we can change it here.
	 *
	 * @return boolean
	 */
	@Override
	public boolean isValid() {
		//
		return true;
	}

	/**
	 * Set the array of throttleValues which is used for comparing against the trans/sec seen in the CSV file
	 * using
	 *
	 * @param throttleValues
	 * @see #checkThrottledTransPerSec()
	 */
	void setThrottleValues(ArrayList<Double> throttleValues) {
		this.throttleValues = throttleValues;
	}

	/**
	 * Compute a moving average (MA) of all the trans/sec data using a window size
	 * and tolerance to compare against the set throttle.  Whenever the MA
	 * stabilizes, keep track of the MA that minimizes variation with previous MA
	 * As soon as MA starts increasing or decreasing, compare the MA that minimized variation
	 * against a throttle value provided in the throttleValues array.  The method
	 *
	 * @see #setThrottleValues(ArrayList) should be called to set the throttleValues against which
	 * 		the trans/sec data from the experiment's csv file is being checked against.
	 */
	public void checkThrottledTransPerSec() {

		if (throttleValues == null) return;
		if (throttleValues.size() == 0) return;

		int throttleValuesIndex = 0;
		double sumMa = 0.0;

		int numNodes = nodeData.size();
		//System.out.println("numNodes: " + numNodes);

		CsvReader nodeCsv = nodeData.get(0).getCsvReader();

		CsvStat transSec = nodeCsv.getColumn(PlatformStatNames.TRANSACTIONS_PER_SECOND);

		if (transSec.dataSize() < MOVING_AVERAGE_WINDOW) return;

		for (int i = 0; i < MOVING_AVERAGE_WINDOW; i++)
			sumMa += transSec.asDouble(transSec.getDataElement(i)); // initialization

		double sumMaPrev = sumMa; // previous value of MA
		double minMaDiff = Double.MAX_VALUE; // the minimum value of the |sumMa - sumMaPrev| once sumMa is stable
		double minVarMa = sumMa; // the sumMa that results in minMaDiff

		int dir = 0;
		int dirPrev = -2;

		for (int i = MOVING_AVERAGE_WINDOW; i < transSec.dataSize(); i++) {
			// generate a moving average and stop when average stabilizes

			sumMa = sumMaPrev - transSec.asDouble(transSec.getDataElement(i - MOVING_AVERAGE_WINDOW)) +
					transSec.asDouble(transSec.getDataElement(i));

			if (Double.isNaN(sumMa)) break; // if a string in this column wasn't parsed as double, just stop

			double maDiff = Math.abs(sumMa - sumMaPrev);
			if (maDiff < TOLERANCE * sumMa) {
				dir = 0; //stable
				if (minMaDiff > maDiff) {
					minMaDiff = maDiff;
					minVarMa = sumMa;
				}
			} else {
				dir = 1; //increasing or decreasing
				minMaDiff = Double.MAX_VALUE;
			}

			if (dir - dirPrev != 0) {
				// checking dirPrev so that we are switching from stable to increasing/decreasing
				// this allows us to have the "best" stable value of sumMa, the one that minimized the variation
				if (dirPrev == 0) {
					// check a throttle value in config versus sumMa
					if (throttleValuesIndex >= throttleValues.size()) {
						//addWarning("\tError: more stable moving averages detected than throttle values");
					} else {
						double averageTrans = minVarMa / MOVING_AVERAGE_WINDOW / numNodes;
						if (Math.abs(throttleValues.get(
								throttleValuesIndex) - averageTrans) > DEVIATION_THRESHOLD * averageTrans) {
							addWarning(String.format("Transactions/sec did not match throttle setting: " +
											"%.3f achieved vs expected value of %.3f",
									averageTrans, throttleValues.get(throttleValuesIndex)));

						}
						addInfo(String.format("Specified throttle value %.3f vs achieved %.3f",
								throttleValues.get(throttleValuesIndex),
								minVarMa / MOVING_AVERAGE_WINDOW / numNodes));
						throttleValuesIndex++;
					}
				}
			}
			sumMaPrev = sumMa;
			dirPrev = dir;

		}
	}
}
