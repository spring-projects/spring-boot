/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RetryPolicySettings}.
 *
 * @author Stephane Nicoll
 */
class RetryPolicySettingsTests {

	@Test
	void exceptionIncludesCanBeReplaced() {
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.getExceptionIncludes().add(IllegalStateException.class);
		settings.setExceptionIncludes(List.of(IllegalArgumentException.class));
		assertThat(settings.getExceptionIncludes()).containsExactly(IllegalArgumentException.class);
	}

	@Test
	void exceptionIncludesListIsCopied() {
		RetryPolicySettings settings = new RetryPolicySettings();
		List<Class<? extends Throwable>> includes = new ArrayList<>();
		includes.add(IllegalStateException.class);
		settings.setExceptionIncludes(includes);
		includes.add(IllegalArgumentException.class);
		assertThat(settings.getExceptionIncludes()).containsExactly(IllegalStateException.class);
	}

	@Test
	void createRetryPolicyWithExceptionIncludes() {
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.getExceptionIncludes().add(IllegalStateException.class);
		RetryPolicy retryPolicy = settings.createRetryPolicy();
		assertThat(retryPolicy.shouldRetry(new IllegalStateException("test"))).isTrue();
		assertThat(retryPolicy.shouldRetry(new IllegalArgumentException("test"))).isFalse();
	}

	@Test
	void exceptionExcludesCanBeReplaced() {
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.getExceptionExcludes().add(IllegalStateException.class);
		settings.setExceptionExcludes(List.of(IllegalArgumentException.class));
		assertThat(settings.getExceptionExcludes()).containsExactly(IllegalArgumentException.class);
	}

	@Test
	void exceptionExcludesListIsCopied() {
		RetryPolicySettings settings = new RetryPolicySettings();
		List<Class<? extends Throwable>> excludes = new ArrayList<>();
		excludes.add(IllegalStateException.class);
		settings.setExceptionExcludes(excludes);
		excludes.add(IllegalArgumentException.class);
		assertThat(settings.getExceptionExcludes()).containsExactly(IllegalStateException.class);
	}

	@Test
	void createRetryPolicyWithExceptionExcludes() {
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.getExceptionExcludes().add(IllegalStateException.class);
		RetryPolicy retryPolicy = settings.createRetryPolicy();
		assertThat(retryPolicy.shouldRetry(new IllegalStateException("test"))).isFalse();
		assertThat(retryPolicy.shouldRetry(new IllegalArgumentException("test"))).isTrue();
	}

	@Test
	void getDefaultExceptionPredicate() {
		assertThat(new RetryPolicySettings().getExceptionPredicate()).isNull();
	}

	@Test
	void createRetryPolicyWithExceptionPredicate() {
		IllegalArgumentException exception = new IllegalArgumentException("test");
		Predicate<Throwable> exceptionPredicate = mock();
		given(exceptionPredicate.test(exception)).willReturn(true);
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.setExceptionPredicate(exceptionPredicate);
		RetryPolicy retryPolicy = settings.createRetryPolicy();
		assertThat(retryPolicy.shouldRetry(exception)).isTrue();
		then(exceptionPredicate).should().test(exception);
		then(exceptionPredicate).shouldHaveNoMoreInteractions();
	}

	@Test
	void createRetryPolicyWithDefaultsMatchesBackOffDefaults() {
		RetryPolicy defaultRetryPolicy = RetryPolicy.builder().build();
		RetryPolicy retryPolicy = new RetryPolicySettings().createRetryPolicy();
		assertThat(retryPolicy.getBackOff()).isInstanceOf(ExponentialBackOff.class);
		ExponentialBackOff defaultBackOff = (ExponentialBackOff) defaultRetryPolicy.getBackOff();
		ExponentialBackOff backOff = (ExponentialBackOff) retryPolicy.getBackOff();
		assertThat(backOff.getMaxAttempts()).isEqualTo(defaultBackOff.getMaxAttempts());
		assertThat(backOff.getInitialInterval()).isEqualTo(defaultBackOff.getInitialInterval());
		assertThat(backOff.getJitter()).isEqualTo(defaultBackOff.getJitter());
		assertThat(backOff.getMultiplier()).isEqualTo(defaultBackOff.getMultiplier());
		assertThat(backOff.getMaxInterval()).isEqualTo(defaultBackOff.getMaxInterval());
	}

	@Test
	void createRetryPolicyWithCustomAttributes() {
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.setMaxAttempts(10L);
		settings.setDelay(Duration.ofSeconds(2));
		settings.setJitter(Duration.ofMillis(500));
		settings.setMultiplier(2.0);
		settings.setMaxDelay(Duration.ofSeconds(20));
		RetryPolicy retryPolicy = settings.createRetryPolicy();
		assertThat(retryPolicy.getBackOff()).isInstanceOfSatisfying(ExponentialBackOff.class, (backOff) -> {
			assertThat(backOff.getMaxAttempts()).isEqualTo(10);
			assertThat(backOff.getInitialInterval()).isEqualTo(2000);
			assertThat(backOff.getJitter()).isEqualTo(500);
			assertThat(backOff.getMultiplier()).isEqualTo(2.0);
			assertThat(backOff.getMaxInterval()).isEqualTo(20_000);
		});
	}

	@Test
	void createRetryPolicyWithFactoryCanOverrideAttribute() {
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.setDelay(Duration.ofSeconds(2));
		settings.setMultiplier(2.0);
		settings.setFactory((builder) -> builder.multiplier(3.0).build());
		RetryPolicy retryPolicy = settings.createRetryPolicy();
		assertThat(retryPolicy.getBackOff()).isInstanceOfSatisfying(ExponentialBackOff.class, (backOff) -> {
			assertThat(backOff.getInitialInterval()).isEqualTo(2000L);
			assertThat(backOff.getMultiplier()).isEqualTo(3.0);
		});
	}

	@Test
	void createRetryPolicyWithFactoryCanIgnoreBuilder() {
		BackOff backOff = mock(BackOff.class);
		RetryPolicySettings settings = new RetryPolicySettings();
		settings.setFactory((builder) -> RetryPolicy.builder().backOff(backOff).build());
		RetryPolicy retryPolicy = settings.createRetryPolicy();
		assertThat(retryPolicy.getBackOff()).isEqualTo(backOff);
	}

}
