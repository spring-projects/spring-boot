/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * {@link ReactiveHealthIndicator} that returns health indications from all registered
 * delegates. Provides an alternative {@link Health} for a delegate that reaches a
 * configurable timeout.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @deprecated since 2.2.0 for removal in 2.4.0 in favor of a
 * {@link CompositeReactiveHealthContributor}
 */
@Deprecated
public class CompositeReactiveHealthIndicator implements ReactiveHealthIndicator {

	private final ReactiveHealthIndicatorRegistry registry;

	private final HealthAggregator healthAggregator;

	private Long timeout;

	private Health timeoutHealth;

	private final Function<Mono<Health>, Mono<Health>> timeoutCompose;

	/**
	 * Create a new {@link CompositeReactiveHealthIndicator} from the indicators in the
	 * given {@code registry}.
	 * @param healthAggregator the health aggregator
	 * @param registry the registry of {@link ReactiveHealthIndicator HealthIndicators}.
	 */
	public CompositeReactiveHealthIndicator(HealthAggregator healthAggregator,
			ReactiveHealthIndicatorRegistry registry) {
		this.registry = registry;
		this.healthAggregator = healthAggregator;
		this.timeoutCompose = (mono) -> (this.timeout != null)
				? mono.timeout(Duration.ofMillis(this.timeout), Mono.just(this.timeoutHealth)) : mono;
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

	ReactiveHealthIndicatorRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public Mono<Health> health() {
		return Flux.fromIterable(this.registry.getAll().entrySet())
				.flatMap((entry) -> Mono.zip(Mono.just(entry.getKey()),
						entry.getValue().health().transformDeferred(this.timeoutCompose)))
				.collectMap(Tuple2::getT1, Tuple2::getT2).map(this.healthAggregator::aggregate);
	}

}
