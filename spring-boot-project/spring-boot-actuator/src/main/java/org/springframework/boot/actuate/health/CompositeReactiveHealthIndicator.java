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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import org.springframework.util.Assert;

/**
 * {@link ReactiveHealthIndicator} that returns health indications from all registered
 * delegates. Provides an alternative {@link Health} for a delegate that reaches a
 * configurable timeout.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CompositeReactiveHealthIndicator implements ReactiveHealthIndicator {

	private final Map<String, ReactiveHealthIndicator> indicators;

	private final HealthAggregator healthAggregator;

	private Long timeout;

	private Health timeoutHealth;

	private final Function<Mono<Health>, Mono<Health>> timeoutCompose;

	public CompositeReactiveHealthIndicator(HealthAggregator healthAggregator) {
		this(healthAggregator, new LinkedHashMap<>());
	}

	public CompositeReactiveHealthIndicator(HealthAggregator healthAggregator,
			Map<String, ReactiveHealthIndicator> indicators) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(indicators, "Indicators must not be null");
		this.indicators = new LinkedHashMap<>(indicators);
		this.healthAggregator = healthAggregator;
		this.timeoutCompose = (mono) -> (this.timeout != null)
				? mono.timeout(Duration.ofMillis(this.timeout), Mono.just(this.timeoutHealth)) : mono;
	}

	/**
	 * Add a {@link ReactiveHealthIndicator} with the specified name.
	 * @param name the name of the health indicator
	 * @param indicator the health indicator to add
	 * @return this instance
	 */
	public CompositeReactiveHealthIndicator addHealthIndicator(String name, ReactiveHealthIndicator indicator) {
		this.indicators.put(name, indicator);
		return this;
	}

	/**
	 * Specify an alternative timeout {@link Health} if a {@link HealthIndicator} failed
	 * to reply after specified {@code timeout}.
	 * @param timeout number of milliseconds to wait before using the
	 * {@code timeoutHealth}
	 * @param timeoutHealth the {@link Health} to use if an health indicator reached the
	 * {@code timeout}
	 * @return this instance
	 */
	public CompositeReactiveHealthIndicator timeoutStrategy(long timeout, Health timeoutHealth) {
		this.timeout = timeout;
		this.timeoutHealth = (timeoutHealth != null) ? timeoutHealth : Health.unknown().build();
		return this;
	}

	@Override
	public Mono<Health> health() {
		return Flux.fromIterable(this.indicators.entrySet()).flatMap(
				(entry) -> Mono.zip(Mono.just(entry.getKey()), entry.getValue().health().compose(this.timeoutCompose)))
				.collectMap(Tuple2::getT1, Tuple2::getT2).map(this.healthAggregator::aggregate);
	}

}
