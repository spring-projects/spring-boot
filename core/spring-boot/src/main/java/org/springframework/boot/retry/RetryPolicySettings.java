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
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryPolicy.Builder;

/**
 * Settings for a {@link RetryPolicy}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class RetryPolicySettings {

	/**
	 * Default number of retry attempts.
	 */
	public static final long DEFAULT_MAX_ATTEMPTS = RetryPolicy.Builder.DEFAULT_MAX_ATTEMPTS;

	/**
	 * Default initial delay.
	 */
	public static final Duration DEFAULT_DELAY = Duration.ofMillis(RetryPolicy.Builder.DEFAULT_DELAY);

	/**
	 * Default multiplier, uses a fixed delay.
	 */
	public static final double DEFAULT_MULTIPLIER = RetryPolicy.Builder.DEFAULT_MULTIPLIER;

	/**
	 * Default maximum delay (infinite).
	 */
	public static final Duration DEFAULT_MAX_DELAY = Duration.ofMillis(RetryPolicy.Builder.DEFAULT_MAX_DELAY);

	private List<Class<? extends Throwable>> exceptionIncludes = new ArrayList<>();

	private List<Class<? extends Throwable>> exceptionExcludes = new ArrayList<>();

	private @Nullable Predicate<Throwable> exceptionPredicate;

	private Long maxAttempts = DEFAULT_MAX_ATTEMPTS;

	private Duration delay = DEFAULT_DELAY;

	private @Nullable Duration jitter;

	private Double multiplier = DEFAULT_MULTIPLIER;

	private Duration maxDelay = DEFAULT_MAX_DELAY;

	private @Nullable Function<Builder, RetryPolicy> factory;

	/**
	 * Create a {@link RetryPolicy} based on the state of this instance.
	 * @return a {@link RetryPolicy}
	 */
	public RetryPolicy createRetryPolicy() {
		PropertyMapper map = PropertyMapper.get();
		RetryPolicy.Builder builder = RetryPolicy.builder();
		map.from(this::getExceptionIncludes).to(builder::includes);
		map.from(this::getExceptionExcludes).to(builder::excludes);
		map.from(this::getExceptionPredicate).to(builder::predicate);
		map.from(this::getMaxAttempts).to(builder::maxAttempts);
		map.from(this::getDelay).to(builder::delay);
		map.from(this::getJitter).to(builder::jitter);
		map.from(this::getMultiplier).to(builder::multiplier);
		map.from(this::getMaxDelay).to(builder::maxDelay);
		return (this.factory != null) ? this.factory.apply(builder) : builder.build();
	}

	/**
	 * Return the applicable exception types to attempt a retry for.
	 * <p>
	 * The default is empty, leading to a retry attempt for any exception.
	 * @return the applicable exception types
	 */
	public List<Class<? extends Throwable>> getExceptionIncludes() {
		return this.exceptionIncludes;
	}

	/**
	 * Replace the applicable exception types to attempt a retry for by the given
	 * {@code includes}. Alternatively consider using {@link #getExceptionIncludes()} to
	 * mutate the existing list.
	 * @param includes the applicable exception types
	 */
	public void setExceptionIncludes(List<Class<? extends Throwable>> includes) {
		this.exceptionIncludes = new ArrayList<>(includes);
	}

	/**
	 * Return the non-applicable exception types to avoid a retry for.
	 * <p>
	 * The default is empty, leading to a retry attempt for any exception.
	 * @return the non-applicable exception types
	 */
	public List<Class<? extends Throwable>> getExceptionExcludes() {
		return this.exceptionExcludes;
	}

	/**
	 * Replace the non-applicable exception types to attempt a retry for by the given
	 * {@code excludes}. Alternatively consider using {@link #getExceptionExcludes()} to
	 * mutate the existing list.
	 * @param excludes the non-applicable types
	 */
	public void setExceptionExcludes(List<Class<? extends Throwable>> excludes) {
		this.exceptionExcludes = new ArrayList<>(excludes);
	}

	/**
	 * Return the predicate to use to determine whether to retry a failed operation based
	 * on a given {@link Throwable}.
	 * @return the predicate to use
	 */
	public @Nullable Predicate<Throwable> getExceptionPredicate() {
		return this.exceptionPredicate;
	}

	/**
	 * Set the predicate to use to determine whether to retry a failed operation based on
	 * a given {@link Throwable}.
	 * @param exceptionPredicate the predicate to use
	 */
	public void setExceptionPredicate(@Nullable Predicate<Throwable> exceptionPredicate) {
		this.exceptionPredicate = exceptionPredicate;
	}

	/**
	 * Return the maximum number of retry attempts.
	 * @return the maximum number of retry attempts
	 * @see #DEFAULT_MAX_ATTEMPTS
	 */
	public Long getMaxAttempts() {
		return this.maxAttempts;
	}

	/**
	 * Specify the maximum number of retry attempts.
	 * @param maxAttempts the max attempts (must be equal or greater than zero)
	 */
	public void setMaxAttempts(Long maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	/**
	 * Return the base delay after the initial invocation.
	 * @return the base delay
	 * @see #DEFAULT_DELAY
	 */
	public Duration getDelay() {
		return this.delay;
	}

	/**
	 * Specify the base delay after the initial invocation.
	 * <p>
	 * If a {@linkplain #getMultiplier() multiplier} is specified, this serves as the
	 * initial delay to multiply from.
	 * @param delay the base delay (must be greater than or equal to zero)
	 */
	public void setDelay(Duration delay) {
		this.delay = delay;
	}

	/**
	 * Return the jitter period to enable random retry attempts.
	 * @return the jitter value
	 */
	public @Nullable Duration getJitter() {
		return this.jitter;
	}

	/**
	 * Specify a jitter period for the base retry attempt, randomly subtracted or added to
	 * the calculated delay, resulting in a value between {@code delay - jitter} and
	 * {@code delay + jitter} but never below the {@linkplain #getDelay() base delay} or
	 * above the {@linkplain #getMaxDelay() max delay}.
	 * <p>
	 * If a {@linkplain #getMultiplier() multiplier} is specified, it is applied to the
	 * jitter value as well.
	 * @param jitter the jitter value (must be positive)
	 */
	public void setJitter(@Nullable Duration jitter) {
		this.jitter = jitter;
	}

	/**
	 * Return the value to multiply the current interval by for each attempt. The default
	 * value, {@code 1.0}, effectively results in a fixed delay.
	 * @return the value to multiply the current interval by for each attempt
	 * @see #DEFAULT_MULTIPLIER
	 */
	public Double getMultiplier() {
		return this.multiplier;
	}

	/**
	 * Specify a multiplier for a delay for the next retry attempt.
	 * @param multiplier value to multiply the current interval by for each attempt (must
	 * be greater than or equal to 1)
	 */
	public void setMultiplier(Double multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Return the maximum delay for any retry attempt.
	 * @return the maximum delay
	 */
	public Duration getMaxDelay() {
		return this.maxDelay;
	}

	/**
	 * Specify the maximum delay for any retry attempt, limiting how far
	 * {@linkplain #getJitter() jitter} and the {@linkplain #getMultiplier() multiplier}
	 * can increase the {@linkplain #getDelay() delay}.
	 * <p>
	 * The default is unlimited.
	 * @param maxDelay the maximum delay (must be positive)
	 * @see #DEFAULT_MAX_DELAY
	 */
	public void setMaxDelay(Duration maxDelay) {
		this.maxDelay = maxDelay;
	}

	/**
	 * Set the factory to use to create the {@link RetryPolicy}, or {@code null} to use
	 * the default. The function takes a {@link Builder RetryPolicy.Builder} initialized
	 * with the state of this instance that can be further configured, or ignored to
	 * restart from scratch.
	 * @param factory a factory to customize the retry policy.
	 */
	public void setFactory(@Nullable Function<RetryPolicy.Builder, RetryPolicy> factory) {
		this.factory = factory;
	}

}
