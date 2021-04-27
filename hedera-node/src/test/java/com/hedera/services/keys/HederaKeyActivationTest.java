package com.hedera.services.keys;

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

import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.legacy.core.jproto.JKeyList;
import com.hedera.test.factories.keys.KeyTree;
import com.hedera.test.factories.sigs.SigWrappers;
import com.swirlds.common.crypto.TransactionSignature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.hedera.services.keys.HederaKeyActivation.ONLY_IF_SIG_IS_VALID;
import static com.hedera.services.keys.HederaKeyActivation.isActive;
import static com.hedera.services.keys.HederaKeyActivation.pkToSigMapFrom;
import static com.hedera.services.sigs.factories.PlatformSigFactory.createEd25519;
import static com.hedera.test.factories.keys.NodeFactory.ed25519;
import static com.hedera.test.factories.keys.NodeFactory.list;
import static com.hedera.test.factories.keys.NodeFactory.threshold;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

public class HederaKeyActivationTest {
	static JKey complexKey;
	byte[] pk = "PK".getBytes();
	byte[] sig = "SIG".getBytes();
	byte[] data = "DATA".getBytes();
	Function<byte[], TransactionSignature> sigsFn;
	BiPredicate<JKey, TransactionSignature> tests;
	final TransactionSignature VALID_SIG = SigWrappers.asValid(List.of(createEd25519(pk, sig, data))).get(0);
	final TransactionSignature INVALID_SIG = SigWrappers.asInvalid(List.of(createEd25519(pk, sig, data))).get(0);

	Function<Integer, TransactionSignature> mockSigFn = i -> createEd25519(
			String.format("PK%d", i).getBytes(),
			String.format("SIG%d", i).getBytes(),
			String.format("DATA%d", i).getBytes());

	@BeforeAll
	private static void setupAll() throws Throwable {
		complexKey = KeyTree.withRoot(
				list(
						ed25519(),
						threshold(1,
								list(list(ed25519(), ed25519()), ed25519()), ed25519()),
						ed25519(),
						list(
								threshold(2,
										ed25519(), ed25519(), ed25519())))).asJKey();
	}

	@BeforeEach
	public void setup() {
		sigsFn = (Function<byte[], TransactionSignature>) mock(Function.class);
		tests = (BiPredicate<JKey, TransactionSignature>) mock(BiPredicate.class);
	}

	@Test
	public void revocationServiceActivatesWithOneTopLevelSig() {
		// setup:
		KeyActivationCharacteristics characteristics =
				RevocationServiceCharacteristics.forTopLevelFile((JKeyList) complexKey);

		given(sigsFn.apply(any()))
				.willReturn(VALID_SIG)
				.willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(VALID_SIG)
				.willReturn(INVALID_SIG)
				.willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(VALID_SIG);

		// when:
		assertTrue(isActive(complexKey, sigsFn, ONLY_IF_SIG_IS_VALID, characteristics));
	}

	@Test
	public void revocationServiceiRequiresOneTopLevelSig() {
		// setup:
		KeyActivationCharacteristics characteristics =
				RevocationServiceCharacteristics.forTopLevelFile((JKeyList) complexKey);

		given(sigsFn.apply(any()))
				.willReturn(INVALID_SIG)
				.willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(INVALID_SIG)
				.willReturn(INVALID_SIG)
				.willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(INVALID_SIG);

		// when:
		assertFalse(isActive(complexKey, sigsFn, ONLY_IF_SIG_IS_VALID, characteristics));
	}

	@Test
	public void mapSupplierReflectsInputList() {
		// setup:
		List<TransactionSignature> presentSigs = List.of(mockSigFn.apply(0), mockSigFn.apply(1));
		TransactionSignature missingSig = mockSigFn.apply(2);

		// given:
		Function<byte[], TransactionSignature> sigsFn = pkToSigMapFrom(presentSigs);

		// when:
		TransactionSignature present0 = sigsFn.apply(presentSigs.get(0).getExpandedPublicKeyDirect());
		TransactionSignature present1 = sigsFn.apply(presentSigs.get(1).getExpandedPublicKeyDirect());
		// and:
		TransactionSignature missing = sigsFn.apply(missingSig.getExpandedPublicKeyDirect());

		// then:
		assertEquals(presentSigs.get(0), present0);
		assertEquals(presentSigs.get(1), present1);
		// and:
		assertEquals(HederaKeyActivation.INVALID_MISSING_SIG, missing);
	}

	@Test
	public void topLevelListActivatesOnlyIfAllChildrenAreActive() {
		given(sigsFn.apply(any())).willReturn(INVALID_SIG).willReturn(VALID_SIG);

		// when:
		assertFalse(isActive(complexKey, sigsFn, ONLY_IF_SIG_IS_VALID));
	}

	@Test
	public void topLevelActivatesIfAllChildrenAreActive() {
		given(sigsFn.apply(any()))
				.willReturn(VALID_SIG)
				.willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(INVALID_SIG).willReturn(VALID_SIG)
				.willReturn(VALID_SIG)
				.willReturn(INVALID_SIG).willReturn(VALID_SIG).willReturn(VALID_SIG);

		// when:
		assertTrue(isActive(complexKey, sigsFn, ONLY_IF_SIG_IS_VALID));
	}
}
