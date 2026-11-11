/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;

/**
 * Information describing an endpoint that can be exposed by registering a servlet.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint}
 */
@Deprecated(since = "3.3.0", forRemoval = true)
@SuppressWarnings("removal")
public interface ExposableServletEndpoint extends ExposableEndpoint<Operation>, PathMappedEndpoint {

	/**
	 * Return details of the servlet that should be registered.
	 * @return the endpoint servlet
	 */
	EndpointServlet getEndpointServlet();

}
