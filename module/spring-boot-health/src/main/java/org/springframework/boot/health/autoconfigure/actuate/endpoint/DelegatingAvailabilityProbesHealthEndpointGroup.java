/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

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
 */
class DelegatingAvailabilityProbesHealthEndpointGroup implements HealthEndpointGroup {

	private final HealthEndpointGroup delegate;

	private final AdditionalHealthEndpointPath additionalPath;

	DelegatingAvailabilityProbesHealthEndpointGroup(HealthEndpointGroup delegate,
			AdditionalHealthEndpointPath additionalPath) {
		Assert.notNull(delegate, "'delegate' must not be null");
		this.delegate = delegate;
		this.additionalPath = additionalPath;
	}

	@Override
	public boolean isMember(String name) {
		return this.delegate.isMember(name);
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
	public AdditionalHealthEndpointPath getAdditionalPath() {
		return this.additionalPath;
	}

}
