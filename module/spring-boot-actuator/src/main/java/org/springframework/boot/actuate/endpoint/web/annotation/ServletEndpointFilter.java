/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscovererEndpointFilter;

/**
 * {@link EndpointFilter} for endpoints discovered by {@link ServletEndpointDiscoverer}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("removal")
class ServletEndpointFilter extends DiscovererEndpointFilter {

	ServletEndpointFilter() {
		super(ServletEndpointDiscoverer.class);
	}

}
