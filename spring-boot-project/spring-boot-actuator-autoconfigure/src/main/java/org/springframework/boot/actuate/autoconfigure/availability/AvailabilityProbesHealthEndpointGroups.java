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

	AvailabilityProbesHealthEndpointGroups(HealthEndpointGroups groups, boolean addAdditionalPaths) {
		Assert.notNull(groups, "Groups must not be null");
		this.groups = groups;
		this.probeGroups = createProbeGroups(addAdditionalPaths);
		Set<String> names = new LinkedHashSet<>(groups.getNames());
		names.addAll(this.probeGroups.keySet());
		this.names = Collections.unmodifiableSet(names);
	}

	private Map<String, HealthEndpointGroup> createProbeGroups(boolean addAdditionalPaths) {
		Map<String, HealthEndpointGroup> probeGroups = new LinkedHashMap<>();
		probeGroups.put("liveness", createProbeGroup(addAdditionalPaths, "/livez", "livenessState"));
		probeGroups.put("readiness", createProbeGroup(addAdditionalPaths, "/readyz", "readinessState"));
		return Collections.unmodifiableMap(probeGroups);
	}

	private AvailabilityProbesHealthEndpointGroup createProbeGroup(boolean addAdditionalPath, String path,
			String members) {
		AdditionalHealthEndpointPath additionalPath = (!addAdditionalPath) ? null
				: AdditionalHealthEndpointPath.of(WebServerNamespace.SERVER, path);
		return new AvailabilityProbesHealthEndpointGroup(additionalPath, members);
	}

	@Override
	public HealthEndpointGroup getPrimary() {
		return this.groups.getPrimary();
	}

	@Override
	public Set<String> getNames() {
		return this.names;
	}

	@Override
	public HealthEndpointGroup get(String name) {
		HealthEndpointGroup group = this.groups.get(name);
		if (group == null) {
			group = this.probeGroups.get(name);
		}
		return group;
	}

	static boolean containsAllProbeGroups(HealthEndpointGroups groups) {
		Set<String> names = groups.getNames();
		return names.contains("liveness") && names.contains("readiness");
	}

}
