/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.FilteredEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.core.annotation.AliasFor;

/**
 * Identifies a type as being an endpoint that supplies a servlet to expose.
 * Implementations must also implement {@link Supplier Supplier&lt;EndpointServlet&gt;}
 * and return a valid {@link EndpointServlet}.
 * <p>
 * This annotation can be used when existing servlets need to be exposed as actuator
 * endpoints, but it is at the expense of portability. Most users should prefer the
 * {@link Endpoint @Endpoint} or {@link WebEndpoint @WebEndpoint} annotations whenever
 * possible.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Endpoint
@FilteredEndpoint(ServletEndpointFilter.class)
@Deprecated(since = "3.3.0", forRemoval = true)
public @interface ServletEndpoint {

	/**
	 * The id of the endpoint.
	 * @return the id
	 */
	@AliasFor(annotation = Endpoint.class)
	String id();

	/**
	 * Level of access to the endpoint that is permitted by default.
	 * @return the default level of access
	 * @since 3.4.0
	 */
	@AliasFor(annotation = Endpoint.class)
	Access defaultAccess() default Access.UNRESTRICTED;

}
