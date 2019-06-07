/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Factory to create a {@link HealthIndicatorRegistry}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class ReactiveHealthIndicatorRegistryFactory {

	private final Function<String, String> healthIndicatorNameFactory;

	public ReactiveHealthIndicatorRegistryFactory(Function<String, String> healthIndicatorNameFactory) {
		this.healthIndicatorNameFactory = healthIndicatorNameFactory;
	}

	public ReactiveHealthIndicatorRegistryFactory() {
		this(new HealthIndicatorNameFactory());
	}

	/**
	 * Create a {@link ReactiveHealthIndicatorRegistry} based on the specified health
	 * indicators. Each {@link HealthIndicator} are wrapped to a
	 * {@link HealthIndicatorReactiveAdapter}. If two instances share the same name, the
	 * reactive variant takes precedence.
	 * @param reactiveHealthIndicators the {@link ReactiveHealthIndicator} instances
	 * mapped by name
	 * @param healthIndicators the {@link HealthIndicator} instances mapped by name if
	 * any.
	 * @return a {@link ReactiveHealthIndicator} that delegates to the specified
	 * {@code reactiveHealthIndicators}.
	 */
	public ReactiveHealthIndicatorRegistry createReactiveHealthIndicatorRegistry(
			Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
			Map<String, HealthIndicator> healthIndicators) {
		Assert.notNull(reactiveHealthIndicators, "ReactiveHealthIndicators must not be null");
		return initialize(new DefaultReactiveHealthIndicatorRegistry(), reactiveHealthIndicators, healthIndicators);
	}

	protected <T extends ReactiveHealthIndicatorRegistry> T initialize(T registry,
			Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
			Map<String, HealthIndicator> healthIndicators) {
		merge(reactiveHealthIndicators, healthIndicators).forEach((beanName, indicator) -> {
			String name = this.healthIndicatorNameFactory.apply(beanName);
			registry.register(name, indicator);
		});
		return registry;
	}

	private Map<String, ReactiveHealthIndicator> merge(Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
			Map<String, HealthIndicator> healthIndicators) {
		if (ObjectUtils.isEmpty(healthIndicators)) {
			return reactiveHealthIndicators;
		}
		Map<String, ReactiveHealthIndicator> allIndicators = new LinkedHashMap<>(reactiveHealthIndicators);
		healthIndicators.forEach((beanName, indicator) -> {
			String name = this.healthIndicatorNameFactory.apply(beanName);
			allIndicators.computeIfAbsent(name, (n) -> new HealthIndicatorReactiveAdapter(indicator));
		});
		return allIndicators;
	}

}
