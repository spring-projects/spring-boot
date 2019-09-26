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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.EndpointCloudFoundryExtension;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;

/**
 * Reactive {@link EndpointExtension @EndpointExtension} for the {@link HealthEndpoint}
 * that always exposes full health details.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@EndpointCloudFoundryExtension(endpoint = HealthEndpoint.class)
public class CloudFoundryReactiveHealthEndpointWebExtension {

	private final ReactiveHealthEndpointWebExtension delegate;

	public CloudFoundryReactiveHealthEndpointWebExtension(ReactiveHealthEndpointWebExtension delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion) {
		return this.delegate.health(apiVersion, SecurityContext.NONE, true);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends HealthComponent>> health(ApiVersion apiVersion,
			@Selector(match = Match.ALL_REMAINING) String... path) {
		return this.delegate.health(apiVersion, SecurityContext.NONE, true, path);
	}

}
