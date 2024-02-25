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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

/**
 * {@link HealthEndpointGroup} used to support availability probes.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class AvailabilityProbesHealthEndpointGroup implements HealthEndpointGroup {

	private final Set<String> members;

	private final AdditionalHealthEndpointPath additionalPath;

	/**
	 * Constructs a new AvailabilityProbesHealthEndpointGroup with the specified
	 * additionalPath and members.
	 * @param additionalPath the additional health endpoint path to be added
	 * @param members the members of the health endpoint group
	 */
	AvailabilityProbesHealthEndpointGroup(AdditionalHealthEndpointPath additionalPath, String... members) {
		this.members = new HashSet<>(Arrays.asList(members));
		this.additionalPath = additionalPath;
	}

	/**
	 * Checks if a given name is a member of the AvailabilityProbesHealthEndpointGroup.
	 * @param name the name to check
	 * @return true if the name is a member, false otherwise
	 */
	@Override
	public boolean isMember(String name) {
		return this.members.contains(name);
	}

	/**
	 * Determines whether to show the components of the
	 * AvailabilityProbesHealthEndpointGroup.
	 * @param securityContext the security context to check for authorization
	 * @return true if the components should be shown, false otherwise
	 */
	@Override
	public boolean showComponents(SecurityContext securityContext) {
		return false;
	}

	/**
	 * Determines whether to show details of the availability probes health endpoint
	 * group.
	 * @param securityContext the security context used to determine access rights
	 * @return true if the details should be shown, false otherwise
	 */
	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return false;
	}

	/**
	 * Returns the default status aggregator for the
	 * AvailabilityProbesHealthEndpointGroup.
	 * @return the default status aggregator
	 */
	@Override
	public StatusAggregator getStatusAggregator() {
		return StatusAggregator.getDefault();
	}

	/**
	 * Returns the HTTP code status mapper for this AvailabilityProbesHealthEndpointGroup.
	 * @return the HTTP code status mapper
	 */
	@Override
	public HttpCodeStatusMapper getHttpCodeStatusMapper() {
		return HttpCodeStatusMapper.DEFAULT;
	}

	/**
	 * Returns the additional health endpoint path.
	 * @return the additional health endpoint path
	 */
	@Override
	public AdditionalHealthEndpointPath getAdditionalPath() {
		return this.additionalPath;
	}

}
