/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.FilteredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Identifies a type as being an endpoint that is only exposed over Spring MVC or Spring
 * WebFlux. Mapped methods must be annotated with {@link GetMapping @GetMapping},
 * {@link PostMapping @PostMapping}, {@link DeleteMapping @DeleteMapping}, etc.
 * annotations rather than {@link ReadOperation @ReadOperation},
 * {@link WriteOperation @WriteOperation}, {@link DeleteOperation @DeleteOperation}.
 * <p>
 * This annotation can be used when deeper Spring integration is required, but at the
 * expense of portability. Most users should prefer the {@link Endpoint @Endpoint} or
 * {@link WebEndpoint @WebEndpoint} annotation whenever possible.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see WebEndpoint
 * @see RestControllerEndpoint
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Endpoint
@FilteredEndpoint(ControllerEndpointFilter.class)
@Deprecated(since = "3.3.0", forRemoval = true)
public @interface ControllerEndpoint {

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
