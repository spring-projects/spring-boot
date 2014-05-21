/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose application health.
 * 
 * @author Dave Syer
 * @author Christian Dupuis
 */
@ConfigurationProperties(prefix = "endpoints.health", ignoreUnknownFields = false)
public class HealthEndpoint extends AbstractEndpoint<Health> {

	private final HealthIndicator healthIndicator;

	/**
	 * Create a new {@link HealthIndicator} instance.
	 */
	public HealthEndpoint(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> healthIndicators) {
		super("health", false, true);

		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(healthIndicators, "HealthIndicators must not be null");

		if (healthIndicators.size() == 1) {
			this.healthIndicator = healthIndicators.values().iterator().next();
		}
		else {
			CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(
					healthAggregator);
			for (Map.Entry<String, HealthIndicator> h : healthIndicators.entrySet()) {
				healthIndicator.addHealthIndicator(getKey(h.getKey()), h.getValue());
			}
			this.healthIndicator = healthIndicator;
		}
	}

	/**
	 * Invoke all {@link HealthIndicator} delegates and collect their health information.
	 */
	@Override
	public Health invoke() {
		return this.healthIndicator.health();
	}

	/**
	 * Turns the bean name into a key that can be used in the map of health information.
	 */
	private String getKey(String name) {
		int index = name.toLowerCase().indexOf("healthindicator");
		if (index > 0) {
			return name.substring(0, index);
		}
		return name;
	}
}
