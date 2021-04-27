package com.hedera.services.bdd.suites.consensus;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.keys.KeyShape;
import com.hedera.services.bdd.spec.keys.SigControl;
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.keys.ControlForKey.forKey;
import static com.hedera.services.bdd.spec.keys.KeyShape.SIMPLE;
import static com.hedera.services.bdd.spec.keys.KeyShape.listOf;
import static com.hedera.services.bdd.spec.keys.KeyShape.sigs;
import static com.hedera.services.bdd.spec.keys.KeyShape.threshOf;
import static com.hedera.services.bdd.spec.keys.SigControl.OFF;
import static com.hedera.services.bdd.spec.keys.SigControl.ON;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTopicInfo;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTxnRecord;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.asTopicId;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.*;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.*;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.*;

public class SubmitMessageSuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(SubmitMessageSuite.class);

	public static void main(String... args) {
		new SubmitMessageSuite().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(
				topicIdIsValidated(),
				messageIsValidated(),
				messageSubmissionSimple(),
				messageSubmissionSizeChange(),
				messageSubmissionIncreasesSeqNo(),
				messageSubmissionWithSubmitKey(),
				messageSubmissionMultiple(),
				messageSubmissionOverSize(),
				messageSubmissionCorrectlyUpdatesRunningHash(),
				feeAsExpected()
		);
	}

	private HapiApiSpec topicIdIsValidated() {
		return defaultHapiSpec("topicIdIsValidated")
				.given(
						cryptoCreate("nonTopicId")
				)
				.when()
				.then(
						submitMessageTo((String) null)
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_TOPIC_ID),
						submitMessageTo("1.2.3")
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_TOPIC_ID),
						submitMessageTo(spec -> asTopicId(spec.registry().getAccountID("nonTopicId")))
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_TOPIC_ID)
				);
	}

	private HapiApiSpec messageIsValidated() {
		return defaultHapiSpec("messageIsValidated")
				.given(
						createTopic("testTopic")
				)
				.when()
				.then(
						submitMessageTo("testTopic")
                                .clearMessage()
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_TOPIC_MESSAGE),
						submitMessageTo("testTopic")
								.message("")
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_TOPIC_MESSAGE)
				);
	}

	private HapiApiSpec messageSubmissionSimple() {
		return defaultHapiSpec("messageSubmissionSimple")
				.given(
						newKeyNamed("submitKey"),
						createTopic("testTopic")
								.submitKeyName("submitKey")
								.hasRetryPrecheckFrom(BUSY)
				)
				.when(
						cryptoCreate("civilian")
				)
				.then(
						submitMessageTo("testTopic")
								.message("testmessage")
								.payingWith("civilian")
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(SUCCESS)
				);
	}

	private HapiApiSpec messageSubmissionSizeChange() {
		return defaultHapiSpec("messageSubmissionSizeChange")
				.given(
						newKeyNamed("submitKey"),
						createTopic("testTopic")
								.submitKeyName("submitKey")
				)
				.when(
						cryptoCreate("civilian"),
						submitMessageTo("testTopic")
								.message("testmessage")
								.payingWith("civilian")
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(SUCCESS),
						fileUpdate(APP_PROPERTIES)
								.payingWith(GENESIS)
								.overridingProps(Map.of("consensus.message.maxBytesAllowed", "20"))
				)
				.then(
						submitMessageTo("testTopic")
								.message("testmessagetestmessagetestmessagetestmessage")
								.payingWith("civilian")
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(MESSAGE_SIZE_TOO_LARGE),
						fileUpdate(APP_PROPERTIES)
								.payingWith(GENESIS)
								.overridingProps(Map.of("consensus.message.maxBytesAllowed", "1024"))
				);
	}

	private HapiApiSpec messageSubmissionIncreasesSeqNo() {
		KeyShape submitKeyShape = threshOf(2, SIMPLE, SIMPLE, listOf(2));

		return defaultHapiSpec("messageSubmissionIncreasesSeqNo")
				.given(
						createTopic("testTopic").submitKeyShape(submitKeyShape)
				)
				.when(
						getTopicInfo("testTopic").hasSeqNo(0),
						submitMessageTo("testTopic")
								.message("Hello world!")
								.hasRetryPrecheckFrom(BUSY)
				)
				.then(
						getTopicInfo("testTopic").hasSeqNo(1)
				);
	}

	private HapiApiSpec messageSubmissionWithSubmitKey() {
		KeyShape submitKeyShape = threshOf(2, SIMPLE, SIMPLE, listOf(2));

		SigControl validSig = submitKeyShape.signedWith(sigs(ON, OFF, sigs(ON, ON)));
		SigControl invalidSig = submitKeyShape.signedWith(sigs(ON, OFF, sigs(ON, OFF)));

		return defaultHapiSpec("messageSubmissionWithSubmitKey")
				.given(
						newKeyNamed("submitKey").shape(submitKeyShape),
						createTopic("testTopic")
								.submitKeyName("submitKey")
				)
				.when()
				.then(
						submitMessageTo("testTopic")
								.sigControl(forKey("testTopicSubmit", invalidSig))
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_SIGNATURE),
						submitMessageTo("testTopic")
								.sigControl(forKey("testTopicSubmit", validSig))
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(SUCCESS)
				);
	}

	private HapiApiSpec messageSubmissionMultiple() {
		final int numMessages = 10;

		return defaultHapiSpec("messageSubmissionMultiple")
				.given(
						createTopic("testTopic")
								.hasRetryPrecheckFrom(BUSY)
				)
				.when(
						inParallel(
								asOpArray(numMessages, i ->
										submitMessageTo("testTopic")
												.message("message")
												.hasRetryPrecheckFrom(BUSY)
								)
						)
				)
				.then(
						sleepFor(1000),
						getTopicInfo("testTopic").hasSeqNo(numMessages)
				);
	}

	private HapiApiSpec messageSubmissionOverSize() {
		final byte[] messageBytes = new byte[8192]; // 8k
		Arrays.fill(messageBytes, (byte) 0b1);

		return defaultHapiSpec("messageSubmissionOverSize")
				.given(
						newKeyNamed("submitKey"),
						createTopic("testTopic")
								.submitKeyName("submitKey")
								.hasRetryPrecheckFrom(BUSY)
				)
				.when()
				.then(
						submitMessageTo("testTopic")
								.message(new String(messageBytes))
								.hasPrecheckFrom(TRANSACTION_OVERSIZE, BUSY)
				);
	}

	private HapiApiSpec feeAsExpected() {
		final byte[] messageBytes = new byte[100]; // 4k
		Arrays.fill(messageBytes, (byte) 0b1);
		return defaultHapiSpec("feeAsExpected")
				.given(
						cryptoCreate("payer")
								.hasRetryPrecheckFrom(BUSY),
						createTopic("testTopic")
								.submitKeyName("payer")
								.hasRetryPrecheckFrom(BUSY)
				)
				.when(
						submitMessageTo("testTopic")
								.blankMemo()
								.payingWith("payer")
								.message(new String(messageBytes))
								.hasRetryPrecheckFrom(BUSY)
								.via("submitMessage")
				)
				.then(
						sleepFor(1000),
						validateChargedUsd("submitMessage", 0.0001)
				);
	}

	private HapiApiSpec messageSubmissionCorrectlyUpdatesRunningHash() {
		String topic = "testTopic";
		String message1 = "Hello world!";
		String message2 = "Hello world again!";
		String nonsense = "Nonsense";
		String message3 = "Goodbye!";

		return defaultHapiSpec("messageSubmissionCorrectlyUpdatesRunningHash")
				.given(
						createTopic(topic),
						getTopicInfo(topic)
								.hasSeqNo(0)
								.hasRunningHash(new byte[48])
								.saveRunningHash()
				)
				.when(
						submitMessageTo(topic)
								.message(message1)
								.hasRetryPrecheckFrom(BUSY)
								.via("submitMessage1")
				)
				.then(
						getTxnRecord("submitMessage1")
								.hasCorrectRunningHash(topic, message1),
						submitMessageTo(topic)
								.message(message2)
								.hasRetryPrecheckFrom(BUSY)
								.via("submitMessage2"),
						getTxnRecord("submitMessage2")
								.hasCorrectRunningHash(topic, message2),
						submitMessageTo(topic)
								.message(nonsense)
								.via("nonsense")
								.chunkInfo(3, 4)
								.hasRetryPrecheckFrom(BUSY)
								.hasKnownStatus(INVALID_CHUNK_NUMBER),
						getTxnRecord("nonsense")
								.hasCorrectRunningHash(topic, message2).logged(),
						submitMessageTo(topic)
								.message(message3)
								.hasRetryPrecheckFrom(BUSY)
								.via("submitMessage3"),
						getTxnRecord("submitMessage3")
								.hasCorrectRunningHash(topic, message3)
				);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
