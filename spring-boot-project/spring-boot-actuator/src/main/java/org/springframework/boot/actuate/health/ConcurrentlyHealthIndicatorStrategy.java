/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * {@link HealthIndicatorStrategy} that returns health indications from all
 * {@link HealthIndicator} instances concurrently.
 *
 * @author Dmytro Nosan
 * @since 2.2.0
 */
public class ConcurrentlyHealthIndicatorStrategy implements HealthIndicatorStrategy {

	private static final Health UNKNOWN = Health.unknown().build();

	private final Executor executor;

	private final Long timeout;

	private final Health timeoutHealth;

	/**
	 * Returns a new {@link ConcurrentlyHealthIndicatorStrategy} with no timeout.
	 * @param executor the executor to submit {@link HealthIndicator HealthIndicators} on.
	 */
	public ConcurrentlyHealthIndicatorStrategy(Executor executor) {
		this.executor = executor;
		this.timeout = null;
		this.timeoutHealth = null;
	}

	/**
	 * Returns a new {@link ConcurrentlyHealthIndicatorStrategy} with timeout.
	 * @param executor the executor to submit {@link HealthIndicator HealthIndicators} on.
	 * @param timeout number of milliseconds to wait before using the
	 * {@code timeoutHealth}
	 * @param timeoutHealth the {@link Health} to use if an health indicator reached the
	 * {@code timeout}. Defaults to {@code unknown} status.
	 */
	public ConcurrentlyHealthIndicatorStrategy(Executor executor, long timeout, Health timeoutHealth) {
		this.executor = executor;
		this.timeout = timeout;
		this.timeoutHealth = (timeoutHealth != null) ? timeoutHealth : UNKNOWN;
	}

	@Override
	public Map<String, Health> doHealth(Map<String, HealthIndicator> healthIndicators) {
		Map<String, Future<Health>> healthsFutures = new LinkedHashMap<>();
		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
			healthsFutures.put(entry.getKey(), CompletableFuture.supplyAsync(entry.getValue()::health, this.executor));
		}
		Map<String, Health> healths = new LinkedHashMap<>();
		for (Map.Entry<String, Future<Health>> entry : healthsFutures.entrySet()) {
			healths.put(entry.getKey(), getHealth(entry.getValue(), this.timeout, this.timeoutHealth));
		}
		return healths;
	}

	private static Health getHealth(Future<Health> healthFuture, Long timeout, Health timeoutHealth) {
		try {
			return (timeout != null) ? healthFuture.get(timeout, TimeUnit.MILLISECONDS) : healthFuture.get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return Health.unknown().withException(ex).build();
		}
		catch (TimeoutException ex) {
			return timeoutHealth;
		}
		catch (ExecutionException ex) {
			Throwable cause = ex.getCause();
			return Health.down((cause instanceof Exception) ? ((Exception) cause) : ex).build();
		}
	}

}
