/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.annotation;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.util.Assert;

/**
 * {@link EndpointFilter} the matches based on the {@link EndpointDiscoverer} the created
 * the endpoint.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class DiscovererEndpointFilter implements EndpointFilter<DiscoveredEndpoint<?>> {

	private final Class<? extends EndpointDiscoverer<?, ?>> discoverer;

	/**
	 * Create a new {@link DiscovererEndpointFilter} instance.
	 * @param discoverer the required discoverer
	 */
	protected DiscovererEndpointFilter(Class<? extends EndpointDiscoverer<?, ?>> discoverer) {
		Assert.notNull(discoverer, "'discoverer' must not be null");
		this.discoverer = discoverer;
	}

	@Override
	public boolean match(DiscoveredEndpoint<?> endpoint) {
		return endpoint.wasDiscoveredBy(this.discoverer);
	}

}
