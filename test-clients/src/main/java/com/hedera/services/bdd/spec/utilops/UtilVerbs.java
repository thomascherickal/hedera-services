package com.hedera.services.bdd.spec.utilops;

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
import com.hedera.services.bdd.spec.infrastructure.OpProvider;
import com.hedera.services.bdd.spec.transactions.consensus.HapiMessageSubmit;
import com.hedera.services.bdd.spec.transactions.crypto.HapiCryptoTransfer;
import com.hedera.services.bdd.spec.transactions.file.HapiFileAppend;
import com.hedera.services.bdd.spec.transactions.file.HapiFileCreate;
import com.hedera.services.bdd.spec.transactions.file.HapiFileUpdate;
import com.hedera.services.bdd.spec.utilops.checks.VerifyGetBySolidityIdNotSupported;
import com.hedera.services.bdd.spec.utilops.checks.VerifyGetLiveHashNotSupported;
import com.hedera.services.bdd.spec.utilops.checks.VerifyGetFastRecordNotSupported;
import com.hedera.services.bdd.spec.utilops.checks.VerifyGetStakersNotSupported;
import com.hedera.services.bdd.spec.utilops.grouping.InBlockingOrder;
import com.hedera.services.bdd.spec.utilops.grouping.ParallelSpecOps;
import com.hedera.services.bdd.spec.utilops.inventory.NewSpecKey;
import com.hedera.services.bdd.spec.utilops.inventory.NewSpecKeyList;
import com.hedera.services.bdd.spec.utilops.inventory.RecordSystemProperty;
import com.hedera.services.bdd.spec.utilops.inventory.SpecKeyFromLiteral;
import com.hedera.services.bdd.spec.utilops.inventory.SpecKeyFromMnemonic;
import com.hedera.services.bdd.spec.utilops.inventory.SpecKeyFromPem;
import com.hedera.services.bdd.spec.utilops.inventory.UsableTxnId;
import com.hedera.services.bdd.spec.utilops.pauses.HapiSpecSleep;
import com.hedera.services.bdd.spec.utilops.pauses.HapiSpecWaitUntil;
import com.hedera.services.bdd.spec.utilops.pauses.NodeLivenessTimeout;
import com.hedera.services.bdd.spec.utilops.streams.RecordStreamVerification;
import com.hedera.services.bdd.spec.utilops.throughput.FinishThroughputObs;
import com.hedera.services.bdd.spec.utilops.throughput.StartThroughputObs;
import com.hedera.services.bdd.suites.crypto.CryptoTransferSuite;
import com.hedera.services.bdd.suites.perf.HCSChunkingRealisticPerfSuite;
import com.hedera.services.bdd.suites.perf.PerfTestLoadSettings;
import com.hedera.services.bdd.suites.utils.sysfiles.serdes.FeesJsonToGrpcBytes;
import com.hedera.services.bdd.suites.utils.sysfiles.serdes.SysFileSerde;
import com.hederahashgraph.api.proto.java.AccountAmount;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.ContractID;
import com.hederahashgraph.api.proto.java.CurrentAndNextFeeSchedule;
import com.hederahashgraph.api.proto.java.FeeSchedule;
import com.hederahashgraph.api.proto.java.HederaFunctionality;
import com.hederahashgraph.api.proto.java.ServicesConfigurationList;
import com.hederahashgraph.api.proto.java.Setting;
import com.hederahashgraph.api.proto.java.TransactionRecord;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.HapiPropertySource;
import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.spec.queries.meta.HapiGetTxnRecord;
import com.hedera.services.bdd.spec.transactions.system.HapiFreeze;
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hedera.services.bdd.spec.HapiPropertySource.asAccount;
import static com.hedera.services.bdd.spec.HapiPropertySource.asAccountString;
import static com.hedera.services.bdd.spec.assertions.ContractInfoAsserts.contractWith;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getContractInfo;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileContents;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getTxnRecord;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.BYTES_4K;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.asId;
import static com.hedera.services.bdd.spec.transactions.TxnUtils.asTransactionID;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileAppend;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileUpdate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.submitMessageTo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.tokenDissociate;
import static com.hedera.services.bdd.spec.transactions.crypto.HapiCryptoTransfer.tinyBarsFromTo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoTransfer;
import static com.hedera.services.bdd.spec.transactions.token.TokenMovement.moving;
import static com.hedera.services.bdd.spec.utilops.CustomSpecAssert.allRunFor;
import static com.hedera.services.bdd.suites.HapiApiSuite.ADDRESS_BOOK_CONTROL;
import static com.hedera.services.bdd.suites.HapiApiSuite.APP_PROPERTIES;
import static com.hedera.services.bdd.suites.HapiApiSuite.FEE_SCHEDULE;
import static com.hedera.services.bdd.suites.HapiApiSuite.GENESIS;
import static com.hedera.services.bdd.suites.HapiApiSuite.EXCHANGE_RATE_CONTROL;
import static com.hedera.services.bdd.suites.HapiApiSuite.ONE_HBAR;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.*;
import static org.junit.Assert.assertEquals;

