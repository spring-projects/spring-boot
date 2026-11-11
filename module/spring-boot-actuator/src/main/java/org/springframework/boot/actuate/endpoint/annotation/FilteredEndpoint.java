/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.EndpointFilter;

/**
 * Annotation that can be used on an {@link Endpoint @Endpoint} to implement implicit
 * filtering. Often used as a meta-annotation on technology specific endpoint annotations,
 * for example:<pre class="code">
 * &#64;Endpoint
 * &#64;FilteredEndpoint(WebEndpointFilter.class)
 * public &#64;interface WebEndpoint {
 *
 *     &#64;AliasFor(annotation = Endpoint.class, attribute = "id")
 *     String id();
 *
 *     &#64;AliasFor(annotation = Endpoint.class, attribute = "enableByDefault")
 *     boolean enableByDefault() default true;
 *
 * } </pre>
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see DiscovererEndpointFilter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FilteredEndpoint {

	/**
	 * The filter class to use.
	 * @return the filter class
	 */
	Class<? extends EndpointFilter<?>> value();

}
