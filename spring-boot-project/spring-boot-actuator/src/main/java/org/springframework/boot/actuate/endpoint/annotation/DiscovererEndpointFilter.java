/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.annotation;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.util.Assert;

/**
 * {@link EndpointFilter} the matches based on the {@link EndpointDiscoverer} the created
 * the endpoint.
 *
 * @author Phillip Webb
 */
public abstract class DiscovererEndpointFilter
		implements EndpointFilter<DiscoveredEndpoint<?>> {

	private final Class<? extends EndpointDiscoverer<?, ?>> discoverer;

	/**
	 * Create a new {@link DiscovererEndpointFilter} instance.
	 * @param discoverer the required discoverer
	 */
	protected DiscovererEndpointFilter(
			Class<? extends EndpointDiscoverer<?, ?>> discoverer) {
		Assert.notNull(discoverer, "Discoverer must not be null");
		this.discoverer = discoverer;
	}

	@Override
	public boolean match(DiscoveredEndpoint<?> endpoint) {
		return endpoint.wasDiscoveredBy(this.discoverer);
	}

}
