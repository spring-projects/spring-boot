/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.security.Principal;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.lang.Nullable;

/**
 * Reactive {@link EndpointWebExtension} for the {@link HealthEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
public class ReactiveHealthEndpointWebExtension {

	private final ReactiveHealthIndicator delegate;

	private final HealthStatusHttpMapper statusHttpMapper;

	private final ShowDetails showDetails;

	public ReactiveHealthEndpointWebExtension(ReactiveHealthIndicator delegate,
			HealthStatusHttpMapper statusHttpMapper, ShowDetails showDetails) {
		this.delegate = delegate;
		this.statusHttpMapper = statusHttpMapper;
		this.showDetails = showDetails;
	}

	@ReadOperation
	public Mono<WebEndpointResponse<Health>> health(@Nullable Principal principal) {
		return health(principal, this.showDetails);
	}

	public Mono<WebEndpointResponse<Health>> health(Principal principal,
			ShowDetails showDetails) {
		return this.delegate.health().map((health) -> {
			Integer status = this.statusHttpMapper.mapStatus(health.getStatus());
			if (this.showDetails == ShowDetails.NEVER
					|| (this.showDetails == ShowDetails.WHEN_AUTHENTICATED
							&& principal == null)) {
				health = Health.status(health.getStatus()).build();
			}
			return new WebEndpointResponse<>(health, status);
		});
	}

}
