/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.core.annotation.AliasFor;

/**
 * Identifies a type as being a Cloud Foundry specific extension for an
 * {@link Endpoint @Endpoint}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EndpointExtension(filter = CloudFoundryEndpointFilter.class)
public @interface EndpointCloudFoundryExtension {

	/**
	 * The class of the endpoint to provide a Cloud Foundry specific extension for.
	 * @return the class of the endpoint to extend
	 */
	@AliasFor(annotation = EndpointExtension.class, attribute = "endpoint")
	Class<?> endpoint();

}
