/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembership;

/**
 * Auto-configured {@link HealthEndpointGroup}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class AutoConfiguredHealthEndpointGroup implements HealthEndpointGroup {

	private final HealthContributorMembership membership;

	private final StatusAggregator statusAggregator;

	private final HttpCodeStatusMapper httpCodeStatusMapper;

	private final @Nullable Show showComponents;

	private final Show showDetails;

	private final Collection<String> roles;

	private final @Nullable AdditionalHealthEndpointPath additionalPath;

	/**
	 * Create a new {@link AutoConfiguredHealthEndpointGroup} instance.
	 * @param membership used to test for group membership
	 * @param statusAggregator the status aggregator to use
	 * @param httpCodeStatusMapper the HTTP code status mapper to use
	 * @param showComponents the show components setting
	 * @param showDetails the show details setting
	 * @param roles the roles to match
	 * @param additionalPath the additional path to use for this group
	 */
	AutoConfiguredHealthEndpointGroup(HealthContributorMembership membership, StatusAggregator statusAggregator,
			HttpCodeStatusMapper httpCodeStatusMapper, @Nullable Show showComponents, Show showDetails,
			Collection<String> roles, @Nullable AdditionalHealthEndpointPath additionalPath) {
		this.membership = membership;
		this.statusAggregator = statusAggregator;
		this.httpCodeStatusMapper = httpCodeStatusMapper;
		this.showComponents = showComponents;
		this.showDetails = showDetails;
		this.roles = roles;
		this.additionalPath = additionalPath;
	}

	@Override
	public boolean isMember(String name) {
		return this.membership.isMember(name);
	}

	@Override
	public boolean showComponents(SecurityContext securityContext) {
		Show show = (this.showComponents != null) ? this.showComponents : this.showDetails;
		return show.isShown(securityContext, this.roles);
	}

	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return this.showDetails.isShown(securityContext, this.roles);
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

}
