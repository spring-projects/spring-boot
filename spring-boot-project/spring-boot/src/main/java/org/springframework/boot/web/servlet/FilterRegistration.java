/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebInitParam;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;

/**
 * Registers a {@link Filter} in a Servlet 3.0+ container. Can be used as an
 * annotation-based alternative to {@link FilterRegistrationBean}.
 *
 * @author Moritz Halbritter
 * @author Daeho Kwon
 * @since 3.5.0
 * @see FilterRegistrationBean
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Order
public @interface FilterRegistration {

	/**
	 * Whether this registration is enabled.
	 * @return whether this registration is enabled
	 */
	boolean enabled() default true;

	/**
	 * Order of the registration bean.
	 * @return the order of the registration bean
	 */
	@AliasFor(annotation = Order.class, attribute = "value")
	int order() default Ordered.LOWEST_PRECEDENCE;

	/**
	 * Name of this registration. If not specified the bean name will be used.
	 * @return the name
	 */
	String name() default "";

	/**
	 * Whether asynchronous operations are supported for this registration.
	 * @return whether asynchronous operations are supported
	 */
	boolean asyncSupported() default true;

	/**
	 * Dispatcher types that should be used with the registration.
	 * @return the dispatcher types
	 */
	DispatcherType[] dispatcherTypes() default {};

	/**
	 * Whether registration failures should be ignored. If set to true, a failure will be
	 * logged. If set to false, an {@link IllegalStateException} will be thrown.
	 * @return whether registration failures should be ignored
	 */
	boolean ignoreRegistrationFailure() default false;

	/**
	 * Init parameters to be used with the filter.
	 * @return the init parameters
	 */
	WebInitParam[] initParameters() default {};

	/**
	 * Whether the filter mappings should be matched after any declared Filter mappings of
	 * the ServletContext.
	 * @return whether the filter mappings should be matched after any declared Filter
	 * mappings of the ServletContext
	 */
	boolean matchAfter() default false;

	/**
	 * Servlet names that the filter will be registered against.
	 * @return the servlet names
	 */
	String[] servletNames() default {};

	/**
	 * Servlet classes that the filter will be registered against.
	 * @return the servlet classes
	 */
	Class<?>[] servletClasses() default {};

	/**
	 * URL patterns, as defined in the Servlet specification, that the filter will be
	 * registered against.
	 * @return the url patterns
	 */
	String[] urlPatterns() default {};

}
