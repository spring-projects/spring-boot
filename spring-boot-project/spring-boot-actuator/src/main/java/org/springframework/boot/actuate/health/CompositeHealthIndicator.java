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

import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} that returns health indications from all registered delegates.
 *
 * @author Tyler J. Frederick
 * @author Phillip Webb
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class CompositeHealthIndicator implements HealthIndicator {

	private final HealthIndicatorRegistry registry;

	private final HealthAggregator aggregator;

	private final HealthStrategy strategy;

	/**
	 * Create a new {@link CompositeHealthIndicator} from the specified indicators.
	 * @param healthAggregator the health aggregator
	 * @param indicators a map of {@link HealthIndicator HealthIndicators} with the key
	 * being used as an indicator name.
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator, Map<String, HealthIndicator> indicators) {
		this(healthAggregator, new DefaultHealthIndicatorRegistry(indicators));
	}

	/**
	 * Create a new {@link CompositeHealthIndicator} from the indicators in the given
	 * {@code registry}.
	 * @param healthAggregator the health aggregator
	 * @param registry the registry of {@link HealthIndicator HealthIndicators}.
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator, HealthIndicatorRegistry registry) {
		this(healthAggregator, registry, new SequentialStrategy());
	}

	private CompositeHealthIndicator(HealthAggregator healthAggregator, HealthIndicatorRegistry registry,
			HealthStrategy strategy) {
		this.aggregator = healthAggregator;
		this.registry = registry;
		this.strategy = strategy;
	}

	/**
	 * Return the {@link HealthIndicatorRegistry} of this instance.
	 * @return the registry of nested {@link HealthIndicator health indicators}
	 * @since 2.1.0
	 */
	public HealthIndicatorRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Returns a new {@link CompositeHealthIndicator} with a parallel strategy that
	 * returns health indications from all registered delegates concurrently.
	 * @param timeout number of milliseconds to wait before using the
	 * {@code timeoutHealth}
	 * @param timeoutHealth the {@link Health} to use if an health indicator reached the
	 * {@code timeout}. Defaults to {@code unknown} status.
	 * @param executor the executor to submit {@link HealthIndicator HealthIndicators} on.
	 * @return new instance with a parallel strategy
	 * @since 2.2.0
	 */
	public CompositeHealthIndicator parallel(Executor executor, long timeout, Health timeoutHealth) {
		Assert.notNull(executor, "Executor must not be null");
		ParallelStrategy strategy = new ParallelStrategy(executor, timeout, timeoutHealth);
		return new CompositeHealthIndicator(this.aggregator, this.registry, strategy);
	}

	/**
	 * Returns a new {@link CompositeHealthIndicator} with a parallel strategy that
	 * returns health indications from all registered delegates concurrently.
	 * @param executor the executor to submit {@link HealthIndicator HealthIndicators} on.
	 * @return new instance with a parallel strategy
	 * @since 2.2.0
	 */
	public CompositeHealthIndicator parallel(Executor executor) {
		Assert.notNull(executor, "Executor must not be null");
		ParallelStrategy strategy = new ParallelStrategy(executor, null, null);
		return new CompositeHealthIndicator(this.aggregator, this.registry, strategy);
	}

	/**
	 * Returns a new {@link CompositeHealthIndicator} with a sequential strategy that
	 * returns health indications from all registered delegates sequentially.
	 * @return new instance with a sequential strategy
	 * @since 2.2.0
	 */
	public CompositeHealthIndicator sequential() {
		return new CompositeHealthIndicator(this.aggregator, this.registry, new SequentialStrategy());
	}

	@Override
	public Health health() {
		Map<String, Health> healths = this.strategy.doHealth(this.registry.getAll());
		return this.aggregator.aggregate(healths);
	}

	@FunctionalInterface
	private interface HealthStrategy {

		Map<String, Health> doHealth(Map<String, HealthIndicator> healthIndicators);

	}

	private static final class SequentialStrategy implements HealthStrategy {

		@Override
		public Map<String, Health> doHealth(Map<String, HealthIndicator> healthIndicators) {
			Map<String, Health> healths = new LinkedHashMap<>();
			for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
				healths.put(entry.getKey(), entry.getValue().health());
			}
			return healths;
		}

	}

	private static final class ParallelStrategy implements HealthStrategy {

		private final Executor executor;

		private final Long timeout;

		private final Health timeoutHealth;

		private ParallelStrategy(Executor executor, Long timeout, Health timeoutHealth) {
			this.executor = executor;
			this.timeout = timeout;
			this.timeoutHealth = (timeoutHealth != null) ? timeoutHealth : Health.unknown().build();
		}

		@Override
		public Map<String, Health> doHealth(Map<String, HealthIndicator> healthIndicators) {
			Map<String, Future<Health>> healthsFutures = new LinkedHashMap<>();
			for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
				healthsFutures.put(entry.getKey(),
						CompletableFuture.supplyAsync(entry.getValue()::health, this.executor));
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

}