public class UtilVerbs {
	public static HapiFreeze freeze() {
		return new HapiFreeze();
	}

	/* Some fairly simple utility ops */
	public static InBlockingOrder blockingOrder(HapiSpecOperation... ops) {
		return new InBlockingOrder(ops);
	}

	public static NodeLivenessTimeout withLiveNode(String node) {
		return new NodeLivenessTimeout(node);
	}

	public static <T> RecordSystemProperty<T> recordSystemProperty(
			String property,
			Function<String, T> converter,
			Consumer<T> historian
	) {
		return new RecordSystemProperty<>(property, converter, historian);
	}

	public static SourcedOp sourcing(Supplier<HapiSpecOperation> source) {
		return new SourcedOp(source);
	}

	public static HapiSpecSleep sleepFor(long timeMs) {
		return new HapiSpecSleep(timeMs);
	}

	public static HapiSpecWaitUntil waitUntil(String timeOfDay) throws ParseException {
		return new HapiSpecWaitUntil(timeOfDay);
	}

	public static UsableTxnId usableTxnIdNamed(String txnId) {
		return new UsableTxnId(txnId);
	}

	public static SpecKeyFromMnemonic keyFromMnemonic(String name, String mnemonic) {
		return new SpecKeyFromMnemonic(name, mnemonic);
	}

	public static SpecKeyFromLiteral keyFromLiteral(String name, String hexEncodedPrivateKey) {
		return new SpecKeyFromLiteral(name, hexEncodedPrivateKey);
	}

	public static HapiSpecOperation expectedEntitiesExist() {
		return withOpContext((spec, opLog) -> spec.persistentEntities().runExistenceChecks());
	}

	public static SpecKeyFromPem keyFromPem(String pemLoc) {
		return new SpecKeyFromPem(pemLoc);
	}

	public static SpecKeyFromPem keyFromPem(Supplier<String> pemLocFn) {
		return new SpecKeyFromPem(pemLocFn);
	}

	public static NewSpecKey newKeyNamed(String key) {
		return new NewSpecKey(key);
	}

	public static NewSpecKeyList newKeyListNamed(String key, List<String> childKeys) {
		return new NewSpecKeyList(key, childKeys);
	}

	public static ParallelSpecOps inParallel(HapiSpecOperation... subs) {
		return new ParallelSpecOps(subs);
	}

	public static CustomSpecAssert assertionsHold(CustomSpecAssert.ThrowingConsumer custom) {
		return new CustomSpecAssert(custom);
	}

	public static CustomSpecAssert addLogInfo(CustomSpecAssert.ThrowingConsumer custom) {
		return new CustomSpecAssert(custom);
	}

