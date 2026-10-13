/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;

/**
 * A discovered {@link ExposableJmxEndpoint JMX endpoint}.
 *
 * @author Phillip Webb
 */
class DiscoveredJmxEndpoint extends AbstractDiscoveredEndpoint<JmxOperation> implements ExposableJmxEndpoint {

	@SuppressWarnings("removal")
	DiscoveredJmxEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<JmxOperation> operations) {
		super(discoverer, endpointBean, id, defaultAccess, operations);
	}

}
