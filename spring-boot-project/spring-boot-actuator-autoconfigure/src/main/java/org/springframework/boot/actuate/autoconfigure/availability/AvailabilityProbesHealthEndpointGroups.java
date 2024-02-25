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

package org.springframework.boot.actuate.autoconfigure.availability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.util.Assert;

/**
 * {@link HealthEndpointGroups} decorator to support availability probes.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class AvailabilityProbesHealthEndpointGroups implements HealthEndpointGroups {

	private final HealthEndpointGroups groups;

	private final Map<String, HealthEndpointGroup> probeGroups;

	private final Set<String> names;

	private static final String LIVENESS = "liveness";

	private static final String READINESS = "readiness";

	/**
	 * Constructs an instance of AvailabilityProbesHealthEndpointGroups with the specified
	 * HealthEndpointGroups and addAdditionalPaths flag.
	 * @param groups the HealthEndpointGroups to be used
	 * @param addAdditionalPaths flag indicating whether additional paths should be added
	 * to the probe groups
	 * @throws IllegalArgumentException if groups is null
	 */
	AvailabilityProbesHealthEndpointGroups(HealthEndpointGroups groups, boolean addAdditionalPaths) {
		Assert.notNull(groups, "Groups must not be null");
		this.groups = groups;
		this.probeGroups = createProbeGroups(addAdditionalPaths);
		Set<String> names = new LinkedHashSet<>(groups.getNames());
		names.addAll(this.probeGroups.keySet());
		this.names = Collections.unmodifiableSet(names);
	}

	/**
	 * Creates probe groups for liveness and readiness endpoints.
	 * @param addAdditionalPaths a boolean indicating whether additional paths should be
	 * added to the probe groups
	 * @return a map of probe groups, with liveness and readiness as keys and
	 * corresponding HealthEndpointGroup objects as values
	 */
	private Map<String, HealthEndpointGroup> createProbeGroups(boolean addAdditionalPaths) {
		Map<String, HealthEndpointGroup> probeGroups = new LinkedHashMap<>();
		probeGroups.put(LIVENESS, getOrCreateProbeGroup(addAdditionalPaths, LIVENESS, "/livez", "livenessState"));
		probeGroups.put(READINESS, getOrCreateProbeGroup(addAdditionalPaths, READINESS, "/readyz", "readinessState"));
		return Collections.unmodifiableMap(probeGroups);
	}

	/**
	 * Retrieves or creates a HealthEndpointGroup for availability probes.
	 * @param addAdditionalPath flag indicating whether to add an additional path
	 * @param name the name of the group
	 * @param path the path for the group
	 * @param members the members of the group
	 * @return the HealthEndpointGroup for availability probes
	 */
	private HealthEndpointGroup getOrCreateProbeGroup(boolean addAdditionalPath, String name, String path,
			String members) {
		HealthEndpointGroup group = this.groups.get(name);
		if (group != null) {
			return determineAdditionalPathForExistingGroup(addAdditionalPath, path, group);
		}
		AdditionalHealthEndpointPath additionalPath = (!addAdditionalPath) ? null
				: AdditionalHealthEndpointPath.of(WebServerNamespace.SERVER, path);
		return new AvailabilityProbesHealthEndpointGroup(additionalPath, members);
	}

	/**
	 * Determines the additional path for an existing health endpoint group.
	 * @param addAdditionalPath true if an additional path should be added, false
	 * otherwise
	 * @param path the path for the additional health endpoint
	 * @param group the existing health endpoint group
	 * @return the health endpoint group with the additional path added, or the original
	 * group if no additional path is added
	 */
	private HealthEndpointGroup determineAdditionalPathForExistingGroup(boolean addAdditionalPath, String path,
			HealthEndpointGroup group) {
		if (addAdditionalPath && group.getAdditionalPath() == null) {
			AdditionalHealthEndpointPath additionalPath = AdditionalHealthEndpointPath.of(WebServerNamespace.SERVER,
					path);
			return new DelegatingAvailabilityProbesHealthEndpointGroup(group, additionalPath);
		}
		return group;
	}

	/**
	 * Returns the primary HealthEndpointGroup.
	 * @return the primary HealthEndpointGroup
	 */
	@Override
	public HealthEndpointGroup getPrimary() {
		return this.groups.getPrimary();
	}

	/**
	 * Returns a set of names.
	 * @return a set of names
	 */
	@Override
	public Set<String> getNames() {
		return this.names;
	}

	/**
	 * Retrieves the HealthEndpointGroup with the specified name.
	 * @param name the name of the HealthEndpointGroup to retrieve
	 * @return the HealthEndpointGroup with the specified name, or null if not found
	 */
	@Override
	public HealthEndpointGroup get(String name) {
		HealthEndpointGroup group = this.groups.get(name);
		if (group == null || isProbeGroup(name)) {
			group = this.probeGroups.get(name);
		}
		return group;
	}

	/**
	 * Checks if the given name is a probe group.
	 * @param name the name to be checked
	 * @return true if the name is a probe group (LIVENESS or READINESS), false otherwise
	 */
	private boolean isProbeGroup(String name) {
		return name.equals(LIVENESS) || name.equals(READINESS);
	}

}
