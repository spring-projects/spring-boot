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

import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.SecurityContext;

/**
 * Test implementation of {@link HealthEndpointGroups}.
 *
 * @author Phillip Webb
 */
class TestHealthEndpointGroup implements HealthEndpointGroup {

	private final StatusAggregator statusAggregator = new SimpleStatusAggregator();

	private final HttpCodeStatusMapper httpCodeStatusMapper = new SimpleHttpCodeStatusMapper();

	private final Predicate<String> memberPredicate;

	private Boolean showComponents;

	private boolean showDetails = true;

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

}
