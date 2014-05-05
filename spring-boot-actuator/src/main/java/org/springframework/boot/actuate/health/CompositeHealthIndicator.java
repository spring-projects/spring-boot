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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link HealthIndicator} that returns health indications from all registered delegates.
 * 
 * @author Tyler J. Frederick
 * @author Phillip Webb
 */
public class CompositeHealthIndicator implements HealthIndicator<Map<String, Object>> {

	private final Map<String, HealthIndicator<?>> indicators;

	/**
	 * Create a new {@link CompositeHealthIndicator}.
	 */
	public CompositeHealthIndicator() {
		this.indicators = new LinkedHashMap<String, HealthIndicator<?>>();
	}

	/**
	 * Create a new {@link CompositeHealthIndicator} from the specified indicators.
	 * @param indicators a map of {@link HealthIndicator}s with the key being used as an
	 * indicator name.
	 */
	public CompositeHealthIndicator(Map<String, HealthIndicator<?>> indicators) {
		this.indicators = new LinkedHashMap<String, HealthIndicator<?>>(indicators);
	}

	public void addHealthIndicator(String name, HealthIndicator<?> indicator) {
		this.indicators.put(name, indicator);
	}

	@Override
	public Map<String, Object> health() {
		Map<String, Object> health = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, HealthIndicator<?>> entry : this.indicators.entrySet()) {
			health.put(entry.getKey(), entry.getValue().health());
		}
		return health;
	}

}
