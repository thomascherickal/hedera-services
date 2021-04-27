package com.hedera.services.bdd.spec.utilops.grouping;

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

import com.google.common.base.MoreObjects;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.spec.infrastructure.RegistryNotFound;
import com.hedera.services.bdd.spec.utilops.UtilOp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class ParallelSpecOps extends UtilOp {
	private static final Logger log = LogManager.getLogger(HapiSpecOperation.class);

	private final HapiSpecOperation[] subs;

	public ParallelSpecOps(HapiSpecOperation... subs) {
		this.subs = subs;
	}

	@Override
	protected boolean submitOp(HapiApiSpec spec) throws Throwable {
		Map<String, Throwable> subErrors = new HashMap<>();

		CompletableFuture future = CompletableFuture.allOf(
				Stream.of(subs)
						.map(op -> CompletableFuture.runAsync(
								() -> op.execFor(spec).map(t -> subErrors.put(op.toString(), t))))
						.toArray(n -> new CompletableFuture[n])
		);
		future.join();

		if (subErrors.size() > 0) {
			String errMessages = subErrors.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof RegistryNotFound))
					.peek(e -> e.getValue().printStackTrace())
					.map(e -> e.getKey() + " :: " + e.getValue().getMessage())
					.collect(joining(", "));
			if (errMessages.length() > 0) {
				log.error("Problem(s) with sub-operation(s): {}", errMessages);
			}
		}

		return false;
	}

	@Override
	public boolean requiresFinalization(HapiApiSpec spec) {
		return Stream.of(subs).anyMatch(operation -> operation.requiresFinalization(spec));
	}

	@Override
	public void finalizeExecFor(HapiApiSpec spec) throws Throwable {
		for (HapiSpecOperation op : subs) {
			if (op.requiresFinalization(spec)) {
				op.finalizeExecFor(spec);
			}
		}
	}

	@Override
	protected MoreObjects.ToStringHelper toStringHelper() {
		return super.toStringHelper().add("numSubOps", subs.length);
	}
}
