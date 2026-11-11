/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.annotation;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;

/**
 * An {@link ExposableEndpoint endpoint} discovered by an {@link EndpointDiscoverer}.
 *
 * @param <O> the operation type
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface DiscoveredEndpoint<O extends Operation> extends ExposableEndpoint<O> {

	/**
	 * Return {@code true} if the endpoint was discovered by the specified discoverer.
	 * @param discoverer the discoverer type
	 * @return {@code true} if discovered using the specified discoverer
	 */
	boolean wasDiscoveredBy(Class<? extends EndpointDiscoverer<?, ?>> discoverer);

	/**
	 * Return the source bean that was used to construct the {@link DiscoveredEndpoint}.
	 * @return the source endpoint bean
	 */
	Object getEndpointBean();

}
