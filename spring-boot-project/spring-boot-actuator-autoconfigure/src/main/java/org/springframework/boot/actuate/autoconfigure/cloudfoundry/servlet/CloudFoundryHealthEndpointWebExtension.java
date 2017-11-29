/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryEndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;

/**
 * {@link EndpointWebExtension} for the {@link HealthEndpoint}
 * that always exposes full health details.
 *
 * @author Madhura Bhave
 */
@EndpointExtension(filter = CloudFoundryEndpointFilter.class, endpoint = HealthEndpoint.class)
public class CloudFoundryHealthEndpointWebExtension {

	private final HealthEndpoint delegate;

	private final HealthStatusHttpMapper statusHttpMapper;

	public CloudFoundryHealthEndpointWebExtension(HealthEndpoint delegate,
			HealthStatusHttpMapper statusHttpMapper) {
		this.delegate = delegate;
		this.statusHttpMapper = statusHttpMapper;
	}

	@ReadOperation
	public WebEndpointResponse<Health> getHealth() {
		Health health = this.delegate.health();
		Integer status = this.statusHttpMapper.mapStatus(health.getStatus());
		return new WebEndpointResponse<>(health, status);
	}

}

