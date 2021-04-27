package com.hedera.services.throttles;

/*-
 * ‌
 * Hedera Services API Utilities
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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static com.hedera.services.throttles.BucketThrottle.CAPACITY_UNITS_PER_NANO_TXN;
import static com.hedera.services.throttles.BucketThrottle.CAPACITY_UNITS_PER_TXN;
import static com.hedera.services.throttles.BucketThrottle.MTPS_PER_TPS;
import static com.hedera.services.throttles.BucketThrottle.NTPS_PER_MTPS;
import static com.hedera.services.throttles.BucketThrottleTest.NTPS_PER_TPS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicThrottleTest {
	@Test
	void factoriesWork() {
		// setup:
		int tps = 1_000;
		long expectedCapacity = tps * NTPS_PER_TPS * CAPACITY_UNITS_PER_NANO_TXN;
		String name = "t6e";

		// given:
		var fromTps = DeterministicThrottle.withTps(tps);
		var fromMtps = DeterministicThrottle.withMtps(tps * MTPS_PER_TPS);
		var fromTpsAndBurstPeriod = DeterministicThrottle.withTpsAndBurstPeriod(tps / 2, 2);
		var fromMtpsAndBurstPeriod = DeterministicThrottle.withMtpsAndBurstPeriod(tps / 2 * MTPS_PER_TPS, 2);
		var fromTpsNamed = DeterministicThrottle.withTpsNamed(tps, name);
		var fromMtpsNamed = DeterministicThrottle.withMtpsNamed(tps * MTPS_PER_TPS, name);
		var fromTpsAndBurstPeriodNamed = DeterministicThrottle.withTpsAndBurstPeriodNamed(tps / 2, 2, name);
		var fromMtpsAndBurstPeriodNamed = DeterministicThrottle.withMtpsAndBurstPeriodNamed(tps / 2 * MTPS_PER_TPS, 2, name);
		var fromTpsAndBurstPeriodMs = DeterministicThrottle.withTpsAndBurstPeriodMs(tps / 2, 2_000);
		var fromMtpsAndBurstPeriodMs = DeterministicThrottle.withMtpsAndBurstPeriodMs(tps / 2 * MTPS_PER_TPS, 2_000);
		var fromTpsAndBurstPeriodMsNamed = DeterministicThrottle.withTpsAndBurstPeriodMsNamed(tps / 2, 2_000, name);
		var fromMtpsAndBurstPeriodMsNamed = DeterministicThrottle.withMtpsAndBurstPeriodMsNamed(tps / 2 * MTPS_PER_TPS, 2_000, name);

		// expect:
		assertEquals(expectedCapacity, fromTps.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromMtps.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromTpsAndBurstPeriod.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromTpsAndBurstPeriodMs.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromMtpsAndBurstPeriod.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromMtpsAndBurstPeriodMs.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromTpsNamed.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromMtpsNamed.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromTpsAndBurstPeriodNamed.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromTpsAndBurstPeriodMsNamed.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromMtpsAndBurstPeriodNamed.delegate().bucket().totalCapacity());
		assertEquals(expectedCapacity, fromMtpsAndBurstPeriodMsNamed.delegate().bucket().totalCapacity());
		// and:
		assertEquals(tps * MTPS_PER_TPS, fromTps.mtps());
		assertEquals(tps * MTPS_PER_TPS, fromMtps.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromTpsAndBurstPeriod.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromTpsAndBurstPeriodMs.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromMtpsAndBurstPeriod.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromMtpsAndBurstPeriodMs.mtps());
		assertEquals(tps * MTPS_PER_TPS, fromTpsNamed.mtps());
		assertEquals(tps * MTPS_PER_TPS, fromMtpsNamed.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromTpsAndBurstPeriodNamed.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromTpsAndBurstPeriodMsNamed.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromMtpsAndBurstPeriodNamed.mtps());
		assertEquals(tps * MTPS_PER_TPS / 2, fromMtpsAndBurstPeriodMsNamed.mtps());
		// and:
		assertNull(fromTps.lastDecisionTime());
		assertNull(fromMtps.lastDecisionTime());
		assertNull(fromTpsAndBurstPeriod.lastDecisionTime());
		assertNull(fromTpsAndBurstPeriodMs.lastDecisionTime());
		assertNull(fromMtpsAndBurstPeriod.lastDecisionTime());
		assertNull(fromMtpsAndBurstPeriodMs.lastDecisionTime());
		assertNull(fromTpsNamed.lastDecisionTime());
		assertNull(fromMtpsNamed.lastDecisionTime());
		assertNull(fromTpsAndBurstPeriodNamed.lastDecisionTime());
		assertNull(fromTpsAndBurstPeriodMsNamed.lastDecisionTime());
		assertNull(fromMtpsAndBurstPeriodNamed.lastDecisionTime());
		assertNull(fromMtpsAndBurstPeriodMsNamed.lastDecisionTime());
		// and:
		assertEquals(name, fromTpsNamed.name());
		assertEquals(name, fromMtpsNamed.name());
		assertEquals(name, fromTpsAndBurstPeriodNamed.name());
		assertEquals(name, fromMtpsAndBurstPeriodNamed.name());
		assertEquals(name, fromTpsAndBurstPeriodMsNamed.name());
		assertEquals(name, fromMtpsAndBurstPeriodMsNamed.name());
	}

	@Test
	void throttlesWithinPermissibleTolerance() throws InterruptedException, IOException {
		// setup:
		long mtps = 123_456L;

		// given:
		var subject = DeterministicThrottle.withMtps(mtps);

		// given:
		double expectedTps = (1.0 * mtps) / 1_000;
		// and:
		subject.resetUsageTo(new DeterministicThrottle.UsageSnapshot(
				subject.capacity() - DeterministicThrottle.capacityRequiredFor(1),
				null));

		// when:
		var helper = new ConcurrentThrottleTestHelper(10, 10, 2);
		// and:
		helper.runWith(subject);

		// then:
		helper.assertTolerableTps(expectedTps, 1.00);
	}

	@Test
	void usesZeroElapsedNanosOnFirstDecision() {
		// setup:
		int tps = 1;
		int burstPeriod = 5;
		long internalCapacity = tps * NTPS_PER_TPS * CAPACITY_UNITS_PER_NANO_TXN * burstPeriod;
		Instant now = Instant.ofEpochSecond(1_234_567L);

		// given:
		var subject = DeterministicThrottle.withTpsAndBurstPeriod(tps, burstPeriod);

		// when:
		var result = subject.allow(1, now);

		// then:
		assertTrue(result);
		assertSame(now, subject.lastDecisionTime());
		assertEquals(internalCapacity - CAPACITY_UNITS_PER_TXN, subject.delegate().bucket().capacityFree());
	}


	@Test
	void requiresMonotonicIncreasingTimeline() {
		// setup:
		int tps = 1;
		int burstPeriod = 5;
		Instant now = Instant.ofEpochSecond(1_234_567L);

		// given:
		var subject = DeterministicThrottle.withTpsAndBurstPeriod(tps, burstPeriod);

		// when:
		subject.allow(1, now);

		// then:
		assertThrows(IllegalArgumentException.class, () -> subject.allow(1, now.minusNanos(1)));
		assertDoesNotThrow(() -> subject.allow(1, now));
	}

	@Test
	void usesCorrectElapsedNanosOnSubsequentDecision() {
		// setup:
		int tps = 1;
		int burstPeriod = 5;
		long internalCapacity = tps * NTPS_PER_TPS * CAPACITY_UNITS_PER_NANO_TXN * burstPeriod;
		long elapsedNanos = 1_234;
		Instant originalDecision = Instant.ofEpochSecond(1_234_567L, 0);
		Instant now = Instant.ofEpochSecond(1_234_567L, elapsedNanos);

		// given:
		var subject = DeterministicThrottle.withTpsAndBurstPeriod(tps, burstPeriod);

		// when:
		subject.allow(1, originalDecision);
		// and:
		var result = subject.allow(1, now);

		// then:
		assertTrue(result);
		assertSame(now, subject.lastDecisionTime());
		assertEquals(
				internalCapacity - 2 * CAPACITY_UNITS_PER_TXN + 1_000 * elapsedNanos,
				subject.delegate().bucket().capacityFree());
	}

	@Test
	void returnsExpectedState() {
		// setup:
		int mtps = 333;
		int burstPeriod = 6;
		Instant originalDecision = Instant.ofEpochSecond(1_234_567L, 0);

		// given:
		var subject = DeterministicThrottle.withMtpsAndBurstPeriod(mtps, burstPeriod);

		// when:
		subject.allow(1, originalDecision);
		// and:
		var state = subject.usageSnapshot();

		// then:
		assertEquals(CAPACITY_UNITS_PER_TXN, state.used());
		assertEquals(originalDecision, state.lastDecisionTime());
	}

	@Test
	void resetsAsExpected() {
		// setup:
		int mtps = 333;
		int burstPeriod = 6;
		long internalCapacity = mtps * NTPS_PER_MTPS * CAPACITY_UNITS_PER_NANO_TXN * burstPeriod;
		long used = internalCapacity / 2;
		Instant originalDecision = Instant.ofEpochSecond(1_234_567L, 0);

		// given:
		var subject = DeterministicThrottle.withMtpsAndBurstPeriod(mtps, burstPeriod);
		// and:
		var snapshot = new DeterministicThrottle.UsageSnapshot(used, originalDecision);

		// when:
		subject.resetUsageTo(snapshot);

		// then:
		assertEquals(used, subject.delegate().bucket().capacityUsed());
		assertEquals(originalDecision, subject.lastDecisionTime());
	}

	@Test
	void reclaimsAsExpected() {
		// setup:
		int mtps = 333;
		int burstPeriod = 6;

		// given:
		var subject = DeterministicThrottle.withMtpsAndBurstPeriod(mtps, burstPeriod);
		// and:
		subject.allow(2);

		// when:
		subject.reclaimLastAllowedUse();

		// then:
		assertEquals(0, subject.delegate().bucket().capacityUsed());
	}

	@Test
	void toStringWorks() {
		// setup:
		int mtps = 333;
		int burstPeriod = 6;
		long internalCapacity = mtps * NTPS_PER_MTPS * CAPACITY_UNITS_PER_NANO_TXN * burstPeriod;
		Instant originalDecision = Instant.ofEpochSecond(1_234_567L, 0);
		String name = "Testing123";

		String expectedForAnonymous = String.format(
				"DeterministicThrottle{mtps=%d, capacity=%d (used=%d), last decision @ %s}",
				mtps, internalCapacity, DeterministicThrottle.capacityRequiredFor(1), originalDecision);
		String expectedForDisclosed = String.format(
				"DeterministicThrottle{name='%s', mtps=%d, capacity=%d (used=%d), last decision @ %s}", name,
				mtps, internalCapacity, DeterministicThrottle.capacityRequiredFor(1), originalDecision);

		// given:
		var anonymousSubject = DeterministicThrottle.withMtpsAndBurstPeriod(mtps, burstPeriod);
		var disclosedSubject = DeterministicThrottle.withMtpsAndBurstPeriodNamed(mtps, burstPeriod, name);
		// and:
		anonymousSubject.allow(1, originalDecision);
		disclosedSubject.allow(1, originalDecision);

		// then:
		assertEquals(expectedForAnonymous, anonymousSubject.toString());
		assertEquals(expectedForDisclosed, disclosedSubject.toString());
	}

	@Test
	void snapshotObjectContractMet() {
		long aUsed = 123, bUsed = 456;
		Instant aLast = Instant.ofEpochSecond(1_234_567L, 890);
		Instant bLast = Instant.ofEpochSecond(7_654_321L, 890);

		// given:
		var a = new DeterministicThrottle.UsageSnapshot(aUsed, aLast);

		// expect:
		assertEquals(a, a);
		assertEquals(a, new DeterministicThrottle.UsageSnapshot(aUsed, aLast));
		assertNotEquals(a, null);
		assertNotEquals(a, new Object());
		assertNotEquals(a, new DeterministicThrottle.UsageSnapshot(bUsed, aLast));
		assertNotEquals(a, new DeterministicThrottle.UsageSnapshot(aUsed, bLast));
		// and:
		assertEquals(a.hashCode(), a.hashCode());
		assertEquals(a.hashCode(), new DeterministicThrottle.UsageSnapshot(aUsed, aLast).hashCode());
		assertNotEquals(a, new Object().hashCode());
		assertNotEquals(a, new DeterministicThrottle.UsageSnapshot(bUsed, aLast).hashCode());
		assertNotEquals(a, new DeterministicThrottle.UsageSnapshot(aUsed, bLast).hashCode());
	}

	@Test
	void snapshotToStringWorks() {
		// setup:
		long aUsed = 123;
		Instant aLast = Instant.ofEpochSecond(1_234_567L, 890);
		var aNoLast = new DeterministicThrottle.UsageSnapshot(aUsed, null);
		var aWithLast = new DeterministicThrottle.UsageSnapshot(aUsed, aLast);

		// given:
		var desiredWithLastDecision = "DeterministicThrottle.UsageSnapshot{used=123, " +
				"last decision @ 1970-01-15T06:56:07.000000890Z}";
		var desiredWithNoLastDecision = "DeterministicThrottle.UsageSnapshot{used=123, " +
				"last decision @ <N/A>}";

		// expect:
		assertEquals(desiredWithNoLastDecision, aNoLast.toString());
		assertEquals(desiredWithLastDecision, aWithLast.toString());
	}
}
