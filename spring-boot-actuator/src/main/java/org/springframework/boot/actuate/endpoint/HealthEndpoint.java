/*
 * Copyright 2012-2016 the original author or authors.
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

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose application health.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
@ConfigurationProperties(prefix = "endpoints.health", ignoreUnknownFields = true)
public class HealthEndpoint extends AbstractEndpoint<Health> {

	private final HealthAggregator healthAggregator;
	private final HealthIndicatorRegistry healthIndicatorRegistry;

	/**
	 * Time to live for cached result, in milliseconds.
	 */
	private long timeToLive = 1000;

	/**
	 * Create a new {@link HealthIndicator} instance.
	 * @param healthAggregator the health aggregator
	 * @param healthIndicatorRegistry the health indicator registry
	 */
	public HealthEndpoint(HealthAggregator healthAggregator,
			HealthIndicatorRegistry healthIndicatorRegistry) {
		super("health", false);
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(healthIndicatorRegistry,
				"healthIndicatorRegistry must not be null");
		this.healthAggregator = healthAggregator;
		this.healthIndicatorRegistry = healthIndicatorRegistry;
	}

	/**
	 * Time to live for cached result. This is particularly useful to cache the result of
	 * this endpoint to prevent a DOS attack if it is accessed anonymously.
	 * @return time to live in milliseconds (default 1000)
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}

	public void setTimeToLive(long ttl) {
		this.timeToLive = ttl;
	}

	/**
	 * Invoke all {@link HealthIndicator} delegates and collect their health information.
	 */
	@Override
	public Health invoke() {
		Map<String, Health> healths = this.healthIndicatorRegistry.runHealthIndicators();
		return this.healthAggregator.aggregate(healths);
	}

}
