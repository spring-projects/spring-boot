/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.health;

import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;

/**
 * Test implementation of {@link HealthEndpointGroups}.
 *
 * @author Phillip Webb
 */
class TestHealthEndpointGroup implements HealthEndpointGroup {

	private final StatusAggregator statusAggregator = StatusAggregator.getDefault();

	private final HttpCodeStatusMapper httpCodeStatusMapper = HttpCodeStatusMapper.getDefault();

	private final Predicate<String> memberPredicate;

	private Boolean showComponents;

	private boolean showDetails = true;

	private AdditionalHealthEndpointPath additionalPath;

	TestHealthEndpointGroup() {
		this((name) -> true);
	}

	TestHealthEndpointGroup(Predicate<String> memberPredicate) {
		this.memberPredicate = memberPredicate;
	}

	@Override
	public boolean isMember(String name) {
		return this.memberPredicate.test(name);
	}

	@Override
	public boolean showComponents(SecurityContext securityContext) {
		return (this.showComponents != null) ? this.showComponents : this.showDetails;
	}

	void setShowComponents(Boolean showComponents) {
		this.showComponents = showComponents;
	}

	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return this.showDetails;
	}

	void setShowDetails(boolean includeDetails) {
		this.showDetails = includeDetails;
	}

	@Override
	public StatusAggregator getStatusAggregator() {
		return this.statusAggregator;
	}

	@Override
	public HttpCodeStatusMapper getHttpCodeStatusMapper() {
		return this.httpCodeStatusMapper;
	}

	@Override
	public AdditionalHealthEndpointPath getAdditionalPath() {
		return this.additionalPath;
	}

	void setAdditionalPath(AdditionalHealthEndpointPath additionalPath) {
		this.additionalPath = additionalPath;
	}

}
