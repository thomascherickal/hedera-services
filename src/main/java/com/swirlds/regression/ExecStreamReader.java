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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecStreamReader {
    private static final Logger log = LogManager.getLogger(Experiment.class);
    private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
    private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");


    public static void outputProcessStreams(String[] execLine) {
        Process process;
        String cmdName = execLine[0];
        try {
            process = Runtime.getRuntime().exec(execLine);

            // Final versions of the the params, to be used within the threads
            final BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Thread that reads std out and feeds the writer given in input
            new Thread() {
                @Override
                public void run() {
                    setupThreadRun(stdOut, cmdName, "input");
                }
            }.start(); // Starts now

            // Thread that reads std err and feeds the writer given err input
            new Thread() {
                @Override
                public void run() {
                    setupThreadRun(stdErr, cmdName, "error");
                }
            }.start(); // Starts now

            // Wait until the end of the process
            process.waitFor();
            log.info(MARKER, "{} exit: {}", cmdName, process.exitValue());
            process.destroy();
        } catch (InterruptedException | IOException e) {
            log.error(ERROR, "{} process was interrupted.", cmdName, e);
        }
    }

    protected static void setupThreadRun(BufferedReader stream, String cmdName, String streamType) {
        String line;
        try {
            while ((line = stream.readLine()) != null) {
                log.info(MARKER, "{} {} line: {}", cmdName, streamType, line);
            }
        } catch (IOException e) {
            log.error(ERROR, "{} {} stream failed", cmdName, streamType, e);
        }
    }

}
