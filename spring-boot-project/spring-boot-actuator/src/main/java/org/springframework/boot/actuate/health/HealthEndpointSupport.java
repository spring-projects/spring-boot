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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.health.contributor.Status;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for health endpoints and health endpoint extensions.
 *
 * @param <H> the health type
 * @param <D> the descriptor type
 * @author Phillip Webb
 * @author Scott Frederick
 */
abstract class HealthEndpointSupport<H, D> {

	static final String[] EMPTY_PATH = {};

	private static final Log logger = LogFactory.getLog(HealthEndpointSupport.class);

	private final Contributor<H, D> rootContributor;

	private final HealthEndpointGroups groups;

	private final Duration slowContributorLoggingThreshold;

	/**
	 * Create a new {@link HealthEndpointSupport} instance.
	 * @param rootContributor the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowContributorLoggingThreshold duration after which slow health contributor
	 * logging should occur
	 */
	HealthEndpointSupport(Contributor<H, D> rootContributor, HealthEndpointGroups groups,
			Duration slowContributorLoggingThreshold) {
		Assert.notNull(rootContributor, "'rootContributor' must not be null");
		Assert.notNull(groups, "'groups' must not be null");
		this.rootContributor = rootContributor;
		this.groups = groups;
		this.slowContributorLoggingThreshold = slowContributorLoggingThreshold;
	}

	Result<D> getResult(ApiVersion apiVersion, WebServerNamespace serverNamespace, SecurityContext securityContext,
			boolean showAll, String... path) {
		HealthEndpointGroup group = (path.length > 0) ? getGroup(serverNamespace, path) : null;
		if (group != null) {
			return getResult(apiVersion, group, securityContext, showAll, path, 1);
		}
		return getResult(apiVersion, this.groups.getPrimary(), securityContext, showAll, path, 0);
	}

	private HealthEndpointGroup getGroup(WebServerNamespace serverNamespace, String... path) {
		if (this.groups.get(path[0]) != null) {
			return this.groups.get(path[0]);
		}
		if (serverNamespace != null) {
			return this.groups.get(AdditionalHealthEndpointPath.of(serverNamespace, path[0]));
		}
		return null;
	}

	private Result<D> getResult(ApiVersion apiVersion, HealthEndpointGroup group, SecurityContext securityContext,
			boolean showAll, String[] path, int pathOffset) {
		boolean showComponents = showAll || group.showComponents(securityContext);
		boolean showDetails = showAll || group.showDetails(securityContext);
		boolean isSystemHealth = group == this.groups.getPrimary() && pathOffset == 0;
		boolean isRoot = path.length - pathOffset == 0;
		if (!showComponents && !isRoot) {
			return null;
		}
		Contributor<H, D> contributor = getContributor(path, pathOffset);
		if (contributor == null) {
			return null;
		}
		String name = getName(path, pathOffset);
		Set<String> groupNames = (!isSystemHealth) ? null : new TreeSet<>(this.groups.getNames());
		D descriptor = getDescriptor(apiVersion, group, name, contributor, showComponents, showDetails, groupNames);
		return (descriptor != null) ? new Result<>(descriptor, group) : null;
	}

	private Contributor<H, D> getContributor(String[] path, int pathOffset) {
		Contributor<H, D> contributor = this.rootContributor;
		while (pathOffset < path.length) {
			if (!contributor.isComposite()) {
				return null;
			}
			contributor = contributor.getChild(path[pathOffset]);
			pathOffset++;
		}
		return contributor;
	}

	private String getName(String[] path, int pathOffset) {
		StringBuilder name = new StringBuilder();
		while (pathOffset < path.length) {
			name.append((!name.isEmpty()) ? "/" : "");
			name.append(path[pathOffset]);
			pathOffset++;
		}
		return name.toString();
	}

	private D getDescriptor(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			Contributor<H, D> contributor, boolean showComponents, boolean showDetails, Set<String> groupNames) {
		if (contributor.isComposite()) {
			return getAggregateDescriptor(apiVersion, group, name, contributor, showComponents, showDetails,
					groupNames);
		}
		if (name.isEmpty() || group.isMember(name)) {
			return getDescriptorAndLogIfSlow(contributor, name, showDetails);
		}
		return null;
	}

	private D getAggregateDescriptor(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			Contributor<H, D> contributor, boolean showComponents, boolean showDetails, Set<String> groupNames) {
		String prefix = (StringUtils.hasText(name)) ? name + "/" : "";
		Map<String, D> descriptors = new LinkedHashMap<>();
		for (Contributor.Child<H, D> child : contributor) {
			String childName = child.name();
			D descriptor = getDescriptor(apiVersion, group, prefix + childName, child.contributor(), showComponents,
					showDetails, null);
			if (descriptor != null) {
				descriptors.put(childName, descriptor);
			}
		}
		if (descriptors.isEmpty()) {
			return null;
		}
		return aggregateDescriptors(apiVersion, descriptors, group.getStatusAggregator(), showComponents, groupNames);
	}

	private D getDescriptorAndLogIfSlow(Contributor<H, D> contributor, String name, boolean showDetails) {
		Instant start = Instant.now();
		try {
			return contributor.getDescriptor(showDetails);
		}
		finally {
			if (logger.isWarnEnabled() && this.slowContributorLoggingThreshold != null) {
				Duration duration = Duration.between(start, Instant.now());
				if (duration.compareTo(this.slowContributorLoggingThreshold) > 0) {
					logger.warn(LogMessage.format("Health contributor %s took %s to respond",
							contributor.getIdentifier(name), DurationStyle.SIMPLE.print(duration)));
				}
			}
		}
	}

	abstract D aggregateDescriptors(ApiVersion apiVersion, Map<String, D> descriptors,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames);

	final CompositeHealthDescriptor getCompositeDescriptor(ApiVersion apiVersion,
			Map<String, HealthDescriptor> descriptors, StatusAggregator statusAggregator, boolean showComponents,
			Set<String> groupNames) {
		Status status = statusAggregator
			.getAggregateStatus(descriptors.values().stream().map(this::getStatus).collect(Collectors.toSet()));
		descriptors = (!showComponents) ? null : descriptors;
		return (groupNames != null) ? new SystemHealthDescriptor(apiVersion, status, descriptors, groupNames)
				: new CompositeHealthDescriptor(apiVersion, status, descriptors);
	}

	private Status getStatus(HealthDescriptor component) {
		return (component != null) ? component.getStatus() : Status.UNKNOWN;
	}

	/**
	 * A health result containing descriptor and the group that created it.
	 *
	 * @param descriptor the health descriptor
	 * @param group the group used to create the health
	 * @param <D> the details type
	 */
	record Result<D>(D descriptor, HealthEndpointGroup group) {

	}

}
