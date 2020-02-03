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

import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;

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
public class HealthEndpoint extends HealthEndpointSupport<HealthContributor, HealthComponent> {

	private static final String[] EMPTY_PATH = {};

	/**
	 * Create a new {@link HealthEndpoint} instance that will use the given {@code
	 * healthIndicator} to generate its response.
	 * @param healthIndicator the health indicator
	 * @deprecated since 2.2.0 in favor of
	 * {@link #HealthEndpoint(HealthContributorRegistry, HealthEndpointGroups)}
	 */
	@Deprecated
	public HealthEndpoint(HealthIndicator healthIndicator) {
	}

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 */
	public HealthEndpoint(HealthContributorRegistry registry, HealthEndpointGroups groups) {
		super(registry, groups);
	}

	@ReadOperation
	public HealthComponent health() {
		HealthComponent health = health(ApiVersion.V3, EMPTY_PATH);
		return (health != null) ? health : DEFAULT_HEALTH;
	}

	@ReadOperation
	public HealthComponent healthForPath(@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(ApiVersion.V3, path);
	}

	private HealthComponent health(ApiVersion apiVersion, String... path) {
		HealthResult<HealthComponent> result = getHealth(apiVersion, SecurityContext.NONE, true, path);
		return (result != null) ? result.getHealth() : null;
	}

	@Override
	protected HealthComponent getHealth(HealthContributor contributor, boolean includeDetails) {
		return ((HealthIndicator) contributor).getHealth(includeDetails);
	}

	@Override
	protected HealthComponent aggregateContributions(ApiVersion apiVersion, Map<String, HealthComponent> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames) {
		return getCompositeHealth(apiVersion, contributions, statusAggregator, showComponents, groupNames);
	}

}
