/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;

/**
 * Information describing an endpoint that can be exposed in some technology specific way.
 *
 * @param <O> the type of the endpoint's operations
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface ExposableEndpoint<O extends Operation> {

	/**
	 * Return the endpoint ID.
	 * @return the endpoint ID
	 */
	EndpointId getEndpointId();

	/**
	 * Returns the access to the endpoint that is permitted by default.
	 * @return access that is permitted by default
	 * @since 3.4.0
	 */
	Access getDefaultAccess();

	/**
	 * Returns the operations of the endpoint.
	 * @return the operations
	 */
	Collection<O> getOperations();

}
