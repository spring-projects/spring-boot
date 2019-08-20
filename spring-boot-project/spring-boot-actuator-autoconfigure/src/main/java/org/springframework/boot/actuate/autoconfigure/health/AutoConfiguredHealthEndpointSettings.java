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

import java.util.Collection;

import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties.ShowDetails;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.HealthEndpointSettings;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.util.CollectionUtils;

/**
 * Auto-configured {@link HealthEndpointSettings} backed by
 * {@link HealthEndpointProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class AutoConfiguredHealthEndpointSettings implements HealthEndpointSettings {

	private final StatusAggregator statusAggregator;

	private final HttpCodeStatusMapper httpCodeStatusMapper;

	private final ShowDetails showDetails;

	private final Collection<String> roles;

	/**
	 * Create a new {@link AutoConfiguredHealthEndpointSettings} instance.
	 * @param statusAggregator the status aggregator to use
	 * @param httpCodeStatusMapper the HTTP code status mapper to use
	 * @param showDetails the show details setting
	 * @param roles the roles to match
	 */
	AutoConfiguredHealthEndpointSettings(StatusAggregator statusAggregator, HttpCodeStatusMapper httpCodeStatusMapper,
			ShowDetails showDetails, Collection<String> roles) {
		this.statusAggregator = statusAggregator;
		this.httpCodeStatusMapper = httpCodeStatusMapper;
		this.showDetails = showDetails;
		this.roles = roles;
	}

	@Override
	public boolean includeDetails(SecurityContext securityContext) {
		ShowDetails showDetails = this.showDetails;
		switch (showDetails) {
		case NEVER:
			return false;
		case ALWAYS:
			return true;
		case WHEN_AUTHORIZED:
			return isAuthorized(securityContext);
		}
		throw new IllegalStateException("Unsupported ShowDetails value " + showDetails);
	}

	private boolean isAuthorized(SecurityContext securityContext) {
		if (securityContext.getPrincipal() == null) {
			return false;
		}
		return CollectionUtils.isEmpty(this.roles) || this.roles.stream().anyMatch(securityContext::isUserInRole);
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
