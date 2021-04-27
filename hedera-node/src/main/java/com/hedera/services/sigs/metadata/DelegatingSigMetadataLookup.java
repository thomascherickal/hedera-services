package com.hedera.services.sigs.metadata;

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

import com.hedera.services.context.properties.NodeLocalProperties;
import com.hedera.services.files.HederaFs;
import com.hedera.services.ledger.accounts.BackingStore;
import com.hedera.services.sigs.metadata.lookups.AccountSigMetaLookup;
import com.hedera.services.sigs.metadata.lookups.BackedAccountLookup;
import com.hedera.services.sigs.metadata.lookups.ContractSigMetaLookup;
import com.hedera.services.sigs.metadata.lookups.DefaultFCMapAccountLookup;
import com.hedera.services.sigs.metadata.lookups.DefaultFCMapContractLookup;
import com.hedera.services.sigs.metadata.lookups.DefaultFCMapTopicLookup;
import com.hedera.services.sigs.metadata.lookups.FileSigMetaLookup;
import com.hedera.services.sigs.metadata.lookups.HfsSigMetaLookup;
import com.hedera.services.sigs.metadata.lookups.RetryingFCMapAccountLookup;
import com.hedera.services.sigs.metadata.lookups.SafeLookupResult;
import com.hedera.services.sigs.metadata.lookups.TopicSigMetaLookup;
import com.hedera.services.state.merkle.MerkleAccount;
import com.hedera.services.state.merkle.MerkleEntityId;
import com.hedera.services.state.merkle.MerkleTopic;
import com.hedera.services.stats.MiscRunningAvgs;
import com.hedera.services.stats.MiscSpeedometers;
import com.hedera.services.utils.Pause;
import com.hedera.services.utils.SleepingPause;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.ContractID;
import com.hederahashgraph.api.proto.java.FileID;
import com.hederahashgraph.api.proto.java.ScheduleID;
import com.hederahashgraph.api.proto.java.TokenID;
import com.hederahashgraph.api.proto.java.TopicID;
import com.swirlds.fcmap.FCMap;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Convenience class that gives unified access to Hedera signing metadata by
 * delegating to type-specific lookups.
 *
 * @author Michael Tinker
 */
public class DelegatingSigMetadataLookup implements SigMetadataLookup {
	private final static Pause pause = SleepingPause.SLEEPING_PAUSE;

	private final FileSigMetaLookup fileSigMetaLookup;
	private final AccountSigMetaLookup accountSigMetaLookup;
	private final ContractSigMetaLookup contractSigMetaLookup;
	private final TopicSigMetaLookup topicSigMetaLookup;

	private final Function<TokenID, SafeLookupResult<TokenSigningMetadata>> tokenSigMetaLookup;
	private final Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>> scheduleSigMetaLookup;

	public static DelegatingSigMetadataLookup backedLookupsFor(
			HederaFs hfs,
			BackingStore<AccountID, MerkleAccount> backingAccounts,
			Supplier<FCMap<MerkleEntityId, MerkleTopic>> topics,
			Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts,
			Function<TokenID, SafeLookupResult<TokenSigningMetadata>> tokenLookup,
			Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>> scheduleSigMetaLookup
	) {
		return new DelegatingSigMetadataLookup(
				new HfsSigMetaLookup(hfs),
				new BackedAccountLookup(backingAccounts),
				new DefaultFCMapContractLookup(accounts),
				new DefaultFCMapTopicLookup(topics),
				tokenLookup,
				scheduleSigMetaLookup);
	}

	public static DelegatingSigMetadataLookup defaultLookupsFor(
			HederaFs hfs,
			Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts,
			Supplier<FCMap<MerkleEntityId, MerkleTopic>> topics,
			Function<TokenID, SafeLookupResult<TokenSigningMetadata>> tokenLookup,
			Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>> scheduleLookup
	) {
		return new DelegatingSigMetadataLookup(
				new HfsSigMetaLookup(hfs),
				new DefaultFCMapAccountLookup(accounts),
				new DefaultFCMapContractLookup(accounts),
				new DefaultFCMapTopicLookup(topics),
				tokenLookup,
				scheduleLookup);
	}

