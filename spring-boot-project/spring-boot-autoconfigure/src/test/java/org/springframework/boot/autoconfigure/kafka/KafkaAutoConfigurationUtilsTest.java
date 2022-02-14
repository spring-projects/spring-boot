/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import org.junit.jupiter.api.Test;

import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.SleepingBackOffPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaAutoConfigurationUtils}.
 *
 * @author Tomaz Fernandes
 */
class KafkaAutoConfigurationUtilsTest {

	@Test
	void shouldCreateExponentialBackOffPolicy() {
		SleepingBackOffPolicy<?> backOff = KafkaAutoConfigurationUtils.createBackOffFrom(100L, 1000L, 2.0, false);
		assertThat(backOff).isInstanceOf(ExponentialBackOffPolicy.class);
		ExponentialBackOffPolicy exponentialBackOff = (ExponentialBackOffPolicy) backOff;
		assertThat(exponentialBackOff.getInitialInterval()).isEqualTo(100);
		assertThat(exponentialBackOff.getMaxInterval()).isEqualTo(1000);
		assertThat(exponentialBackOff.getMultiplier()).isEqualTo(2.0);
	}

	@Test
	void shouldCreateExponentialRandomBackOffPolicy() {
		SleepingBackOffPolicy<?> backOff = KafkaAutoConfigurationUtils.createBackOffFrom(100L, 1000L, 2.0, true);
		assertThat(backOff).isInstanceOf(ExponentialRandomBackOffPolicy.class);
		ExponentialRandomBackOffPolicy exponentialBackOff = (ExponentialRandomBackOffPolicy) backOff;
		assertThat(exponentialBackOff.getInitialInterval()).isEqualTo(100);
		assertThat(exponentialBackOff.getMaxInterval()).isEqualTo(1000);
		assertThat(exponentialBackOff.getMultiplier()).isEqualTo(2.0);
	}

	@Test
	void shouldCreateUniformRandomBackOffPolicy() {
		SleepingBackOffPolicy<?> backOff = KafkaAutoConfigurationUtils.createBackOffFrom(100L, 1000L, null, null);
		assertThat(backOff).isInstanceOf(UniformRandomBackOffPolicy.class);
		UniformRandomBackOffPolicy exponentialBackOff = (UniformRandomBackOffPolicy) backOff;
		assertThat(exponentialBackOff.getMinBackOffPeriod()).isEqualTo(100);
		assertThat(exponentialBackOff.getMaxBackOffPeriod()).isEqualTo(1000);
	}

	@Test
	void shouldCreateFixedBackOffPolicy() {
		SleepingBackOffPolicy<?> backOff = KafkaAutoConfigurationUtils.createBackOffFrom(100L, null, null, null);
		assertThat(backOff).isInstanceOf(FixedBackOffPolicy.class);
		FixedBackOffPolicy exponentialBackOff = (FixedBackOffPolicy) backOff;
		assertThat(exponentialBackOff.getBackOffPeriod()).isEqualTo(100);
	}

}
