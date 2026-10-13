/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscovererEndpointFilter;

/**
 * {@link EndpointFilter} for endpoints discovered by {@link JmxEndpointDiscoverer}.
 *
 * @author Phillip Webb
 */
class JmxEndpointFilter extends DiscovererEndpointFilter {

	JmxEndpointFilter() {
		super(JmxEndpointDiscoverer.class);
	}

}
