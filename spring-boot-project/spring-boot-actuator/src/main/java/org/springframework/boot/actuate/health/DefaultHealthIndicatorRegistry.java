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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link HealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.1.0
 * @deprecated since 2.2.0 for removal in 2.4.0 in favor of
 * {@link DefaultContributorRegistry}
 */
@Deprecated
public class DefaultHealthIndicatorRegistry implements HealthIndicatorRegistry {

	private final Object monitor = new Object();

	private final Map<String, HealthIndicator> healthIndicators;

	/**
	 * Create a new {@link DefaultHealthIndicatorRegistry}.
	 */
	public DefaultHealthIndicatorRegistry() {
		this(new LinkedHashMap<>());
	}

	/**
	 * Create a new {@link DefaultHealthIndicatorRegistry} from the specified indicators.
	 * @param healthIndicators a map of {@link HealthIndicator}s with the key being used
	 * as an indicator name.
	 */
	public DefaultHealthIndicatorRegistry(Map<String, HealthIndicator> healthIndicators) {
		Assert.notNull(healthIndicators, "HealthIndicators must not be null");
		this.healthIndicators = new LinkedHashMap<>(healthIndicators);
	}

	@Override
	public void register(String name, HealthIndicator healthIndicator) {
		Assert.notNull(healthIndicator, "HealthIndicator must not be null");
		Assert.notNull(name, "Name must not be null");
		synchronized (this.monitor) {
			HealthIndicator existing = this.healthIndicators.putIfAbsent(name, healthIndicator);
			if (existing != null) {
				throw new IllegalStateException("HealthIndicator with name '" + name + "' already registered");
			}
		}
	}

	@Override
	public HealthIndicator unregister(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.monitor) {
			return this.healthIndicators.remove(name);
		}
	}

	@Override
	public HealthIndicator get(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.monitor) {
			return this.healthIndicators.get(name);
		}
	}

	@Override
	public Map<String, HealthIndicator> getAll() {
		synchronized (this.monitor) {
			return Collections.unmodifiableMap(new LinkedHashMap<>(this.healthIndicators));
		}
	}

}
