package com.hedera.services.bdd.spec.infrastructure.providers.ops.token;

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

import com.hedera.services.bdd.spec.HapiSpecOperation;
import com.hedera.services.bdd.spec.infrastructure.EntityNameProvider;
import com.hedera.services.bdd.spec.infrastructure.OpProvider;
import com.hedera.services.bdd.spec.infrastructure.providers.names.RegistrySourcedNameProvider;
import com.hedera.services.bdd.spec.transactions.token.HapiTokenCreate;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.Key;
import com.hederahashgraph.api.proto.java.ResponseCodeEnum;
import com.hederahashgraph.api.proto.java.TokenID;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hedera.services.bdd.spec.infrastructure.providers.ops.token.RandomToken.DEFAULT_MAX_SUPPLY;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.mintToken;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_BURN_AMOUNT;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_MINT_AMOUNT;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_SUPPLY_KEY;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_IS_IMMUTABLE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.TOKEN_WAS_DELETED;

public class RandomTokenMint implements OpProvider {
	private final RegistrySourcedNameProvider<TokenID> tokens;

	private final ResponseCodeEnum[] permissibleOutcomes = standardOutcomesAnd(
			TOKEN_WAS_DELETED,
			TOKEN_HAS_NO_SUPPLY_KEY,
			INVALID_TOKEN_MINT_AMOUNT
	);


	public RandomTokenMint(RegistrySourcedNameProvider<TokenID> tokens) {
		this.tokens = tokens;
	}

	@Override
	public Optional<HapiSpecOperation> get() {
		Optional<String> token = tokens.getQualifying();
		if (token.isEmpty()) {
			return Optional.empty();
		}

		var amount = BASE_RANDOM.nextLong(1, DEFAULT_MAX_SUPPLY);

		var op = mintToken(token.get(), amount)
				.hasPrecheckFrom(STANDARD_PERMISSIBLE_PRECHECKS)
				.hasKnownStatusFrom(permissibleOutcomes);

		return Optional.of(op);
	}
}
