/*
 * Copyright 2012-2022 the original author or authors.
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
public class HealthEndpoint extends HealthEndpointSupport<HealthContributor, HealthComponent> {

	/**
	 * Health endpoint id.
	 */
	public static final EndpointId ID = EndpointId.of("health");

	private static final String[] EMPTY_PATH = {};

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 * @since 2.6.9
	 */
	public HealthEndpoint(HealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		super(registry, groups, slowIndicatorLoggingThreshold);
	}

	/**
	 * Retrieves the health component.
	 * @return the health component
	 */
	@ReadOperation
	public HealthComponent health() {
		HealthComponent health = health(ApiVersion.V3, EMPTY_PATH);
		return (health != null) ? health : DEFAULT_HEALTH;
	}

	/**
	 * Retrieves the health component for the specified path.
	 * @param path The path to retrieve the health component for.
	 * @return The health component for the specified path.
	 */
	@ReadOperation
	public HealthComponent healthForPath(@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(ApiVersion.V3, path);
	}

	/**
	 * Retrieves the health component for the specified API version and path.
	 * @param apiVersion The API version to retrieve the health component for.
	 * @param path The path to retrieve the health component for.
	 * @return The health component for the specified API version and path, or null if not
	 * found.
	 */
	private HealthComponent health(ApiVersion apiVersion, String... path) {
		HealthResult<HealthComponent> result = getHealth(apiVersion, null, SecurityContext.NONE, true, path);
		return (result != null) ? result.getHealth() : null;
	}

	/**
	 * Retrieves the health component of a given health contributor.
	 * @param contributor the health contributor to retrieve the health component from
	 * @param includeDetails a boolean indicating whether to include detailed information
	 * in the health component
	 * @return the health component of the given health contributor
	 */
	@Override
	protected HealthComponent getHealth(HealthContributor contributor, boolean includeDetails) {
		return ((HealthIndicator) contributor).getHealth(includeDetails);
	}

	/**
	 * Aggregates the contributions from different health components and returns the
	 * composite health status.
	 * @param apiVersion The version of the API.
	 * @param contributions A map containing the contributions from different health
	 * components.
	 * @param statusAggregator The aggregator used to determine the overall health status.
	 * @param showComponents A flag indicating whether to include individual health
	 * components in the response.
	 * @param groupNames A set of group names to filter the health components.
	 * @return The composite health status.
	 */
	@Override
	protected HealthComponent aggregateContributions(ApiVersion apiVersion, Map<String, HealthComponent> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames) {
		return getCompositeHealth(apiVersion, contributions, statusAggregator, showComponents, groupNames);
	}

}
