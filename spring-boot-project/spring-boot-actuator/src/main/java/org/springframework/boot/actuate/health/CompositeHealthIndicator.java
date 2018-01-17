/*
 * Copyright 2012-2017 the original author or authors.
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

	private final Map<String, HealthIndicator> indicators;

	private final HealthAggregator healthAggregator;

	/**
	 * Create a new {@link CompositeHealthIndicator}.
	 * @param healthAggregator the health aggregator
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator) {
		this(healthAggregator, new LinkedHashMap<>());
	}

	/**
	 * Create a new {@link CompositeHealthIndicator} from the specified indicators.
	 * @param healthAggregator the health aggregator
	 * @param indicators a map of {@link HealthIndicator}s with the key being used as an
	 * indicator name.
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> indicators) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(indicators, "Indicators must not be null");
		this.indicators = new LinkedHashMap<>(indicators);
		this.healthAggregator = healthAggregator;
	}

	public void addHealthIndicator(String name, HealthIndicator indicator) {
		this.indicators.put(name, indicator);
	}

	@Override
	public Health health() {
		Map<String, Health> healths = new LinkedHashMap<>();
		for (Map.Entry<String, HealthIndicator> entry : this.indicators.entrySet()) {
			healths.put(entry.getKey(), entry.getValue().health());
		}
		return this.healthAggregator.aggregate(healths);
	}

}
