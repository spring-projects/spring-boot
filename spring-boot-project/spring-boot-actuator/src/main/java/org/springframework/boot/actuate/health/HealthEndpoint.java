/*
 * Copyright 2012-2017 the original author or authors.
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

/**
 * {@link Endpoint} to expose application health information.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "health")
public class HealthEndpoint {

	private final HealthIndicator healthIndicator;

	private final boolean showDetails;

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param healthIndicator the health indicator
	 * @param showDetails if full details should be returned instead of just the status
	 */
	public HealthEndpoint(HealthIndicator healthIndicator, boolean showDetails) {
		this.healthIndicator = healthIndicator;
		this.showDetails = showDetails;
	}

	@ReadOperation
	public Health health() {
		Health health = this.healthIndicator.health();
		if (this.showDetails) {
			return health;
		}
		return Health.status(health.getStatus()).build();
	}

	public HealthIndicator getHealthIndicator() {
		return this.healthIndicator;
	}

}
