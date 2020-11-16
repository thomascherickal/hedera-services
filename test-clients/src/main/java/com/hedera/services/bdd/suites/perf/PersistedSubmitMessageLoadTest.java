package com.hedera.services.bdd.suites.perf;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
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
import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.spec.utilops.LoadTest;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.hedera.services.bdd.spec.HapiApiSpec.customHapiSpec;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.randomUtf8Bytes;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.submitMessageTo;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.logIt;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.BUSY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.DUPLICATE_TRANSACTION;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.OK;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.PLATFORM_TRANSACTION_NOT_CREATED;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.SUCCESS;
import static org.apache.commons.lang3.ArrayUtils.addAll;

public class PersistedSubmitMessageLoadTest extends LoadTest {
	private static final Logger log = LogManager.getLogger(PersistedSubmitMessageLoadTest.class);

	private static int messageSize = 256;

	private static String PERSISTENT_PAYER_NAME = "extremelyRich";
	private static String PERSISTENT_TOPIC_NAME = "extremelyInteresting";

	public static void main(String... args) {
		int usedArgs = parseArgs(args);

		if (args.length > (usedArgs)) {
			messageSize = Integer.parseInt(args[usedArgs]);
			log.info("Set messageSize as " + messageSize);
			usedArgs++;
		}

		PersistedSubmitMessageLoadTest suite = new PersistedSubmitMessageLoadTest();
		suite.setReportStats(true);
		suite.runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(runSubmitMessages());
	}

	@Override
	public boolean hasInterestingStats() {
		return true;
	}

	private static HapiApiSpec runSubmitMessages() {
		PerfTestLoadSettings settings = new PerfTestLoadSettings();

		Supplier<HapiSpecOperation[]> submitBurst = () -> new HapiSpecOperation[] {
				opSupplier(settings).get()
		};

		return customHapiSpec("RunSubmitMessages").withProperties(Map.of(
				"persistentEntities.dir.path", "persistent-entities/",
				"ci.properties.map", "mins=1"
		)).given(
				withOpContext((spec, ignore) -> settings.setFrom(spec.setup().ciPropertiesMap())),
				logIt(ignore -> settings.toString())
		).when().then(
				defaultLoadTest(submitBurst, settings)
		);
	}

	private static Supplier<HapiSpecOperation> opSupplier(PerfTestLoadSettings settings) {
		byte[] payload = randomUtf8Bytes(settings.getIntProperty("messageSize", messageSize) - 8);

		var op = submitMessageTo(PERSISTENT_TOPIC_NAME)
				.message(addAll(ByteBuffer.allocate(8).putLong(Instant.now().toEpochMilli()).array(), payload))
				.noLogging()
				.suppressStats(true)
				.payingWith(PERSISTENT_PAYER_NAME)
				.hasRetryPrecheckFrom(PRECHECK_RETRY_STATUSES)
				.hasKnownStatusFrom(SUCCESS, OK)
				.deferStatusResolution();
		if (settings.getBooleanProperty("isChunk", false)) {
			return () -> op.chunkInfo(1, 1).usePresetTimestamp();
		}
		return () -> op;
	}

	private static ResponseCodeEnum[] PRECHECK_RETRY_STATUSES = {
			BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED, INSUFFICIENT_PAYER_BALANCE
	};

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
