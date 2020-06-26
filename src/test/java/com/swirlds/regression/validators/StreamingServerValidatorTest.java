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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingServerValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/sha1test/sha1sum-success-noevents",
			"logs/sha1test/sha1sum-success-events",
			"logs/sha1test/sha1sum-success-uneven-events",
			"logs/sha1test/evtssig-success-missing-last-one"
	})
	void validateSuccess(final String testDir) {
		final List<StreamingServerData> data = ValidatorTestUtil.loadStreamingServerData(testDir, StreamType.EVENT);
		final StreamingServerValidator validator = new StreamingServerValidator(data, false, StreamType.EVENT);
		validator.validate();

		System.out.println(validator.concatAllMessages());

		assertTrue(validator.isValid());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/sha1test/sha1sum-fail-noevents",
			"logs/sha1test/sha1sum-fail-uneven-events",
			"logs/sha1test/sha1sum-fail-empty-events",
			"logs/sha1test/sha1sum-fail-empty-total-file"
	})
	void validateFailure(final String testDir) {
		final List<StreamingServerData> data = ValidatorTestUtil.loadStreamingServerData(testDir, StreamType.EVENT);
		final StreamingServerValidator validator = new StreamingServerValidator(data, false, StreamType.EVENT);
		validator.validate();

		System.out.println(validator.concatAllMessages());

		assertFalse(validator.isValid());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/sha1test/sha1sum-fail-nototal"
	})
	void validateException(final String testDir) {
		Throwable rte = assertThrows(RuntimeException.class,
				() -> ValidatorTestUtil.loadStreamingServerData(testDir, StreamType.EVENT));
		assertEquals("Cannot find log files in: " + testDir, rte.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "logs/sha1test/evtssig-fail-notmatching" })
	void checksForInvalidEvgsSigInNode0003(final String testDir) {
		final List<StreamingServerData> data = ValidatorTestUtil.loadStreamingServerData(testDir, StreamType.EVENT);
		final StreamingServerValidator validator = new StreamingServerValidator(data, false, StreamType.EVENT);
		validator.validate();

		System.out.println(validator.concatAllMessages());

		assertFalse(validator.isValid());
		final String errorMessage = validator.errorMessages.get(0);
		assertEquals("The contents of two nodes don't match:\n" +
				"\n" +
				"Reference Node 0: \n" +
				"2019-07-23T01_40_50.083046Z.evts_sig\n" +
				"2019-07-23T01_41_00.032741Z.evts_sig\n" +
				"2019-07-23T01_42_10.051057Z.evts_sig\n" +
				"2019-07-23T01_43_20.100352Z.evts_sig\n" +
				"\n" +
				"Validating node 3: \n" +
				"2019-07-23T01_40_50.083046Z.evts_sig\n" +
				"2019-07-23T01_41_00.032741Z.evts_sig\n" +
				"2019-07-23T01_42_10.051057Z.evts_sig\n" +
				"2019-07-23T01_43_20.100353Z.evts_sig\n" +
				"\n" +
				"--- End of diff\n" +
				"\n", errorMessage);
	}

	@ParameterizedTest
	@ValueSource(strings = { "logs/sha1test/evtssig-success-missing-two" })
	void checksForInvalidEvgsSigInNode0003MisssingTwoFiles(final String testDir) {
		final List<StreamingServerData> data = ValidatorTestUtil.loadStreamingServerData(testDir, StreamType.EVENT);
		final StreamingServerValidator validator = new StreamingServerValidator(data, false, StreamType.EVENT);
		validator.validate();

		System.out.println(validator.concatAllMessages());

		assertTrue(validator.isValid());
	}
}