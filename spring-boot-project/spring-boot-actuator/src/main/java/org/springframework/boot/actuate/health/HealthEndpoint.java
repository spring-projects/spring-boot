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

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose application health information.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Endpoint(id = "health")
public class HealthEndpoint {

	private final HealthAggregator healthAggregator;

	private final HealthIndicatorRegistry healthIndicatorRegistry;

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param healthAggregator the health aggregator
	 * @param healthIndicatorRegistry the health indicator registry
	 */
	public HealthEndpoint(HealthAggregator healthAggregator,
			HealthIndicatorRegistry healthIndicatorRegistry) {
		Assert.notNull(healthAggregator, "healthAggregator must not be null");
		Assert.notNull(healthIndicatorRegistry, "healthIndicatorRegistry must not be null");
		this.healthAggregator = healthAggregator;
		this.healthIndicatorRegistry = healthIndicatorRegistry;
	}

	@ReadOperation
	public Health health() {
		CompositeHealthIndicatorFactory factory = new CompositeHealthIndicatorFactory();
		CompositeHealthIndicator healthIndicator = factory.createHealthIndicator(
				this.healthAggregator, this.healthIndicatorRegistry.getAll());
		return healthIndicator.health();
	}

}
