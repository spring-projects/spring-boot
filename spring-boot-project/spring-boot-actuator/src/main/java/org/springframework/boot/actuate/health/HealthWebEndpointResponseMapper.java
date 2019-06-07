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

package org.springframework.boot.actuate.health;

import java.util.Set;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.util.CollectionUtils;

/**
 * Maps a {@link Health} to a {@link WebEndpointResponse}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class HealthWebEndpointResponseMapper {

	private final HealthStatusHttpMapper statusHttpMapper;

	private final ShowDetails showDetails;

	private final Set<String> authorizedRoles;

	public HealthWebEndpointResponseMapper(HealthStatusHttpMapper statusHttpMapper, ShowDetails showDetails,
			Set<String> authorizedRoles) {
		this.statusHttpMapper = statusHttpMapper;
		this.showDetails = showDetails;
		this.authorizedRoles = authorizedRoles;
	}

	/**
	 * Maps the given {@code health} to a {@link WebEndpointResponse}, honouring the
	 * mapper's default {@link ShowDetails} using the given {@code securityContext}.
	 * @param health the health to map
	 * @param securityContext the security context
	 * @return the mapped response
	 */
	public WebEndpointResponse<Health> map(Health health, SecurityContext securityContext) {
		return map(health, securityContext, this.showDetails);
	}

	/**
	 * Maps the given {@code health} to a {@link WebEndpointResponse}, honouring the given
	 * {@code showDetails} using the given {@code securityContext}.
	 * @param health the health to map
	 * @param securityContext the security context
	 * @param showDetails when to show details in the response
	 * @return the mapped response
	 */
	public WebEndpointResponse<Health> map(Health health, SecurityContext securityContext, ShowDetails showDetails) {
		if (showDetails == ShowDetails.NEVER || (showDetails == ShowDetails.WHEN_AUTHORIZED
				&& (securityContext.getPrincipal() == null || !isUserInRole(securityContext)))) {
			health = Health.status(health.getStatus()).build();
		}
		Integer status = this.statusHttpMapper.mapStatus(health.getStatus());
		return new WebEndpointResponse<>(health, status);
	}

	private boolean isUserInRole(SecurityContext securityContext) {
		if (CollectionUtils.isEmpty(this.authorizedRoles)) {
			return true;
		}
		for (String role : this.authorizedRoles) {
			if (securityContext.isUserInRole(role)) {
				return true;
			}
		}
		return false;
	}

}
