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

import java.util.Map;

import org.springframework.util.Assert;

/**
 * Factory to create a {@link HealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class HealthIndicatorFactory {

	/**
	 * Create a {@link CompositeHealthIndicator} based on the specified health indicators.
	 * @param healthAggregator the {@link HealthAggregator}
	 * @param healthIndicators the {@link HealthIndicator} instances mapped by name
	 * @return a {@link HealthIndicator} that delegates to the specified
	 * {@code healthIndicators}.
	 */
	public HealthIndicator createHealthIndicator(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> healthIndicators) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(healthIndicators, "HealthIndicators must not be null");
		CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(
				healthAggregator);
		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
			healthIndicator.addHealthIndicator(getKey(entry.getKey()), entry.getValue());
		}
		return healthIndicator;
	}

	/**
	 * Turns the health indicator name into a key that can be used in the map of health
	 * information.
	 * @param name the health indicator name
	 * @return the key
	 */
	private String getKey(String name) {
		int index = name.toLowerCase().indexOf("healthindicator");
		if (index > 0) {
			return name.substring(0, index);
		}
		return name;
	}

}
