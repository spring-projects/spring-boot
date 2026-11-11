/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscovererEndpointFilter;

/**
 * {@link EndpointFilter} for endpoints discovered by
 * {@link CloudFoundryWebEndpointDiscoverer}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryEndpointFilter extends DiscovererEndpointFilter {

	protected CloudFoundryEndpointFilter() {
		super(CloudFoundryWebEndpointDiscoverer.class);
	}

}
