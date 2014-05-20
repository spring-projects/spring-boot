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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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
public class HealthEndpoint extends AbstractEndpoint<Map<String, Object>> {

	private final Map<String, HealthIndicator<? extends Object>> healthIndicators;

	/**
	 * Create a new {@link HealthIndicator} instance.
	 */
	public HealthEndpoint(Map<String, HealthIndicator<? extends Object>> healthIndicators) {
		super("health", false, true);
		Assert.notNull(healthIndicators, "HealthIndicator must not be null");
		this.healthIndicators = healthIndicators;
	}

	/**
	 * Invoke all {@link HealthIndicator} delegates and collect their health information.
	 */
	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> health = new LinkedHashMap<String, Object>();
		for (Entry<String, HealthIndicator<?>> entry : this.healthIndicators.entrySet()) {
			health.put(getKey(entry.getKey()), entry.getValue().health());
		}
		return health;
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