	public static CustomSpecAssert withOpContext(CustomSpecAssert.ThrowingConsumer custom) {
		return new CustomSpecAssert(custom);
	}

	public static BalanceSnapshot balanceSnapshot(String name, String forAccount) {
		return new BalanceSnapshot(forAccount, name);
	}

	public static BalanceSnapshot balanceSnapshot(Function<HapiApiSpec, String> nameFn, String forAccount) {
		return new BalanceSnapshot(forAccount, nameFn);
	}

	public static StartThroughputObs startThroughputObs(String name) {
		return new StartThroughputObs(name);
	}

	public static FinishThroughputObs finishThroughputObs(String name) {
		return new FinishThroughputObs(name);
	}

	public static VerifyGetLiveHashNotSupported getClaimNotSupported() {
		return new VerifyGetLiveHashNotSupported();
	}

	public static VerifyGetStakersNotSupported getStakersNotSupported() {
		return new VerifyGetStakersNotSupported();
	}

	public static VerifyGetFastRecordNotSupported getFastRecordNotSupported() {
		return new VerifyGetFastRecordNotSupported();
	}

	public static VerifyGetBySolidityIdNotSupported getBySolidityIdNotSupported() {
		return new VerifyGetBySolidityIdNotSupported();
	}

	public static RunLoadTest runLoadTest(Supplier<HapiSpecOperation[]> opSource) {
		return new RunLoadTest(opSource);
	}

	public static LogMessage logIt(String msg) {
		return new LogMessage(msg);
	}

	public static LogMessage logIt(Function<HapiApiSpec, String> messageFn) {
		return new LogMessage(messageFn);
	}

	public static ProviderRun runWithProvider(Function<HapiApiSpec, OpProvider> provider) {
		return new ProviderRun(provider);
	}

	public static HapiSpecOperation overriding(String property, String value) {
		return fileUpdate(APP_PROPERTIES)
				.payingWith(ADDRESS_BOOK_CONTROL)
				.overridingProps(Map.of(property, "" + value));
	}

	public static CustomSpecAssert exportAccountBalances(Supplier<String> acctBalanceFile) {
		return new CustomSpecAssert((spec, log) -> {
			spec.exportAccountBalances(acctBalanceFile);
		});
	}


	/* Stream validation. */
	public static RecordStreamVerification verifyRecordStreams(Supplier<String> baseDir) {
		return new RecordStreamVerification(baseDir);
	}

	/* Some more complicated ops built from primitive sub-ops */
	public static CustomSpecAssert recordFeeAmount(String forTxn, String byName) {
		return new CustomSpecAssert((spec, workLog) -> {
			HapiGetTxnRecord subOp = getTxnRecord(forTxn);
			allRunFor(spec, subOp);
			TransactionRecord record = subOp.getResponseRecord();
			long fee = record.getTransactionFee();
			spec.registry().saveAmount(byName, fee);
		});
	}

	public static HapiSpecOperation fundAnAccount(String account) {
		return withOpContext((spec, ctxLog) -> {
			if (!asId(account, spec).equals(asId(GENESIS, spec))) {
				HapiCryptoTransfer subOp =
						cryptoTransfer(tinyBarsFromTo(GENESIS, account, HapiApiSuite.ADEQUATE_FUNDS));
				CustomSpecAssert.allRunFor(spec, subOp);
			}
		});
	}

	public static Setting from(String name, String value) {
		return Setting.newBuilder()
				.setName(name)
				.setValue(value)
				.build();
	}

	public static HapiSpecOperation chunkAFile(String filePath, int chunkSize, String payer, String topic) {
		return chunkAFile(filePath, chunkSize, payer, topic, new AtomicLong(-1));
	}

