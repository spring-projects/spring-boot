/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.core.annotation.AliasFor;

/**
 * Identifies a type as being a JMX-specific extension of an {@link Endpoint @Endpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see Endpoint
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EndpointExtension(filter = JmxEndpointFilter.class)
public @interface EndpointJmxExtension {

	/**
	 * The {@link Endpoint endpoint} class to which this JMX extension relates.
	 * @return the endpoint class
	 */
	@AliasFor(annotation = EndpointExtension.class)
	Class<?> endpoint();

}
