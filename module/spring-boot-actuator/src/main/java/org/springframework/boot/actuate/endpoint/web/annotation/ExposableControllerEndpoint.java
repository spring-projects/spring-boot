/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Information describing an endpoint that can be exposed over Spring MVC or Spring
 * WebFlux. Mappings should be discovered directly from {@link #getController()} and
 * {@link #getOperations()} should always return an empty collection.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.3 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@Deprecated(since = "3.3.3", forRemoval = true)
public interface ExposableControllerEndpoint extends ExposableEndpoint<Operation>, PathMappedEndpoint {

	/**
	 * Return the source controller that contains {@link RequestMapping @RequestMapping}
	 * methods.
	 * @return the source controller
	 */
	Object getController();

}