	public static HapiSpecOperation chunkAFile(String filePath, int chunkSize, String payer, String topic,
											   AtomicLong count) {
		return withOpContext((spec, ctxLog) -> {
			List<HapiSpecOperation> opsList = new ArrayList<HapiSpecOperation>();
			String overriddenFile = new String(filePath);
			int overriddenChunkSize = chunkSize;
			String overriddenTopic = new String(topic);
			boolean validateRunningHash = false;

			long currentCount = count.getAndIncrement();
			if (currentCount >= 0) {
				var ciProperties = spec.setup().ciPropertiesMap();
				if (null != ciProperties) {
					if (ciProperties.has("file")) {
						overriddenFile = ciProperties.get("file");
					}
					if (ciProperties.has("chunkSize")) {
						overriddenChunkSize = ciProperties.getInteger("chunkSize");
					}
					if (ciProperties.has("validateRunningHash")) {
						validateRunningHash = ciProperties.getBoolean("validateRunningHash");
					}
					int threads = PerfTestLoadSettings.DEFAULT_THREADS;
					if (ciProperties.has("threads")) {
						threads = ciProperties.getInteger("threads");
					}
					int factor = HCSChunkingRealisticPerfSuite.DEFAULT_COLLISION_AVOIDANCE_FACTOR;
					if (ciProperties.has("collisionAvoidanceFactor")) {
						factor = ciProperties.getInteger("collisionAvoidanceFactor");
					}
					overriddenTopic += currentCount % (threads * factor);
				}
			}
			ByteString msg = ByteString.copyFrom(
					Files.readAllBytes(Paths.get(overriddenFile))
			);
			int size = msg.size();
			int totalChunks = (size + overriddenChunkSize - 1) / overriddenChunkSize;
			int position = 0;
			int currentChunk = 0;
			var initialTransactionID = asTransactionID(spec, Optional.of(payer));

			while (position < size) {
				++currentChunk;
				int newPosition = Math.min(size, position + overriddenChunkSize);
				ByteString subMsg = msg.substring(position, newPosition);
				HapiMessageSubmit subOp = submitMessageTo(overriddenTopic)
						.message(subMsg)
						.chunkInfo(totalChunks, currentChunk, initialTransactionID)
						.payingWith(payer)
						.hasKnownStatus(SUCCESS)
						.hasRetryPrecheckFrom(BUSY, DUPLICATE_TRANSACTION, PLATFORM_TRANSACTION_NOT_CREATED,
								INSUFFICIENT_PAYER_BALANCE)
						.noLogging()
						.suppressStats(true);
				if (1 == currentChunk) {
					subOp = subOp.usePresetTimestamp();
				}
				if (validateRunningHash) {
					String txnName = "submitMessage-" + overriddenTopic + "-" + currentChunk;
					HapiGetTxnRecord validateOp = getTxnRecord(txnName)
							.hasCorrectRunningHash(overriddenTopic, subMsg.toByteArray())
							.payingWith(payer)
							.noLogging();
					opsList.add(subOp.via(txnName));
					opsList.add(validateOp);
				} else {
					opsList.add(subOp.deferStatusResolution());
				}
				position = newPosition;
			}

			CustomSpecAssert.allRunFor(spec, opsList);
		});
	}

	public static HapiSpecOperation ensureDissociated(String account, List<String> tokens) {
		return withOpContext((spec, opLog) -> {
			var query = getAccountBalance(account);
			allRunFor(spec, query);
			var answer = query.getResponse().getCryptogetAccountBalance().getTokenBalancesList();
			for (String token : tokens) {
				var tid = spec.registry().getTokenID(token);
				var match = answer.stream().filter(tb -> tb.getTokenId().equals(tid)).findAny();
				if (match.isPresent()) {
					var tb = match.get();
					opLog.info(
							"Account '{}' is associated to token '{}' ({})",
							account, token, HapiPropertySource.asTokenString(tid));
					if (tb.getBalance() > 0) {
						opLog.info("  -->> Balance is {}, transferring to treasury", tb.getBalance());
						var treasury = spec.registry().getTreasury(token);
						var xfer = cryptoTransfer(moving(tb.getBalance(), token).between(account, treasury));
						allRunFor(spec, xfer);
					}
					opLog.info("  -->> Dissociating '{}' from '{}' now", account, token);
					var dis = tokenDissociate(account, token);
					allRunFor(spec, dis);
				}
			}
		});
	}

