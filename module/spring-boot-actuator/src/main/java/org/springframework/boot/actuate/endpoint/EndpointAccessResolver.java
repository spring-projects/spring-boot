/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

/**
 * Resolver for the permitted level of {@link Access access} to an endpoint.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public interface EndpointAccessResolver {

	/**
	 * Resolves the permitted level of access for the endpoint with the given
	 * {@code endpointId} and {@code defaultAccess}.
	 * @param endpointId the ID of the endpoint
	 * @param defaultAccess the default access level of the endpoint
	 * @return the permitted level of access, never {@code null}
	 */
	Access accessFor(EndpointId endpointId, Access defaultAccess);

}
