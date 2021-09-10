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

	AvailabilityProbesHealthEndpointGroup(AdditionalHealthEndpointPath additionalPath, String... members) {
		this.members = new HashSet<>(Arrays.asList(members));
		this.additionalPath = additionalPath;
	}

	@Override
	public boolean isMember(String name) {
		return this.members.contains(name);
	}

	@Override
	public boolean showComponents(SecurityContext securityContext) {
		return false;
	}

	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return false;
	}

	@Override
	public StatusAggregator getStatusAggregator() {
		return StatusAggregator.getDefault();
	}

	@Override
	public HttpCodeStatusMapper getHttpCodeStatusMapper() {
		return HttpCodeStatusMapper.DEFAULT;
	}

	@Override
	public AdditionalHealthEndpointPath getAdditionalPath() {
		return this.additionalPath;
	}

}