	public static DelegatingSigMetadataLookup defaultLookupsPlusAccountRetriesFor(
			HederaFs hfs,
			Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts,
			Supplier<FCMap<MerkleEntityId, MerkleTopic>> topics,
			Function<TokenID, SafeLookupResult<TokenSigningMetadata>> tokenLookup,
			Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>> scheduleLookup,
			int maxRetries,
			int retryWaitIncrementMs,
			MiscRunningAvgs runningAvgs,
			MiscSpeedometers speedometers
	) {
		var accountLookup = new RetryingFCMapAccountLookup(
				accounts,
				maxRetries,
				retryWaitIncrementMs,
				pause,
				runningAvgs,
				speedometers);
		return new DelegatingSigMetadataLookup(
				new HfsSigMetaLookup(hfs),
				accountLookup,
				new DefaultFCMapContractLookup(accounts),
				new DefaultFCMapTopicLookup(topics),
				tokenLookup,
				scheduleLookup);
	}

	public static DelegatingSigMetadataLookup defaultAccountRetryingLookupsFor(
			HederaFs hfs,
			NodeLocalProperties properties,
			Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts,
			Supplier<FCMap<MerkleEntityId, MerkleTopic>> topics,
			Function<TokenID, SafeLookupResult<TokenSigningMetadata>> tokenLookup,
			Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>> scheduleLookup,
			MiscRunningAvgs runningAvgs,
			MiscSpeedometers speedometers
	) {
		var accountLookup = new RetryingFCMapAccountLookup(pause, properties, accounts, runningAvgs, speedometers);
		return new DelegatingSigMetadataLookup(
				new HfsSigMetaLookup(hfs),
				accountLookup,
				new DefaultFCMapContractLookup(accounts),
				new DefaultFCMapTopicLookup(topics),
				tokenLookup,
				scheduleLookup);
	}

	public DelegatingSigMetadataLookup(
			FileSigMetaLookup fileSigMetaLookup,
			AccountSigMetaLookup accountSigMetaLookup,
			ContractSigMetaLookup contractSigMetaLookup,
			TopicSigMetaLookup topicSigMetaLookup,
			Function<TokenID, SafeLookupResult<TokenSigningMetadata>> tokenSigMetaLookup,
			Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>> scheduleSigMetaLookup
	) {
		this.fileSigMetaLookup = fileSigMetaLookup;
		this.accountSigMetaLookup = accountSigMetaLookup;
		this.contractSigMetaLookup = contractSigMetaLookup;
		this.topicSigMetaLookup = topicSigMetaLookup;
		this.tokenSigMetaLookup = tokenSigMetaLookup;
		this.scheduleSigMetaLookup = scheduleSigMetaLookup;
	}

	@Override
	public SafeLookupResult<ContractSigningMetadata> contractSigningMetaFor(ContractID id) {
		return contractSigMetaLookup.safeLookup(id);
	}

	@Override
	public SafeLookupResult<FileSigningMetadata> fileSigningMetaFor(FileID id) {
		return fileSigMetaLookup.safeLookup(id);
	}

	@Override
	public SafeLookupResult<ScheduleSigningMetadata> scheduleSigningMetaFor(ScheduleID id) {
		return scheduleSigMetaLookup.apply(id);
	}

	@Override
	public SafeLookupResult<AccountSigningMetadata> accountSigningMetaFor(AccountID id) {
		return accountSigMetaLookup.safeLookup(id);
	}

	@Override
	public SafeLookupResult<TopicSigningMetadata> topicSigningMetaFor(TopicID id) {
		return topicSigMetaLookup.safeLookup(id);
	}

	@Override
	public SafeLookupResult<TokenSigningMetadata> tokenSigningMetaFor(TokenID id) {
		return tokenSigMetaLookup.apply(id);
	}
}
