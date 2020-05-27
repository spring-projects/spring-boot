/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.source;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SoftReferenceConfigurationPropertyCache}.
 *
 * @author Phillip Webb
 */
class SoftReferenceConfigurationPropertyCacheTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2020-01-02T09:00:00Z"), ZoneOffset.UTC);

	private Clock clock = FIXED_CLOCK;

	private AtomicInteger createCount = new AtomicInteger();

	private TestSoftReferenceConfigurationPropertyCache cache = new TestSoftReferenceConfigurationPropertyCache(false);

	@Test
	void getReturnsValueWithCorrectCounts() {
		get(this.cache).assertCounts(0, 0);
		get(this.cache).assertCounts(0, 1);
		get(this.cache).assertCounts(0, 2);
	}

	@Test
	void getWhenNeverExpireReturnsValueWithCorrectCounts() {
		this.cache = new TestSoftReferenceConfigurationPropertyCache(true);
		get(this.cache).assertCounts(0, 0);
		get(this.cache).assertCounts(0, 0);
		get(this.cache).assertCounts(0, 0);
	}

	@Test
	void enableEnablesCachingWithUnlimtedTimeToLive() {
		this.cache.enable();
		get(this.cache).assertCounts(0, 0);
		tick(Duration.ofDays(300));
		get(this.cache).assertCounts(0, 0);
	}

	@Test
	void setTimeToLiveEnablesCachingWithTimeToLive() {
		this.cache.setTimeToLive(Duration.ofDays(1));
		get(this.cache).assertCounts(0, 0);
		tick(Duration.ofHours(2));
		get(this.cache).assertCounts(0, 0);
		tick(Duration.ofDays(2));
		get(this.cache).assertCounts(0, 1);
	}

	@Test
	void setTimeToLiveWhenZeroDisablesCaching() {
		this.cache.setTimeToLive(Duration.ZERO);
		get(this.cache).assertCounts(0, 0);
		get(this.cache).assertCounts(0, 1);
		get(this.cache).assertCounts(0, 2);
	}

	@Test
	void setTimeToLiveWhenNullDisablesCaching() {
		this.cache.setTimeToLive(null);
		get(this.cache).assertCounts(0, 0);
		get(this.cache).assertCounts(0, 1);
		get(this.cache).assertCounts(0, 2);
	}

	@Test
	void clearExpiresCache() {
		this.cache.enable();
		get(this.cache).assertCounts(0, 0);
		get(this.cache).assertCounts(0, 0);
		this.cache.clear();
		get(this.cache).assertCounts(0, 1);

	}

	private Value get(SoftReferenceConfigurationPropertyCache<Value> cache) {
		return cache.get(this::createValue, this::updateValue);
	}

	private Value createValue() {
		return new Value(this.createCount.getAndIncrement(), -1);
	}

	private Value updateValue(Value value) {
		return new Value(value.createCount, value.refreshCount + 1);
	}

	private void tick(Duration duration) {
		this.clock = Clock.offset(this.clock, duration);
	}

	/**
	 * Testable {@link SoftReferenceConfigurationPropertyCache} that actually uses real
	 * references.
	 */
	class TestSoftReferenceConfigurationPropertyCache extends SoftReferenceConfigurationPropertyCache<Value> {

		private Value value;

		TestSoftReferenceConfigurationPropertyCache(boolean neverExpire) {
			super(neverExpire);
		}

		@Override
		protected Value getValue() {
			return this.value;
		}

		@Override
		protected void setValue(Value value) {
			this.value = value;
		}

		@Override
		protected Instant now() {
			return SoftReferenceConfigurationPropertyCacheTests.this.clock.instant();
		}

	}

	/**
	 * Value used for testing.
	 */
	static class Value {

		private final int createCount;

		private int refreshCount;

		Value(int createCount, int refreshCount) {
			this.createCount = createCount;
			this.refreshCount = refreshCount;
		}

		void assertCounts(int expectedCreateCount, int expectedRefreshCount) {
			assertThat(this.createCount).as("created").isEqualTo(expectedCreateCount);
			assertThat(this.refreshCount).as("refreshed").isEqualTo(expectedRefreshCount);
		}

	}

}
