/*
 * Copyright 2012-2019 the original author or authors.
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

import java.security.Principal;
import java.util.Collection;
import java.util.function.Predicate;

import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.Show;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Auto-configured {@link HealthEndpointGroup} backed by {@link HealthProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class AutoConfiguredHealthEndpointGroup implements HealthEndpointGroup {

	private final Predicate<String> members;

	private final StatusAggregator statusAggregator;

	private final HttpCodeStatusMapper httpCodeStatusMapper;

	private final Show showComponents;

	private final Show showDetails;

	private final Collection<String> roles;

	/**
	 * Create a new {@link AutoConfiguredHealthEndpointGroup} instance.
	 * @param members a predicate used to test for group membership
	 * @param statusAggregator the status aggregator to use
	 * @param httpCodeStatusMapper the HTTP code status mapper to use
	 * @param showComponents the show components setting
	 * @param showDetails the show details setting
	 * @param roles the roles to match
	 */
	AutoConfiguredHealthEndpointGroup(Predicate<String> members, StatusAggregator statusAggregator,
			HttpCodeStatusMapper httpCodeStatusMapper, Show showComponents, Show showDetails,
			Collection<String> roles) {
		this.members = members;
		this.statusAggregator = statusAggregator;
		this.httpCodeStatusMapper = httpCodeStatusMapper;
		this.showComponents = showComponents;
		this.showDetails = showDetails;
		this.roles = roles;
	}

	@Override
	public boolean isMember(String name) {
		return this.members.test(name);
	}

	@Override
	public boolean showComponents(SecurityContext securityContext) {
		if (this.showComponents == null) {
			return showDetails(securityContext);
		}
		return getShowResult(securityContext, this.showComponents);
	}

	@Override
	public boolean showDetails(SecurityContext securityContext) {
		return getShowResult(securityContext, this.showDetails);
	}

	private boolean getShowResult(SecurityContext securityContext, Show show) {
		switch (show) {
		case NEVER:
			return false;
		case ALWAYS:
			return true;
		case WHEN_AUTHORIZED:
			return isAuthorized(securityContext);
		}
		throw new IllegalStateException("Unsupported 'show' value " + show);
	}

	private boolean isAuthorized(SecurityContext securityContext) {
		Principal principal = securityContext.getPrincipal();
		if (principal == null) {
			return false;
		}
		if (CollectionUtils.isEmpty(this.roles)) {
			return true;
		}
		boolean checkAuthorities = isSpringSecurityAuthentication(principal);
		for (String role : this.roles) {
			if (securityContext.isUserInRole(role)) {
				return true;
			}
			if (checkAuthorities) {
				Authentication authentication = (Authentication) principal;
				for (GrantedAuthority authority : authentication.getAuthorities()) {
					String name = authority.getAuthority();
					if (role.equals(name)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isSpringSecurityAuthentication(Principal principal) {
		return ClassUtils.isPresent("org.springframework.security.core.Authentication", null)
				&& (principal instanceof Authentication);
	}

	@Override
	public StatusAggregator getStatusAggregator() {
		return this.statusAggregator;
	}

	@Override
	public HttpCodeStatusMapper getHttpCodeStatusMapper() {
		return this.httpCodeStatusMapper;
	}

}
