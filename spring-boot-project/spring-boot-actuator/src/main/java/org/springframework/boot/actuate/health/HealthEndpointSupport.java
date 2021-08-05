/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.util.Assert;

/**
 * Base class for health endpoints and health endpoint extensions.
 *
 * @param <C> the contributor type
 * @param <T> the contributed health component type
 * @author Phillip Webb
 * @author Scott Frederick
 */
abstract class HealthEndpointSupport<C, T> {

	static final Health DEFAULT_HEALTH = Health.up().build();

	private final ContributorRegistry<C> registry;

	private final HealthEndpointGroups groups;

	/**
	 * Create a new {@link HealthEndpointSupport} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 */
	HealthEndpointSupport(ContributorRegistry<C> registry, HealthEndpointGroups groups) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(groups, "Groups must not be null");
		this.registry = registry;
		this.groups = groups;
	}

	HealthResult<T> getHealth(ApiVersion apiVersion, SecurityContext securityContext, boolean showAll, String... path) {
		HealthEndpointGroup group = (path.length > 0) ? this.groups.get(path[0]) : null;
		if (group != null) {
			return getHealth(apiVersion, group, securityContext, showAll, path, 1);
		}
		return getHealth(apiVersion, this.groups.getPrimary(), securityContext, showAll, path, 0);
	}

	private HealthResult<T> getHealth(ApiVersion apiVersion, HealthEndpointGroup group, SecurityContext securityContext,
			boolean showAll, String[] path, int pathOffset) {
		boolean showComponents = showAll || group.showComponents(securityContext);
		boolean showDetails = showAll || group.showDetails(securityContext);
		boolean isSystemHealth = group == this.groups.getPrimary() && pathOffset == 0;
		boolean isRoot = path.length - pathOffset == 0;
		if (!showComponents && !isRoot) {
			return null;
		}
		Object contributor = getContributor(path, pathOffset);
		T health = getContribution(apiVersion, group, contributor, showComponents, showDetails,
				isSystemHealth ? this.groups.getNames() : null, false);
		return (health != null) ? new HealthResult<>(health, group) : null;
	}

	@SuppressWarnings("unchecked")
	private Object getContributor(String[] path, int pathOffset) {
		Object contributor = this.registry;
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
	private T getContribution(ApiVersion apiVersion, HealthEndpointGroup group, Object contributor,
			boolean showComponents, boolean showDetails, Set<String> groupNames, boolean isNested) {
		if (contributor instanceof NamedContributors) {
			return getAggregateHealth(apiVersion, group, (NamedContributors<C>) contributor, showComponents,
					showDetails, groupNames, isNested);
		}
		return (contributor != null) ? getHealth((C) contributor, showDetails) : null;
	}

	private T getAggregateHealth(ApiVersion apiVersion, HealthEndpointGroup group,
			NamedContributors<C> namedContributors, boolean showComponents, boolean showDetails, Set<String> groupNames,
			boolean isNested) {
		Map<String, T> contributions = new LinkedHashMap<>();
		for (NamedContributor<C> namedContributor : namedContributors) {
			String name = namedContributor.getName();
			C contributor = namedContributor.getContributor();
			if (group.isMember(name) || isNested) {
				T contribution = getContribution(apiVersion, group, contributor, showComponents, showDetails, null,
						true);
				if (contribution != null) {
					contributions.put(name, contribution);
				}
			}
		}
		if (contributions.isEmpty()) {
			return null;
		}
		return aggregateContributions(apiVersion, contributions, group.getStatusAggregator(), showComponents,
				groupNames);
	}

	protected abstract T getHealth(C contributor, boolean includeDetails);

	protected abstract T aggregateContributions(ApiVersion apiVersion, Map<String, T> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames);

	protected final CompositeHealth getCompositeHealth(ApiVersion apiVersion, Map<String, HealthComponent> components,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames) {
		Status status = statusAggregator
				.getAggregateStatus(components.values().stream().map(this::getStatus).collect(Collectors.toSet()));
		Map<String, HealthComponent> instances = showComponents ? components : null;
		if (groupNames != null) {
			return new SystemHealth(apiVersion, status, instances, groupNames);
		}
		return new CompositeHealth(apiVersion, status, instances);
	}

	private Status getStatus(HealthComponent component) {
		return (component != null) ? component.getStatus() : Status.UNKNOWN;
	}

	/**
	 * A health result containing health and the group that created it.
	 *
	 * @param <T> the contributed health component
	 */
	static class HealthResult<T> {

		private final T health;

		private final HealthEndpointGroup group;

		HealthResult(T health, HealthEndpointGroup group) {
			this.health = health;
			this.group = group;
		}

		T getHealth() {
			return this.health;
		}

		HealthEndpointGroup getGroup() {
			return this.group;
		}

	}

}
