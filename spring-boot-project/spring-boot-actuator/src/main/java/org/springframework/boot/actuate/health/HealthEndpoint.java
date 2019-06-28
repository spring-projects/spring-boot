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
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose application health information.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Endpoint(id = "health")
public class HealthEndpoint {

	private final HealthIndicator healthIndicator;

	/**
	 * Create a new {@link HealthEndpoint} instance that will use the given
	 * {@code healthIndicator} to generate its response.
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

	/**
	 * Return the {@link Health} of a particular component or {@code null} if such
	 * component does not exist.
	 * @param component the name of a particular {@link HealthIndicator}
	 * @return the {@link Health} for the component or {@code null}
	 */
	@ReadOperation
	public Health healthForComponent(@Selector String component) {
		HealthIndicator indicator = getNestedHealthIndicator(this.healthIndicator, component);
		return (indicator != null) ? indicator.health() : null;
	}

	/**
	 * Return the {@link Health} of a particular {@code instance} managed by the specified
	 * {@code component} or {@code null} if that particular component is not a
	 * {@link CompositeHealthIndicator} or if such instance does not exist.
	 * @param component the name of a particular {@link CompositeHealthIndicator}
	 * @param instance the name of an instance managed by that component
	 * @return the {@link Health} for the component instance of {@code null}
	 */
	@ReadOperation
	public Health healthForComponentInstance(@Selector String component, @Selector String instance) {
		HealthIndicator indicator = getNestedHealthIndicator(this.healthIndicator, component);
		HealthIndicator nestedIndicator = getNestedHealthIndicator(indicator, instance);
		return (nestedIndicator != null) ? nestedIndicator.health() : null;
	}

	private HealthIndicator getNestedHealthIndicator(HealthIndicator healthIndicator, String name) {
		if (healthIndicator instanceof CompositeHealthIndicator) {
			return ((CompositeHealthIndicator) healthIndicator).getRegistry().get(name);
		}
		return null;
	}

}
