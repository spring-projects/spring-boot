/*
 * Copyright 2012-2023 the original author or authors.
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.log.LogMessage;
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

	private static final Log logger = LogFactory.getLog(HealthEndpointSupport.class);

	static final Health DEFAULT_HEALTH = Health.up().build();

	private final ContributorRegistry<C> registry;

	private final HealthEndpointGroups groups;

	private final Duration slowIndicatorLoggingThreshold;

	/**
	 * Create a new {@link HealthEndpointSupport} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 */
	HealthEndpointSupport(ContributorRegistry<C> registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(groups, "Groups must not be null");
		this.registry = registry;
		this.groups = groups;
		this.slowIndicatorLoggingThreshold = slowIndicatorLoggingThreshold;
	}

	/**
	 * Retrieves the health result for the specified API version, web server namespace,
	 * security context, showAll flag, and path.
	 * @param apiVersion The API version to retrieve the health result for.
	 * @param serverNamespace The web server namespace to retrieve the health result for.
	 * @param securityContext The security context to use for retrieving the health
	 * result.
	 * @param showAll A flag indicating whether to show all health information.
	 * @param path The path to retrieve the health result for.
	 * @return The health result for the specified parameters.
	 */
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

	/**
	 * Retrieves the HealthEndpointGroup based on the provided serverNamespace and path.
	 * @param serverNamespace The WebServerNamespace to retrieve the HealthEndpointGroup
	 * from.
	 * @param path The path to retrieve the HealthEndpointGroup from.
	 * @return The HealthEndpointGroup associated with the provided serverNamespace and
	 * path, or null if not found.
	 */
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

	/**
	 * Retrieves the health result for a specific API version, health endpoint group,
	 * security context, show all flag, path, and path offset.
	 * @param apiVersion The API version.
	 * @param group The health endpoint group.
	 * @param securityContext The security context.
	 * @param showAll The flag indicating whether to show all components.
	 * @param path The path.
	 * @param pathOffset The path offset.
	 * @param <T> The type of health result.
	 * @return The health result.
	 */
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

	/**
	 * Retrieves the contributor object from the registry based on the given path.
	 * @param path the array of path elements
	 * @param pathOffset the starting index in the path array
	 * @return the contributor object if found, null otherwise
	 */
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

	/**
	 * Returns the name of the file or directory specified by the given path array
	 * starting from the specified offset.
	 * @param path The array of path elements.
	 * @param pathOffset The starting offset in the path array.
	 * @return The name of the file or directory.
	 */
	private String getName(String[] path, int pathOffset) {
		StringBuilder name = new StringBuilder();
		while (pathOffset < path.length) {
			name.append((!name.isEmpty()) ? "/" : "");
			name.append(path[pathOffset]);
			pathOffset++;
		}
		return name.toString();
	}

	/**
	 * Retrieves the contribution for a specific health endpoint.
	 * @param apiVersion the API version
	 * @param group the health endpoint group
	 * @param name the name of the health endpoint
	 * @param contributor the contributor object
	 * @param showComponents flag indicating whether to show components
	 * @param showDetails flag indicating whether to show details
	 * @param groupNames the set of group names
	 * @return the contribution for the specified health endpoint, or null if not found
	 */
	@SuppressWarnings("unchecked")
	private T getContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name, Object contributor,
			boolean showComponents, boolean showDetails, Set<String> groupNames) {
		if (contributor instanceof NamedContributors) {
			return getAggregateContribution(apiVersion, group, name, (NamedContributors<C>) contributor, showComponents,
					showDetails, groupNames);
		}
		if (contributor != null && (name.isEmpty() || group.isMember(name))) {
			return getLoggedHealth((C) contributor, name, showDetails);
		}
		return null;
	}

	/**
	 * Retrieves the aggregate contribution for a specific health endpoint group and name.
	 * @param apiVersion The version of the API.
	 * @param group The health endpoint group.
	 * @param name The name of the health endpoint.
	 * @param namedContributors The named contributors.
	 * @param showComponents Flag indicating whether to show components.
	 * @param showDetails Flag indicating whether to show details.
	 * @param groupNames The set of group names.
	 * @return The aggregate contribution.
	 */
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

	/**
	 * Retrieves the logged health status of a contributor.
	 * @param contributor the contributor to retrieve the health status from
	 * @param name the name of the contributor (optional)
	 * @param showDetails flag indicating whether to show detailed health information
	 * @return the logged health status of the contributor
	 */
	private T getLoggedHealth(C contributor, String name, boolean showDetails) {
		Instant start = Instant.now();
		try {
			return getHealth(contributor, showDetails);
		}
		finally {
			if (logger.isWarnEnabled() && this.slowIndicatorLoggingThreshold != null) {
				Duration duration = Duration.between(start, Instant.now());
				if (duration.compareTo(this.slowIndicatorLoggingThreshold) > 0) {
					String contributorClassName = contributor.getClass().getName();
					Object contributorIdentifier = (!StringUtils.hasLength(name)) ? contributorClassName
							: contributorClassName + " (" + name + ")";
					logger.warn(LogMessage.format("Health contributor %s took %s to respond", contributorIdentifier,
							DurationStyle.SIMPLE.print(duration)));
				}
			}
		}
	}

	/**
	 * Retrieves the health information for the specified contributor.
	 * @param contributor the contributor for which to retrieve the health information
	 * @param includeDetails flag indicating whether to include detailed health
	 * information
	 * @return the health information for the specified contributor
	 */
	protected abstract T getHealth(C contributor, boolean includeDetails);

	/**
	 * Aggregates the contributions from different components based on the provided API
	 * version.
	 * @param apiVersion the API version to use for aggregation
	 * @param contributions a map of component names to their respective contributions
	 * @param statusAggregator the status aggregator to use for aggregating the status of
	 * components
	 * @param showComponents a flag indicating whether to include component details in the
	 * aggregation
	 * @param groupNames a set of group names to filter the contributions by
	 * @return the aggregated contributions based on the provided parameters
	 */
	protected abstract T aggregateContributions(ApiVersion apiVersion, Map<String, T> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames);

	/**
	 * Returns a CompositeHealth object based on the provided parameters.
	 * @param apiVersion the version of the API
	 * @param components a map of health components
	 * @param statusAggregator the status aggregator used to calculate the aggregate
	 * status
	 * @param showComponents a flag indicating whether to include individual health
	 * components in the result
	 * @param groupNames a set of group names to filter the health components by
	 * @return a CompositeHealth object representing the composite health status
	 */
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

	/**
	 * Returns the status of the given HealthComponent.
	 * @param component the HealthComponent to get the status from
	 * @return the status of the HealthComponent, or Status.UNKNOWN if the component is
	 * null
	 */
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

		/**
		 * Constructs a new HealthResult object with the specified health and group.
		 * @param health the health status of the result
		 * @param group the group to which the health result belongs
		 */
		HealthResult(T health, HealthEndpointGroup group) {
			this.health = health;
			this.group = group;
		}

		/**
		 * Returns the health value of the HealthResult object.
		 * @return the health value of the HealthResult object
		 */
		T getHealth() {
			return this.health;
		}

		/**
		 * Returns the HealthEndpointGroup associated with this HealthResult.
		 * @return the HealthEndpointGroup associated with this HealthResult
		 */
		HealthEndpointGroup getGroup() {
			return this.group;
		}

	}

}
