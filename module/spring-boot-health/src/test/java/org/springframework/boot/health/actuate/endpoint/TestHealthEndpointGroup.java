/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.SecurityContext;

/**
 * Test implementation of {@link HealthEndpointGroups}.
 *
 * @author Phillip Webb
 */
class TestHealthEndpointGroup implements HealthEndpointGroup {

	private final StatusAggregator statusAggregator = StatusAggregator.getDefault();

	private final HttpCodeStatusMapper httpCodeStatusMapper = HttpCodeStatusMapper.getDefault();

	private final Predicate<String> memberPredicate;

	private @Nullable Boolean showComponents;

	private boolean showDetails = true;

	private @Nullable AdditionalHealthEndpointPath additionalPath;

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
	public @Nullable AdditionalHealthEndpointPath getAdditionalPath() {
		return this.additionalPath;
	}

	void setAdditionalPath(AdditionalHealthEndpointPath additionalPath) {
		this.additionalPath = additionalPath;
	}

}