	public static HapiSpecOperation makeFree(HederaFunctionality function) {
		return reduceFeeFor(function, 0L, 0L, 0L);
	}

	public static HapiSpecOperation reduceFeeFor(HederaFunctionality function,
			long tinyBarMaxNodeFee, long tinyBarMaxNetworkFee, long tinyBarMaxServiceFee) {
		return withOpContext((spec, opLog) -> {
			if (!spec.setup().defaultNode().equals(asAccount("0.0.3"))) {
				opLog.info("Sleeping to wait for fee reduction...");
				Thread.sleep(20000);
				return;
			}
			opLog.info("Sleeping so not to spoil/fail the fee initializations on other clients...");
			Thread.sleep(10000);
			opLog.info("Reducing fee for {}...", function);
			var query = getFileContents(FEE_SCHEDULE).payingWith(GENESIS);
			allRunFor(spec, query);
			byte[] rawSchedules =
					query.getResponse().getFileGetContents().getFileContents().getContents().toByteArray();

			// Convert from tinyBar to one-thousandth of a tinyCent, the unit of max field in FeeComponents
			long centEquiv = spec.ratesProvider().rates().getCentEquiv();
			long hbarEquiv = spec.ratesProvider().rates().getHbarEquiv();
			long maxNodeFee = tinyBarMaxNodeFee * centEquiv * 1000L / hbarEquiv;
			long maxNetworkFee = tinyBarMaxNetworkFee * centEquiv * 1000L / hbarEquiv;
			long maxServiceFee = tinyBarMaxServiceFee * centEquiv * 1000L / hbarEquiv;

			var perturbedSchedules = CurrentAndNextFeeSchedule.parseFrom(rawSchedules).toBuilder();
			reduceFeeComponentsFor(perturbedSchedules.getCurrentFeeScheduleBuilder(), function,
					maxNodeFee, maxNetworkFee, maxServiceFee);
			reduceFeeComponentsFor(perturbedSchedules.getNextFeeScheduleBuilder(), function,
					maxNodeFee, maxNetworkFee, maxServiceFee);
			var rawPerturbedSchedules = perturbedSchedules.build().toByteString();
			allRunFor(spec, updateLargeFile(GENESIS, FEE_SCHEDULE, rawPerturbedSchedules));
		});
	}

	private static void reduceFeeComponentsFor(FeeSchedule.Builder feeSchedule, HederaFunctionality function,
			long maxNodeFee, long maxNetworkFee, long maxServiceFee) {
		var feeData = feeSchedule.getTransactionFeeScheduleBuilderList()
				.stream()
				.filter(tfs -> tfs.getHederaFunctionality() == function)
				.findAny()
				.get()
				.getFeeDataBuilder();
		feeData.getNodedataBuilder().setMax(maxNodeFee);
		feeData.getNetworkdataBuilder().setMax(maxNetworkFee);
		feeData.getServicedataBuilder().setMax(maxServiceFee);
	}

	public static HapiSpecOperation uploadDefaultFeeSchedules(String payer) {
		return withOpContext((spec, opLog) -> {
			allRunFor(spec, updateLargeFile(payer, FEE_SCHEDULE, defaultFeeSchedules()));
			if (!spec.tryReinitializingFees()) {
				throw new IllegalStateException("New fee schedules won't be available, dying!");
			}
		});
	}

