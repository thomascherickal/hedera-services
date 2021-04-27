package com.hedera.services.sigs.metadata.lookups;

/*-
 * ‌
 * Hedera Services Node
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

import com.hedera.services.files.HederaFs;
import com.hedera.services.sigs.metadata.FileSigningMetadata;
import com.hederahashgraph.api.proto.java.FileID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hedera.services.sigs.order.KeyOrderingFailure.MISSING_FILE;

/**
 * Trivial file metadata lookup.
 *
 * @author Michael Tinker
 */
public class HfsSigMetaLookup implements FileSigMetaLookup {
	private static final Logger log = LogManager.getLogger(HfsSigMetaLookup.class);

	private final HederaFs hfs;

	public HfsSigMetaLookup(HederaFs hfs) {
		this.hfs = hfs;
	}

	@Override
	public SafeLookupResult<FileSigningMetadata> safeLookup(FileID id) {
		if (!hfs.exists(id)) {
			return SafeLookupResult.failure(MISSING_FILE);
		}
		return new SafeLookupResult<>(new FileSigningMetadata(hfs.getattr(id).getWacl()));
	}
}
