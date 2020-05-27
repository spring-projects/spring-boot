/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Map;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * Factory to create a {@link HealthIndicatorRegistry}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 * @deprecated since 2.2.0 in favor of {@link DefaultHealthIndicatorRegistry}
 */
@Deprecated
public class HealthIndicatorRegistryFactory {

	private final Function<String, String> healthIndicatorNameFactory;

	public HealthIndicatorRegistryFactory(Function<String, String> healthIndicatorNameFactory) {
		this.healthIndicatorNameFactory = healthIndicatorNameFactory;
	}

	public HealthIndicatorRegistryFactory() {
		this(new HealthIndicatorNameFactory());
	}

	/**
	 * Create a {@link HealthIndicatorRegistry} based on the specified health indicators.
	 * @param healthIndicators the {@link HealthIndicator} instances mapped by name
	 * @return a {@link HealthIndicator} that delegates to the specified
	 * {@code healthIndicators}.
	 */
	public HealthIndicatorRegistry createHealthIndicatorRegistry(Map<String, HealthIndicator> healthIndicators) {
		Assert.notNull(healthIndicators, "HealthIndicators must not be null");
		return initialize(new DefaultHealthIndicatorRegistry(), healthIndicators);
	}

	protected <T extends HealthIndicatorRegistry> T initialize(T registry,
			Map<String, HealthIndicator> healthIndicators) {
		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
			String name = this.healthIndicatorNameFactory.apply(entry.getKey());
			registry.register(name, entry.getValue());
		}
		return registry;
	}

}
