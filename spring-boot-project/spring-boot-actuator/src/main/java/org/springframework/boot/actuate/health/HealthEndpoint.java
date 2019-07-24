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

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

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

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param healthIndicator the health indicator
	 */
	public HealthEndpoint(HealthIndicator healthIndicator) {
		Assert.notNull(healthIndicator, "HealthIndicator must not be null");
		this.healthIndicator = healthIndicator;
	}

	@ReadOperation
	public Health health() {
		return this.healthIndicator.health();
	}

}
