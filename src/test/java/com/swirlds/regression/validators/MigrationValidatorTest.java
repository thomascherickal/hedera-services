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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigrationValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/migrationFCM/migrate10K"
	})
	void validateMigrationWithMissingLog(String testDir) throws IOException {
		final List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "MigrationTestingApp", 1);
		nodeData.set(0, new NodeData(null, null));
		final NodeValidator validator = new MigrationValidator(nodeData);
		validator.validate();
		final List<String> errorMessages = validator.getErrorMessages();

		assertFalse(errorMessages.isEmpty());
		assertFalse(validator.isValid());
		assertEquals(1, errorMessages.size());
		assertEquals("Could not load log for node 0", errorMessages.get(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/migrationFCM/migrate10K",
			"logs/migrationFCM/migrate20K"
	})
	void validateMigrationLogs(String testDir) throws IOException {
		final List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "MigrationTestingApp", 1);
		final NodeValidator validator = new MigrationValidator(nodeData);
		validator.validate();
		final List<String> errorMessages = validator.getErrorMessages();

		assertTrue(errorMessages.isEmpty());
		assertTrue(validator.isValid());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/migrationFCM/migrate10KFailedToLoad"
	})
	void validateFailToLoadMigrationLogs(String testDir) throws IOException {
		final List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "MigrationStats", 1);
		final NodeValidator validator = new MigrationValidator(nodeData);
		validator.validate();
		final List<String> errorMessages = validator.getErrorMessages();

		assertFalse(errorMessages.isEmpty());
		assertFalse(validator.isValid());
		assertEquals(1, errorMessages.size());
		assertEquals("Node 0 started loading state from disk but didn't finish", errorMessages.get(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/migrationFCM/migrate20KFailedToStartLoading"
	})
	void validateFailToStartLoading(final String testDir) throws IOException {
		final List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "MigrationTestingApp", 1);
		final NodeValidator validator = new MigrationValidator(nodeData);
		validator.validate();
		final List<String> errorMessages = validator.getErrorMessages();

		assertFalse(errorMessages.isEmpty());
		assertFalse(validator.isValid());
		assertEquals(1, errorMessages.size());
		assertEquals("Node 0 didn't start loading state from disk", errorMessages.get(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/migrationFCM/migrate20KFailedToStartProcessing"
	})
	void validateFailToStartProcessingTransactions(final String testDir) throws IOException {
		final List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "MigrationTestingApp", 1);
		final NodeValidator validator = new MigrationValidator(nodeData);
		validator.validate();
		final List<String> errorMessages = validator.getErrorMessages();

		assertFalse(errorMessages.isEmpty());
		assertFalse(validator.isValid());
		assertEquals(1, errorMessages.size());
		assertEquals("Node 2 didn't start processing transactions", errorMessages.get(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"logs/migrationFCM/migrate20KFailedToFinishProcessing"
	})
	void validateFailToFinishProcessingTransactions(final String testDir) throws IOException {
		final List<NodeData> nodeData = ValidatorTestUtil.loadNodeData(testDir, "MigrationTestingApp", 1);
		final NodeValidator validator = new MigrationValidator(nodeData);
		validator.validate();
		final List<String> errorMessages = validator.getErrorMessages();

		assertFalse(errorMessages.isEmpty());
		assertFalse(validator.isValid());
		assertEquals(1, errorMessages.size());
		assertEquals("Node 1 didn't finish processing transactions", errorMessages.get(0));
	}
}
