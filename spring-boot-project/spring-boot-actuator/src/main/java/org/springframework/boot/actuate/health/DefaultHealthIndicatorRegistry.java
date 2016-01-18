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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link HealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 * @since 2.1.0
 */
public class DefaultHealthIndicatorRegistry implements HealthIndicatorRegistry {

	private final Map<String, HealthIndicator> healthIndicators = new HashMap<>();

	@Override
	public void register(String name, HealthIndicator healthIndicator) {
		Assert.notNull(healthIndicator, "HealthIndicator must not be null");
		synchronized (this.healthIndicators) {
			if (this.healthIndicators.get(name) != null) {
				throw new IllegalStateException(
						"HealthIndicator with name '" + name + "' already registered");
			}
			this.healthIndicators.put(name, healthIndicator);
		}
	}

	@Override
	public HealthIndicator unregister(String name) {
		synchronized (this.healthIndicators) {
			return this.healthIndicators.remove(name);
		}
	}

	@Override
	public HealthIndicator get(String name) {
		synchronized (this.healthIndicators) {
			return this.healthIndicators.get(name);
		}
	}

	@Override
	public Map<String, HealthIndicator> getAll() {
		synchronized (this.healthIndicators) {
			return Collections.unmodifiableMap(new HashMap<>(this.healthIndicators));
		}
	}

}
