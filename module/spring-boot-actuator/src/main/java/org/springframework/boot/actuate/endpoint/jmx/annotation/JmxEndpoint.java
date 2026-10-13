/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.FilteredEndpoint;
import org.springframework.core.annotation.AliasFor;

/**
 * Identifies a type as being an endpoint that is only exposed over JMX.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Endpoint
@FilteredEndpoint(JmxEndpointFilter.class)
public @interface JmxEndpoint {

	/**
	 * The id of the endpoint.
	 * @return the id
	 */
	@AliasFor(annotation = Endpoint.class)
	String id() default "";

	/**
	 * Level of access to the endpoint that is permitted by default.
	 * @return the default level of access
	 * @since 3.4.0
	 */
	@AliasFor(annotation = Endpoint.class)
	Access defaultAccess() default Access.UNRESTRICTED;

}
