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

import java.util.Map;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * Factory to create a {@link CompositeHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @deprecated since 2.1.0 in favor of
 * {@link CompositeHealthIndicator#CompositeHealthIndicator(HealthAggregator, HealthIndicatorRegistry)}
 */
@Deprecated
public class CompositeHealthIndicatorFactory {

	private final Function<String, String> healthIndicatorNameFactory;

	public CompositeHealthIndicatorFactory() {
		this(new HealthIndicatorNameFactory());
	}

	public CompositeHealthIndicatorFactory(
			Function<String, String> healthIndicatorNameFactory) {
		this.healthIndicatorNameFactory = healthIndicatorNameFactory;
	}

	/**
	 * Create a {@link CompositeHealthIndicator} based on the specified health indicators.
	 * @param healthAggregator the {@link HealthAggregator}
	 * @param healthIndicators the {@link HealthIndicator} instances mapped by name
	 * @return a {@link HealthIndicator} that delegates to the specified
	 * {@code healthIndicators}.
	 */
	public CompositeHealthIndicator createHealthIndicator(
			HealthAggregator healthAggregator,
			Map<String, HealthIndicator> healthIndicators) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(healthIndicators, "HealthIndicators must not be null");
		HealthIndicatorRegistryFactory factory = new HealthIndicatorRegistryFactory(
				this.healthIndicatorNameFactory);
		return new CompositeHealthIndicator(healthAggregator,
				factory.createHealthIndicatorRegistry(healthIndicators));
	}

}
