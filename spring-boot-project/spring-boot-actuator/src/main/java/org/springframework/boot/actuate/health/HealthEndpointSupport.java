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
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	HealthResult<T> getHealth(ApiVersion apiVersion, WebServerNamespace serverNamespace,
			SecurityContext securityContext, boolean showAll, String... path) {
		if (path.length > 0) {
			HealthEndpointGroup group = getHealthGroup(serverNamespace, path);
			if (group != null) {
				return getHealth(apiVersion, group, securityContext, showAll, path, 1);
			}
		}
		return getHealth(apiVersion, this.groups.getPrimary(), securityContext, showAll, path, 0);
	}

	private HealthEndpointGroup getHealthGroup(WebServerNamespace serverNamespace, String... path) {
		if (this.groups.get(path[0]) != null) {
			return this.groups.get(path[0]);
		}
		if (serverNamespace != null) {
			AdditionalHealthEndpointPath additionalPath = AdditionalHealthEndpointPath.of(serverNamespace, path[0]);
			return this.groups.get(additionalPath);
		}
		return null;
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
		if (contributor == null) {
			return null;
		}
		String name = getName(path, pathOffset);
		Set<String> groupNames = isSystemHealth ? this.groups.getNames() : null;
		T health = getContribution(apiVersion, group, name, contributor, showComponents, showDetails, groupNames);
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

	private String getName(String[] path, int pathOffset) {
		StringBuilder name = new StringBuilder();
		while (pathOffset < path.length) {
			name.append((name.length() != 0) ? "/" : "");
			name.append(path[pathOffset]);
			pathOffset++;
		}
		return name.toString();
	}

	@SuppressWarnings("unchecked")
	private T getContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name, Object contributor,
			boolean showComponents, boolean showDetails, Set<String> groupNames) {
		if (contributor instanceof NamedContributors) {
			return getAggregateContribution(apiVersion, group, name, (NamedContributors<C>) contributor, showComponents,
					showDetails, groupNames);
		}
		if (contributor != null && (name.isEmpty() || group.isMember(name))) {
			return getHealth((C) contributor, showDetails);
		}
		return null;
	}

	private T getAggregateContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			NamedContributors<C> namedContributors, boolean showComponents, boolean showDetails,
			Set<String> groupNames) {
		String prefix = (StringUtils.hasText(name)) ? name + "/" : "";
		Map<String, T> contributions = new LinkedHashMap<>();
		for (NamedContributor<C> child : namedContributors) {
			T contribution = getContribution(apiVersion, group, prefix + child.getName(), child.getContributor(),
					showComponents, showDetails, null);
			if (contribution != null) {
				contributions.put(child.getName(), contribution);
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
