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
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.util.Assert;

/**
 * Base class for health endpoints and health endpoint extensions.
 *
 * @param <C> the contributor type
 * @param <T> the contributed health component type
 * @author Phillip Webb
 */
abstract class HealthEndpointSupport<C, T> {

	private final ContributorRegistry<C> registry;

	private final HealthEndpointSettings settings;

	/**
	 * Throw a new {@link IllegalStateException} to indicate a constructor has been
	 * deprecated.
	 * @deprecated since 2.2.0 in order to support deprecated subclass constructors
	 */
	@Deprecated
	HealthEndpointSupport() {
		throw new IllegalStateException("Unable to create " + getClass() + " using deprecated constructor");
	}

	/**
	 * Create a new {@link HealthEndpointSupport} instance.
	 * @param registry the health contributor registry
	 * @param settings the health settings
	 */
	HealthEndpointSupport(ContributorRegistry<C> registry, HealthEndpointSettings settings) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(settings, "Settings must not be null");
		this.registry = registry;
		this.settings = settings;
	}

	/**
	 * Return the health endpoint settings.
	 * @return the settings
	 */
	protected final HealthEndpointSettings getSettings() {
		return this.settings;
	}

	T getHealth(SecurityContext securityContext, boolean alwaysIncludeDetails, String... path) {
		boolean includeDetails = alwaysIncludeDetails || this.settings.includeDetails(securityContext);
		boolean isRoot = path.length == 0;
		if (!includeDetails && !isRoot) {
			return null;
		}
		Object contributor = getContributor(path);
		return getContribution(contributor, includeDetails);
	}

	@SuppressWarnings("unchecked")
	private Object getContributor(String[] path) {
		Object contributor = this.registry;
		int pathOffset = 0;
		while (pathOffset < path.length) {
			if (!(contributor instanceof NamedContributors)) {
				return null;
			}
			contributor = ((NamedContributors<C>) contributor).getContributor(path[pathOffset]);
			pathOffset++;
		}
		return contributor;
	}

	@SuppressWarnings("unchecked")
	private T getContribution(Object contributor, boolean includeDetails) {
		if (contributor instanceof NamedContributors) {
			return getAggregateHealth((NamedContributors<C>) contributor, includeDetails);
		}
		return (contributor != null) ? getHealth((C) contributor, includeDetails) : null;
	}

	private T getAggregateHealth(NamedContributors<C> namedContributors, boolean includeDetails) {
		Map<String, T> contributions = new LinkedHashMap<>();
		for (NamedContributor<C> namedContributor : namedContributors) {
			String name = namedContributor.getName();
			T contribution = getContribution(namedContributor.getContributor(), includeDetails);
			contributions.put(name, contribution);
		}
		if (contributions.isEmpty()) {
			return null;
		}
		return aggregateContributions(contributions, this.settings.getStatusAggregator(), includeDetails);
	}

	protected abstract T getHealth(C contributor, boolean includeDetails);

	protected abstract T aggregateContributions(Map<String, T> contributions, StatusAggregator statusAggregator,
			boolean includeDetails);

	protected final CompositeHealth getCompositeHealth(Map<String, HealthComponent> components,
			StatusAggregator statusAggregator, boolean includeDetails) {
		Status status = statusAggregator.getAggregateStatus(
				components.values().stream().map(HealthComponent::getStatus).collect(Collectors.toSet()));
		Map<String, HealthComponent> includedComponents = includeDetails ? components : null;
		return new CompositeHealth(status, includedComponents);
	}

}
