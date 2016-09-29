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

package org.springframework.boot.actuate.health;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link HealthIndicator} that returns health indications from all
 * registered delegates.
 *
 * @author Tyler J. Frederick
 */
public class CompositeHealthIndicator implements HealthIndicator<Map<String, Object>> {

	private final Map<String, HealthIndicator<?>> healthIndicators =
			new HashMap<String, HealthIndicator<?>>();

	public CompositeHealthIndicator(Map<String, HealthIndicator<?>> healthIndicators) {
		this.healthIndicators.putAll(healthIndicators);
	}

	@Override
	public Map<String, Object> health() {
		Map<String, Object> healthIndications = new HashMap<String, Object>();

		for (String indicatorName : this.healthIndicators.keySet()) {
			Object healthIndication = this.healthIndicators.get(indicatorName).health();
			healthIndications.put(indicatorName, healthIndication);
		}

		return healthIndications;
	}

	public void addHealthIndicator(String indicatorName, HealthIndicator<?> healthIndicator) {
		healthIndicators.put(indicatorName, healthIndicator);
	}
}
