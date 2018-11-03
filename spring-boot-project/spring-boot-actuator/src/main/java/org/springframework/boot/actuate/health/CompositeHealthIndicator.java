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

import java.util.LinkedHashMap;
import java.util.Map;

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

	/**
	 * Create a new {@link CompositeHealthIndicator}.
	 * @param healthAggregator the health aggregator
	 * @deprecated since 2.1.0 in favor of
	 * {@link #CompositeHealthIndicator(HealthAggregator, HealthIndicatorRegistry)}
	 */
	@Deprecated
	public CompositeHealthIndicator(HealthAggregator healthAggregator) {
		this(healthAggregator, new DefaultHealthIndicatorRegistry());
	}

	/**
	 * Create a new {@link CompositeHealthIndicator} from the specified indicators.
	 * @param healthAggregator the health aggregator
	 * @param indicators a map of {@link HealthIndicator HealthIndicators} with the key
	 * being used as an indicator name.
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> indicators) {
		this(healthAggregator, new DefaultHealthIndicatorRegistry(indicators));
	}

	/**
	 * Create a new {@link CompositeHealthIndicator} from the indicators in the given
	 * {@code registry}.
	 * @param healthAggregator the health aggregator
	 * @param registry the registry of {@link HealthIndicator HealthIndicators}.
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator,
			HealthIndicatorRegistry registry) {
		this.aggregator = healthAggregator;
		this.registry = registry;
	}

	/**
	 * Adds the given {@code healthIndicator}, associating it with the given {@code name}.
	 * @param name the name of the indicator
	 * @param indicator the indicator
	 * @throws IllegalStateException if an indicator with the given {@code name} is
	 * already registered.
	 * @deprecated since 2.1.0 in favor of
	 * {@link HealthIndicatorRegistry#register(String, HealthIndicator)}
	 */
	@Deprecated
	public void addHealthIndicator(String name, HealthIndicator indicator) {
		this.registry.register(name, indicator);
	}

	/**
	 * Return the {@link HealthIndicatorRegistry} of this instance.
	 * @return the registry of nested {@link HealthIndicator health indicators}
	 * @since 2.1.0
	 */
	public HealthIndicatorRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public Health health() {
		Map<String, Health> healths = new LinkedHashMap<>();
		for (Map.Entry<String, HealthIndicator> entry : this.registry.getAll()
				.entrySet()) {
			healths.put(entry.getKey(), entry.getValue().health());
		}
		return this.aggregator.aggregate(healths);
	}

}
