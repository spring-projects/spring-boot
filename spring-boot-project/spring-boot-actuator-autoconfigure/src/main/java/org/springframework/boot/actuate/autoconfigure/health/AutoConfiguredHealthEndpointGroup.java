/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collection;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

/**
 * Auto-configured {@link HealthEndpointGroup} backed by {@link HealthProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class AutoConfiguredHealthEndpointGroup implements HealthEndpointGroup {

	private final Predicate<String> members;

	private final StatusAggregator statusAggregator;

	private final HttpCodeStatusMapper httpCodeStatusMapper;

	private final Show showComponents;

	private final Show showDetails;

	private final Collection<String> roles;

	private final AdditionalHealthEndpointPath additionalPath;

	/**
	 * Create a new {@link AutoConfiguredHealthEndpointGroup} instance.
	 * @param members a predicate used to test for group membership
	 * @param statusAggregator the status aggregator to use
	 * @param httpCodeStatusMapper the HTTP code status mapper to use
	 * @param showComponents the show components setting
	 * @param showDetails the show details setting
	 * @param roles the roles to match
	 * @param additionalPath the additional path to use for this group
	 */
	AutoConfiguredHealthEndpointGroup(Predicate<String> members, StatusAggregator statusAggregator,
			HttpCodeStatusMapper httpCodeStatusMapper, Show showComponents, Show showDetails, Collection<String> roles,
			AdditionalHealthEndpointPath additionalPath) {
		this.members = members;
		this.statusAggregator = statusAggregator;
		this.httpCodeStatusMapper = httpCodeStatusMapper;
		this.showComponents = showComponents;
		this.showDetails = showDetails;
		this.roles = roles;
		this.additionalPath = additionalPath;
	}

	/**
	 * Checks if a given name is a member of the AutoConfiguredHealthEndpointGroup.
	 * @param name the name to check
	 * @return true if the name is a member of the AutoConfiguredHealthEndpointGroup,
	 * false otherwise
	 */
	@Override
	public boolean isMember(String name) {
		return this.members.test(name);
	}

	/**
	 * Determines whether to show the components based on the provided security context.
	 * @param securityContext the security context to check
	 * @return true if the components should be shown, false otherwise
	 */
	@Override
	public boolean showComponents(SecurityContext securityContext) {
		Show show = (this.showComponents != null) ? this.showComponents : this.showDetails;
		return show.isShown(securityContext, this.roles);
	}

	/**
	 * Determines whether to show details for the health endpoint group.
	 * @param securityContext the security context to check for authorization
	 * @return true if details should be shown, false otherwise
	 */
	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return this.showDetails.isShown(securityContext, this.roles);
	}

	/**
	 * Returns the status aggregator associated with this
	 * AutoConfiguredHealthEndpointGroup.
	 * @return the status aggregator
	 */
	@Override
	public StatusAggregator getStatusAggregator() {
		return this.statusAggregator;
	}

	/**
	 * Returns the HttpCodeStatusMapper used by this AutoConfiguredHealthEndpointGroup.
	 * @return the HttpCodeStatusMapper used by this AutoConfiguredHealthEndpointGroup
	 */
	@Override
	public HttpCodeStatusMapper getHttpCodeStatusMapper() {
		return this.httpCodeStatusMapper;
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
