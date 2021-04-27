package com.hedera.services.bdd.suites.file.negative;

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
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileContents;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.*;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.MAX_FILE_SIZE_EXCEEDED;

public class AppendFailuresSpec extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(AppendFailuresSpec.class);

	public static void main(String... args) {
		new AppendFailuresSpec().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[] {
						handleRejectsOversized(),
				}
		);
	}

	private HapiApiSpec handleRejectsOversized() {
		byte[] BYTES_3K_MINUS1 = new byte[3 * 1024 - 1];
		Arrays.fill(BYTES_3K_MINUS1, (byte)0xAB);
		byte[] BYTES_1 = new byte[] { (byte)0xAB };

		return defaultHapiSpec("handleRejectsMissingWacl")
				.given(
						getFileContents(APP_PROPERTIES)
								.saveTo("tmp-application.properties"),
						fileUpdate(APP_PROPERTIES)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.overridingProps(Map.of("maxFileSize", "3"))
				).when(
						fileCreate("file")
								.contents(BYTES_3K_MINUS1),
						fileAppend("file")
								.content(BYTES_1),
						fileAppend("file")
								.content(BYTES_1)
								.hasKnownStatus(MAX_FILE_SIZE_EXCEEDED)
				).then(
						fileUpdate(APP_PROPERTIES)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.overridingProps(Map.of("maxFileSize", "1024"))
								.droppingUnmentioned(),
						fileUpdate(APP_PROPERTIES)
								.payingWith(ADDRESS_BOOK_CONTROL)
								.path("tmp-application.properties")
				);
	}


	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
