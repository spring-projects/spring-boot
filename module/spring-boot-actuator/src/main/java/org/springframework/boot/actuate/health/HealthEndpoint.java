/*
 * Copyright 2012-present the original author or authors.
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

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;

/**
 * {@link Endpoint @Endpoint} to expose application health information.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@Endpoint(id = "health")
public class HealthEndpoint extends HealthEndpointSupport<Health, HealthDescriptor> {

	/**
	 * Health endpoint id.
	 */
	public static final EndpointId ID = EndpointId.of("health");

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param registry the health contributor registry
	 * @param fallbackRegistry the fallback registry or {@code null}
	 * @param groups the health endpoint groups
	 * @param slowContributorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 * @since 4.0.0
	 */
	public HealthEndpoint(HealthContributorRegistry registry, ReactiveHealthContributorRegistry fallbackRegistry,
			HealthEndpointGroups groups, Duration slowContributorLoggingThreshold) {
		super(Contributor.blocking(registry, fallbackRegistry), groups, slowContributorLoggingThreshold);
	}

	@ReadOperation
	public HealthDescriptor health() {
		HealthDescriptor health = health(ApiVersion.V3, EMPTY_PATH);
		return (health != null) ? health : IndicatedHealthDescriptor.UP;
	}

	@ReadOperation
	public HealthDescriptor healthForPath(@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(ApiVersion.V3, path);
	}

	private HealthDescriptor health(ApiVersion apiVersion, String... path) {
		Result<HealthDescriptor> result = getResult(apiVersion, null, SecurityContext.NONE, true, path);
		return (result != null) ? result.descriptor() : null;
	}

	@Override
	protected HealthDescriptor aggregateDescriptors(ApiVersion apiVersion, Map<String, HealthDescriptor> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames) {
		return getCompositeDescriptor(apiVersion, contributions, statusAggregator, showComponents, groupNames);
	}

}
