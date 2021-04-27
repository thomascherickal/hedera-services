package com.hedera.services.yahcli.suites;

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

import com.google.protobuf.ByteString;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.suites.HapiApiSuite;
import com.hedera.services.bdd.suites.utils.sysfiles.serdes.AddrBkJsonToGrpcBytes;
import com.hedera.services.bdd.suites.utils.sysfiles.serdes.SysFileSerde;
import com.hedera.services.yahcli.output.CommonMessages;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import static com.hedera.services.bdd.spec.utilops.UtilVerbs.updateLargeFile;
import static com.hedera.services.bdd.suites.utils.sysfiles.serdes.StandardSerdes.SYS_FILE_SERDES;
import static com.hedera.services.yahcli.output.CommonMessages.COMMON_MESSAGES;

public class SysFileUploadSuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(SysFileUploadSuite.class);

	private final long sysFileId;
	private final String srcDir;
	private final Map<String, String> specConfig;

	public SysFileUploadSuite(final String srcDir, final Map<String, String> specConfig, final String sysFile) {
		this.srcDir = srcDir;
		this.specConfig = specConfig;
		this.sysFileId = Utils.rationalized(sysFile);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[] {
				uploadSysFiles(),
		});
	}

	private HapiApiSpec uploadSysFiles() {
		final ByteString uploadData = appropriateContents(sysFileId);

		return HapiApiSpec.customHapiSpec(String.format("UploadSystemFile-%s", sysFileId)).withProperties(
				specConfig
		).given().when().then(
				updateLargeFile(
						DEFAULT_PAYER,
						String.format("0.0.%d", sysFileId),
						uploadData,
						true,
						OptionalLong.of(10_000_000_000L),
						updateOp -> updateOp
								.alertingPre(COMMON_MESSAGES::uploadBeginning)
								.alertingPost(COMMON_MESSAGES::uploadEnding),
						appendOp -> appendOp
								.alertingPre(COMMON_MESSAGES::appendBeginning)
								.alertingPost(COMMON_MESSAGES::appendEnding)
				)
		);
	}

	private ByteString appropriateContents(final Long fileNum) {
		SysFileSerde<String> serde = SYS_FILE_SERDES.get(fileNum);
		String name = serde.preferredFileName();
		String loc = srcDir + File.separator + name;
		try {
			var stylized = Files.readString(Paths.get(loc));
			return ByteString.copyFrom(serde.toValidatedRawFile(stylized));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read update file @ '" + loc + "'!", e);
		}
	}
}
