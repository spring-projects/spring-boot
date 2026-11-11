/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.util.Assert;

/**
 * {@link HealthEndpointGroup} used to support availability probes that delegates to an
 * existing group.
 *
 * @author Madhura Bhave
 * @author Taejin Han
 */
class DelegatingAvailabilityProbesHealthEndpointGroup implements HealthEndpointGroup {

	private final HealthEndpointGroup delegate;

	private final @Nullable AdditionalHealthEndpointPath additionalPath;

	private final @Nullable Set<String> members;

	DelegatingAvailabilityProbesHealthEndpointGroup(HealthEndpointGroup delegate,
			@Nullable AdditionalHealthEndpointPath additionalPath, @Nullable Set<String> members) {
		Assert.notNull(delegate, "'delegate' must not be null");
		this.delegate = delegate;
		this.additionalPath = additionalPath;
		this.members = members;
	}

	@Override
	public boolean isMember(String name) {
		return (this.members != null) ? this.members.contains(name) : this.delegate.isMember(name);
	}

	@Override
	public boolean showComponents(SecurityContext securityContext) {
		return this.delegate.showComponents(securityContext);
	}

	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return this.delegate.showDetails(securityContext);
	}

	@Override
	public StatusAggregator getStatusAggregator() {
		return this.delegate.getStatusAggregator();
	}

	@Override
	public HttpCodeStatusMapper getHttpCodeStatusMapper() {
		return this.delegate.getHttpCodeStatusMapper();
	}

	@Override
	public @Nullable AdditionalHealthEndpointPath getAdditionalPath() {
		return (this.additionalPath != null) ? this.additionalPath : this.delegate.getAdditionalPath();
	}

}
