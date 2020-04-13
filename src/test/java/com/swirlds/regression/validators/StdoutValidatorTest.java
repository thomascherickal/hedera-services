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

package com.swirlds.regression.validators;

import com.swirlds.regression.logs.LogReader;
import com.swirlds.regression.logs.StdoutLogParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdoutValidatorTest {
	private static final String STDOUT =
			"Reading the settings from the file:        /home/ubuntu/remoteExperiment/settings.txt\n" +
					"Reading the configuration from the file:   /home/ubuntu/remoteExperiment/config.txt\n\n" +
					"***** ERROR: Couldn't load app /home/ubuntu/remoteExperiment/data/apps/PlatformTestingDemo.jar " +
					"Creating keys, because there are no files: /home/ubuntu/remoteExperiment/data/keys/*.pfx\n" +
					"Could not load libOpenCL.so, error libOpenCL.so: cannot open shared object file: No such file or" +
					" directory";

	private static final String STDOUT_CLEAN =
			"Reading the settings from the file:        /home/ubuntu/remoteExperiment/settings.txt\n" +
					"Reading the configuration from the file:   /home/ubuntu/remoteExperiment/config.txt\n" +
					"Creating keys, because there are no files: /home/ubuntu/remoteExperiment/data/keys/*.pfx\n" +
					"This computer has an internal IP address:  172.31.13.113";

	@Test
	void validate() throws IOException {
		StdoutValidator v = new StdoutValidator(
				List.of(
						new NodeData(
								null,
								null,
								LogReader.createReader(
										new StdoutLogParser(),
										new ByteArrayInputStream(STDOUT.getBytes())
								)
						)
				)
		);

		v.validate();
		assertTrue(v.hasErrors());


	}

	@Test
	void validateClean() throws IOException {
		StdoutValidator v = new StdoutValidator(
				List.of(
						new NodeData(
								null,
								null,
								LogReader.createReader(
										new StdoutLogParser(),
										new ByteArrayInputStream(STDOUT_CLEAN.getBytes())
								)
						)
				)
		);
		v.validate();
		assertFalse(v.hasErrors());
	}
}