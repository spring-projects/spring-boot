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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose application health.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
@Endpoint(id = "health")
public class HealthEndpoint {

	private final HealthIndicator healthIndicator;

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param healthAggregator the health aggregator
	 * @param healthIndicators the health indicators
	 */
	public HealthEndpoint(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> healthIndicators) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(healthIndicators, "HealthIndicators must not be null");
		CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(
				healthAggregator);
		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
			healthIndicator.addHealthIndicator(getKey(entry.getKey()), entry.getValue());
		}
		this.healthIndicator = healthIndicator;
	}

	@ReadOperation
	public Health health() {
		return this.healthIndicator.health();
	}

	/**
	 * Turns the bean name into a key that can be used in the map of health information.
	 * @param name the bean name
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
