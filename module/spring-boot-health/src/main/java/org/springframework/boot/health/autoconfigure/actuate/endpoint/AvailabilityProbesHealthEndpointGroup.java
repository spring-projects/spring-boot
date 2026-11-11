/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;

/**
 * {@link HealthEndpointGroup} used to support availability probes.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class AvailabilityProbesHealthEndpointGroup implements HealthEndpointGroup {

	private final Set<String> members;

	private final @Nullable AdditionalHealthEndpointPath additionalPath;

	AvailabilityProbesHealthEndpointGroup(@Nullable AdditionalHealthEndpointPath additionalPath, String... members) {
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
		return HttpCodeStatusMapper.getDefault();
	}

	@Override
	public @Nullable AdditionalHealthEndpointPath getAdditionalPath() {
		return this.additionalPath;
	}

}
