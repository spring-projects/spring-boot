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

package org.springframework.boot.actuate.autoconfigure.availability;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
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
		Assert.notNull(delegate, "Delegate must not be null");
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
