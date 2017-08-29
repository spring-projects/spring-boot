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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.autoconfigure.endpoint.support.EndpointEnablementProvider;
import org.springframework.boot.endpoint.EndpointDelivery;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.Operation;
import org.springframework.core.env.Environment;

/**
 * Provides the endpoints that are enabled according to an {@link EndpointDiscoverer} and
 * the current {@link Environment}.
 *
 * @param <T> the endpoint operation type
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public final class EndpointProvider<T extends Operation> {

	private final EndpointDiscoverer<T> discoverer;

	private final EndpointEnablementProvider endpointEnablementProvider;

	private final EndpointDelivery delivery;

	/**
	 * Creates a new instance.
	 * @param environment the environment to use to check the endpoints that are enabled
	 * @param discoverer the discoverer to get the initial set of endpoints
	 * @param delivery the delivery technology for the endpoint
	 */
	public EndpointProvider(Environment environment, EndpointDiscoverer<T> discoverer,
			EndpointDelivery delivery) {
		this.discoverer = discoverer;
		this.endpointEnablementProvider = new EndpointEnablementProvider(environment);
		this.delivery = delivery;
	}

	public Collection<EndpointInfo<T>> getEndpoints() {
		return this.discoverer.discoverEndpoints().stream().filter(this::isEnabled)
				.collect(Collectors.toList());
	}

	private boolean isEnabled(EndpointInfo<?> endpoint) {
		return this.endpointEnablementProvider.getEndpointEnablement(endpoint.getId(),
				endpoint.isEnabledByDefault(), this.delivery).isEnabled();
	}

}
