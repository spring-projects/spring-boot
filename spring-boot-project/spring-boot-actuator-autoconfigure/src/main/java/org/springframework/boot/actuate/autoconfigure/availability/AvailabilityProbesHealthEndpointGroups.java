/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.util.Assert;

/**
 * {@link HealthEndpointGroups} decorator to support availability probes.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class AvailabilityProbesHealthEndpointGroups implements HealthEndpointGroups {

	private static final Map<String, AvailabilityProbesHealthEndpointGroup> GROUPS;
	static {
		Map<String, AvailabilityProbesHealthEndpointGroup> groups = new LinkedHashMap<>();
		groups.put("liveness", new AvailabilityProbesHealthEndpointGroup("livenessState"));
		groups.put("readiness", new AvailabilityProbesHealthEndpointGroup("readinessState"));
		GROUPS = Collections.unmodifiableMap(groups);
	}

	private final HealthEndpointGroups groups;

	private final Set<String> names;

	AvailabilityProbesHealthEndpointGroups(HealthEndpointGroups groups) {
		Assert.notNull(groups, "Groups must not be null");
		this.groups = groups;
		Set<String> names = new LinkedHashSet<>(groups.getNames());
		names.addAll(GROUPS.keySet());
		this.names = Collections.unmodifiableSet(names);
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
			group = GROUPS.get(name);
		}
		return group;
	}

	static boolean containsAllProbeGroups(HealthEndpointGroups groups) {
		return groups.getNames().containsAll(GROUPS.keySet());
	}

}
