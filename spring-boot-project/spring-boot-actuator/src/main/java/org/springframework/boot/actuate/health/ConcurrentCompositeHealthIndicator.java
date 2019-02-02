/*
 * Copyright 2012-2018 the original author or authors.
 *
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
 */

package org.springframework.boot.actuate.health;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Version of {@link CompositeHealthIndicator} that gathers health indications from all
 * registered delegates concurrently.
 *
 * @author Rik vd Ende
 */
public class ConcurrentCompositeHealthIndicator extends CompositeHealthIndicator {

	private final ThreadPoolTaskExecutor executor;

	private final Function<Future<Health>, Health> futureFunction;

	/**
	 * Create a new {@link ConcurrentCompositeHealthIndicator} from the specified
	 * indicators.
	 * @param healthAggregator the health aggregator
	 * @param indicators a map of {@link HealthIndicator HealthIndicators} with the key
	 * being used as an indicator name.
	 * @param executor the {@link ThreadPoolTaskExecutor} to submit HealthIndicators on
	 */
	public ConcurrentCompositeHealthIndicator(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> indicators, ThreadPoolTaskExecutor executor) {
		this(healthAggregator, new DefaultHealthIndicatorRegistry(indicators), executor);
	}

	/**
	 * Create a new {@link ConcurrentCompositeHealthIndicator} from the specified
	 * indicators.
	 * @param healthAggregator the health aggregator
	 * @param indicators a map of {@link HealthIndicator HealthIndicators} with the key
	 * being used as an indicator name.
	 * @param executor the {@link ThreadPoolTaskExecutor} to submit HealthIndicators on
	 * @param timeout the maximum time to wait for a HealthIndicator to complete
	 */
	public ConcurrentCompositeHealthIndicator(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> indicators, ThreadPoolTaskExecutor executor,
			Duration timeout) {
		this(healthAggregator, new DefaultHealthIndicatorRegistry(indicators), executor,
				timeout);
	}

	/**
	 * Create a new
	 * {@link org.springframework.boot.actuate.health.ConcurrentCompositeHealthIndicator}
	 * from the indicators in the given {@code registry} and provide a
	 * ThreadPoolTaskExecutor to submit the HealthIndicators to, without a timeout.
	 * @param healthAggregator the health aggregator
	 * @param registry the registry of {@link HealthIndicator HealthIndicators}.
	 * @param executor the {@link ThreadPoolTaskExecutor} to submit HealthIndicators on
	 */
	public ConcurrentCompositeHealthIndicator(HealthAggregator healthAggregator,
			HealthIndicatorRegistry registry, ThreadPoolTaskExecutor executor) {
		this(healthAggregator, registry, executor, (future) -> {
			try {
				return future.get();
			}
			catch (InterruptedException | ExecutionException ex) {
				return Health.down()
						.withDetail("error",
								ex.getClass().getName()
										+ ": Health check did not compete successfully")
						.build();
			}
		});
	}

	/**
	 * Create a new
	 * {@link org.springframework.boot.actuate.health.ConcurrentCompositeHealthIndicator}
	 * from the indicators in the given {@code registry} and provide a
	 * ThreadPoolTaskExecutor for submitting the health checks.
	 * @param healthAggregator the health aggregator
	 * @param registry the registry of {@link HealthIndicator HealthIndicators}.
	 * @param executor the {@link ThreadPoolTaskExecutor} to submit HealthIndicators on
	 * @param timeout the maximum time to wait for a HealthIndicator to complete
	 */
	public ConcurrentCompositeHealthIndicator(HealthAggregator healthAggregator,
			HealthIndicatorRegistry registry, ThreadPoolTaskExecutor executor,
			Duration timeout) {
		this(healthAggregator, registry, executor, (future) -> {
			try {
				return future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
			}
			catch (InterruptedException | ExecutionException ex) {
				return Health.down()
						.withDetail("error",
								ex.getClass().getName()
										+ ": health check did not compete successfully")
						.build();
			}
			catch (TimeoutException ex) {
				return Health.down()
						.withDetail("error",
								ex.getClass().getName()
										+ ": health check timed out after " + timeout)
						.build();
			}
		});
	}

	/**
	 * Create a new
	 * {@link org.springframework.boot.actuate.health.ConcurrentCompositeHealthIndicator}
	 * from the indicators in the given {@code registry} and provide a
	 * ThreadPoolTaskExecutor for submitting the health checks.
	 * @param healthAggregator the health aggregator
	 * @param registry the registry of {@link HealthIndicator HealthIndicators}.
	 * @param executor the {@link ThreadPoolTaskExecutor} to submit HealthIndicators on
	 * @param futureFunction function to select Future::get with or without a timeout
	 */
	private ConcurrentCompositeHealthIndicator(HealthAggregator healthAggregator,
			HealthIndicatorRegistry registry, ThreadPoolTaskExecutor executor,
			Function<Future<Health>, Health> futureFunction) {
		super(healthAggregator, registry);
		this.executor = executor;
		this.futureFunction = futureFunction;
	}

	@Override
	public Health health() {
		Map<String, Future<Health>> futureHealths = getRegistry().getAll().entrySet()
				.stream()
				.map((entry) -> new SimpleEntry<>(entry.getKey(),
						this.executor.submit(() -> entry.getValue().health())))
				.collect(LinkedHashMap::new,
						(map, entry) -> map.put(entry.getKey(), entry.getValue()),
						Map::putAll);

		Map<String, Health> healths = futureHealths.entrySet().stream()
				.map((entry) -> new SimpleEntry<>(entry.getKey(),
						this.futureFunction.apply(entry.getValue())))
				.collect(LinkedHashMap::new,
						(map, entry) -> map.put(entry.getKey(), entry.getValue()),
						Map::putAll);

		return getAggregator().aggregate(healths);
	}

}
