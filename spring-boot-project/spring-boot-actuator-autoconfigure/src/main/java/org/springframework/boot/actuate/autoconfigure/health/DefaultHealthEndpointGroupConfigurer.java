/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroup.Show;
import org.springframework.boot.actuate.health.HealthEndpointGroupConfigurer;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

/**
 * Mutable {@link HealthEndpointGroupConfigurer configurer} for
 * {@link HealthEndpointGroup}.
 *
 * @author Brian Clozel
 */
class DefaultHealthEndpointGroupConfigurer implements HealthEndpointGroupConfigurer {

	Set<String> includedIndicators;

	Set<String> excludedIndicators;

	private StatusAggregator statusAggregator;

	private HttpCodeStatusMapper httpCodeStatusMapper;

	private Show showComponents;

	private Show showDetails;

	private Set<String> roles;

	DefaultHealthEndpointGroupConfigurer(StatusAggregator defaultStatusAggregator,
			HttpCodeStatusMapper defaultHttpCodeStatusMapper, Show defaultShowComponents, Show defaultShowDetails,
			Set<String> defaultRoles) {
		this.statusAggregator = defaultStatusAggregator;
		this.httpCodeStatusMapper = defaultHttpCodeStatusMapper;
		this.showComponents = defaultShowComponents;
		this.showDetails = defaultShowDetails;
		this.roles = new HashSet<>(defaultRoles);
	}

	@Override
	public HealthEndpointGroupConfigurer include(String... indicators) {
		this.includedIndicators = new HashSet<>(Arrays.asList(indicators));
		return this;
	}

	@Override
	public HealthEndpointGroupConfigurer exclude(String... exclude) {
		this.excludedIndicators = new HashSet<>(Arrays.asList(exclude));
		return this;
	}

	@Override
	public HealthEndpointGroupConfigurer statusAggregator(StatusAggregator statusAggregator) {
		this.statusAggregator = statusAggregator;
		return this;
	}

	@Override
	public HealthEndpointGroupConfigurer httpCodeStatusMapper(HttpCodeStatusMapper httpCodeStatusMapper) {
		this.httpCodeStatusMapper = httpCodeStatusMapper;
		return this;
	}

	@Override
	public HealthEndpointGroupConfigurer showComponents(Show showComponents) {
		this.showComponents = showComponents;
		return this;
	}

	@Override
	public HealthEndpointGroupConfigurer showDetails(Show showDetails) {
		this.showDetails = showDetails;
		return this;
	}

	@Override
	public HealthEndpointGroupConfigurer roles(String... roles) {
		this.roles = new HashSet<>(Arrays.asList(roles));
		return this;
	}

	HealthEndpointGroup toHealthEndpointGroup() {
		IncludeExcludeGroupMemberPredicate predicate = new IncludeExcludeGroupMemberPredicate(this.includedIndicators,
				this.excludedIndicators);
		return new DefaultHealthEndpointGroup(predicate, this.statusAggregator, this.httpCodeStatusMapper,
				this.showComponents, this.showDetails, this.roles);
	}

}