	private static ByteString defaultFeeSchedules() {
		SysFileSerde<String> serde = new FeesJsonToGrpcBytes();
		var baos = new ByteArrayOutputStream();
		try {
			var schedulesIn = HapiFileCreate.class.getClassLoader().getResourceAsStream("FeeSchedule.json");
			if (schedulesIn == null) {
				throw new IllegalStateException("No FeeSchedule.json resource available!");
			}
			schedulesIn.transferTo(baos);
			baos.close();
			baos.flush();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		var stylized = new String(baos.toByteArray());
		return ByteString.copyFrom(serde.toRawFile(stylized));
	}

	public static HapiSpecOperation updateLargeFile(
			String payer,
			String fileName,
			ByteString byteString
	) {
		return updateLargeFile(payer, fileName, byteString, false, OptionalLong.empty());
	}

	public static HapiSpecOperation updateLargeFile(
			String payer,
			String fileName,
			ByteString byteString,
			boolean signOnlyWithPayer,
			OptionalLong tinyBarsToOffer
	) {
		return updateLargeFile(payer, fileName, byteString, signOnlyWithPayer, tinyBarsToOffer, op -> {}, op -> {});
	}

	public static HapiSpecOperation updateLargeFile(
			String payer,
			String fileName,
			ByteString byteString,
			boolean signOnlyWithPayer,
			OptionalLong tinyBarsToOffer,
			Consumer<HapiFileUpdate> updateCustomizer,
			Consumer<HapiFileAppend> appendCustomizer
	) {
		return withOpContext((spec, ctxLog) -> {
			List<HapiSpecOperation> opsList = new ArrayList<HapiSpecOperation>();

			int fileSize = byteString.size();
			int position = Math.min(BYTES_4K, fileSize);

			HapiFileUpdate updateSubOp = fileUpdate(fileName)
					.contents(byteString.substring(0, position))
					.hasKnownStatusFrom(SUCCESS, FEE_SCHEDULE_FILE_PART_UPLOADED)
					.noLogging()
					.payingWith(payer);
			updateCustomizer.accept(updateSubOp);
			if (tinyBarsToOffer.isPresent()) {
				updateSubOp = updateSubOp.fee(tinyBarsToOffer.getAsLong());
			}
			if (signOnlyWithPayer) {
				updateSubOp = updateSubOp.signedBy(payer);
			}
			opsList.add(updateSubOp);

			while (position < fileSize) {
				int newPosition = Math.min(fileSize, position + BYTES_4K);
				var appendSubOp = fileAppend(fileName)
						.content(byteString.substring(position, newPosition).toByteArray())
						.hasKnownStatusFrom(SUCCESS, FEE_SCHEDULE_FILE_PART_UPLOADED)
						.noLogging()
						.payingWith(payer);
				appendCustomizer.accept(appendSubOp);
				if (tinyBarsToOffer.isPresent()) {
					appendSubOp = appendSubOp.fee(tinyBarsToOffer.getAsLong());
				}
				if (signOnlyWithPayer) {
					appendSubOp = appendSubOp.signedBy(payer);
				}
				opsList.add(appendSubOp);
				position = newPosition;
			}

			CustomSpecAssert.allRunFor(spec, opsList);
		});
	}

	public static HapiSpecOperation updateLargeFile(String payer, String fileName, String registryEntry) {
		return withOpContext((spec, ctxLog) -> {
			ByteString bt = ByteString.copyFrom(spec.registry().getBytes(registryEntry));
			CustomSpecAssert.allRunFor(spec, updateLargeFile(payer, fileName, bt));
		});
	}

	public static HapiSpecOperation saveFileToRegistry(String fileName, String registryEntry) {
		return getFileContents(fileName)
				.payingWith(GENESIS)
				.saveToRegistry(registryEntry);
	}

	public static HapiSpecOperation restoreFileFromRegistry(String fileName, String registryEntry) {
		return updateLargeFile(GENESIS, fileName, registryEntry);
	}

	public static HapiSpecOperation contractListWithPropertiesInheritedFrom(
			final String contractList,
			final long expectedSize,
			final String parent) {
		return withOpContext((spec, ctxLog) -> {
			List<HapiSpecOperation> opsList = new ArrayList<HapiSpecOperation>();
			long contractListSize = spec.registry().getAmount(contractList + "Size");
			Assert.assertEquals(contractList + " has bad size!", expectedSize, contractListSize);
			if (contractListSize > 1) {
				ContractID currentID = spec.registry().getContractId(contractList + "0");
				long nextIndex = 1;
				while (nextIndex < contractListSize) {
					ContractID nextID = spec.registry().getContractId(contractList + nextIndex);
					Assert.assertEquals(currentID.getShardNum(), nextID.getShardNum());
					Assert.assertEquals(currentID.getRealmNum(), nextID.getRealmNum());
					Assert.assertTrue(currentID.getContractNum() < nextID.getContractNum());
					currentID = nextID;
					nextIndex++;
				}
			}
			for (long i = 0; i < contractListSize; i++) {
				HapiSpecOperation op = getContractInfo(contractList + i)
						.has(contractWith().propertiesInheritedFrom(parent))
						.logged();
				opsList.add(op);
			}
			CustomSpecAssert.allRunFor(spec, opsList);
		});
	}

	/**
	 * Validates that fee charged for a transaction is within +/- 0.0001$ of
	 * expected fee (taken from pricing calculator)
	 */
	public static CustomSpecAssert validateChargedUsd(String txn, double expectedUsd) {
		return validateChargedUsdWithin(txn, expectedUsd, 1.0);
	}

	public static CustomSpecAssert validateChargedUsdWithin(String txn, double expectedUsd, double allowedPercentDiff) {
		return assertionsHold((spec, assertLog) -> {
			var subOp = getTxnRecord(txn).logged();
			allRunFor(spec, subOp);

			var record = subOp.getResponseRecord();
			double actualUsdCharged = (1.0 * record.getTransactionFee())
					/ ONE_HBAR
					/ record.getReceipt().getExchangeRate().getCurrentRate().getHbarEquiv()
					* record.getReceipt().getExchangeRate().getCurrentRate().getCentEquiv()
					/ 100;
			assertEquals(
					String.format(
							"%s fee (%s) more than %.2f percent different than expected!",
							CryptoTransferSuite.sdec(actualUsdCharged, 4),
							txn,
							allowedPercentDiff),
					expectedUsd,
					actualUsdCharged,
					(allowedPercentDiff / 100.0) * expectedUsd);
		});
	}

	public static CustomSpecAssert getTransactionFee(String txn, StringBuilder feeTableBuilder, String operation) {
		return assertionsHold((spec, asertLog) -> {
			var subOp = getTxnRecord(txn);
			allRunFor(spec, subOp);

			var record = subOp.getResponseRecord();
			double actualUsdCharged = (1.0 * record.getTransactionFee())
					/ ONE_HBAR
					/ record.getReceipt().getExchangeRate().getCurrentRate().getHbarEquiv()
					* record.getReceipt().getExchangeRate().getCurrentRate().getCentEquiv()
					/ 100;

			feeTableBuilder.append(String.format("%30s | %1.5f \t |\n", operation, actualUsdCharged));
		});
	}

	public static HapiSpecOperation[] takeBalanceSnapshots(String... entities) {
		return HapiApiSuite.flattened(
				cryptoTransfer(
						tinyBarsFromTo(GENESIS, EXCHANGE_RATE_CONTROL, 1_000_000_000L)).noLogging(),
				Stream.of(entities).map(account ->
						balanceSnapshot(
								spec -> asAccountString(spec.registry().getAccountID(account)) + "Snapshot",
								account).payingWith(EXCHANGE_RATE_CONTROL)
				).toArray(n -> new HapiSpecOperation[n]));
	}

	public static HapiSpecOperation validateRecordTransactionFees(String txn) {
		return validateRecordTransactionFees(txn,
				Set.of(
						HapiPropertySource.asAccount("0.0.3"),
						HapiPropertySource.asAccount("0.0.98")));
	}

	public static HapiSpecOperation validateRecordTransactionFees(String txn, Set<AccountID> feeRecipients) {
		return assertionsHold((spec, assertLog) -> {
			HapiGetTxnRecord subOp = getTxnRecord(txn)
					.logged()
					.payingWith(EXCHANGE_RATE_CONTROL)
					.expectStrictCostAnswer();
			allRunFor(spec, subOp);
			TransactionRecord record = subOp.getResponse().getTransactionGetRecord().getTransactionRecord();
			long realFee = record.getTransferList().getAccountAmountsList()
					.stream()
					.filter(aa -> feeRecipients.contains(aa.getAccountID()))
					.mapToLong(AccountAmount::getAmount)
					.sum();
			Assert.assertEquals("Inconsistent transactionFee field!", realFee, record.getTransactionFee());
		});
	}

	public static HapiSpecOperation validateTransferListForBalances(String txn, List<String> accounts) {
		return validateTransferListForBalances(List.of(txn), accounts);
	}

	public static HapiSpecOperation validateTransferListForBalances(
			String txn,
			List<String> accounts,
			Set<String> wereDeleted
	) {
		return validateTransferListForBalances(List.of(txn), accounts, wereDeleted);
	}

	public static HapiSpecOperation validateTransferListForBalances(List<String> txns, List<String> accounts) {
		return validateTransferListForBalances(txns, accounts, Collections.EMPTY_SET);
	}

	public static HapiSpecOperation validateTransferListForBalances(
			List<String> txns,
			List<String> accounts,
			Set<String> wereDeleted
	) {
		return assertionsHold((spec, assertLog) -> {
			Map<String, Long> actualBalances = accounts
					.stream()
					.collect(Collectors.toMap(
							(String account) ->
									asAccountString(spec.registry().getAccountID(account)),
							(String account) -> {
								if (wereDeleted.contains(account)) {
									return 0L;
								}
								long balance = -1L;
								try {
									BalanceSnapshot preOp = balanceSnapshot("x", account);
									allRunFor(spec, preOp);
									balance = spec.registry().getBalanceSnapshot("x");
								} catch (Throwable ignore) {
								}
								return balance;
							}
					));

			List<AccountAmount> transfers = new ArrayList<>();

			for (String txn : txns) {
				HapiGetTxnRecord subOp = getTxnRecord(txn).logged()
						.payingWith(EXCHANGE_RATE_CONTROL);
				allRunFor(spec, subOp);
				TransactionRecord record = subOp.getResponse().getTransactionGetRecord().getTransactionRecord();
				transfers.addAll(record.getTransferList().getAccountAmountsList());
			}

			Map<String, Long> changes = changesAccordingTo(transfers);
			assertLog.info("Balance changes according to transfer list: " + changes);
			changes.entrySet().forEach(change -> {
				String account = change.getKey();
				long oldBalance = -1L;
				/* The account/contract may have just been created, no snapshot was taken. */
				try {
					oldBalance = spec.registry().getBalanceSnapshot(account + "Snapshot");
				} catch (Throwable ignore) {
				}
				long expectedBalance = change.getValue() + Math.max(0L, oldBalance);
				long actualBalance = actualBalances.getOrDefault(account, -1L);
				assertLog.info("Balance of " + account + " was expected to be " + expectedBalance
						+ ", is actually " + actualBalance + "...");
				Assert.assertEquals(
						"New balance for " + account + " should be " + expectedBalance + " tinyBars.",
						expectedBalance, actualBalance);
			});
		});
	}

	private static Map<String, Long> changesAccordingTo(List<AccountAmount> transfers) {
		return transfers
				.stream()
				.map(aa -> new AbstractMap.SimpleEntry<>(asAccountString(aa.getAccountID()), aa.getAmount()))
				.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));
	}

	public static boolean isNotThrottleProp(Setting setting) {
		var name = setting.getName();
		return !name.startsWith("hapi.throttling");
	}
}
