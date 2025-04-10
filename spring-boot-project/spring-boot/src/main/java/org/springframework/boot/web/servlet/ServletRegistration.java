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

import jakarta.servlet.Servlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebInitParam;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;

/**
 * Registers a {@link Servlet} in a Servlet 3.0+ container. Can be used as an
 * annotation-based alternative to {@link ServletRegistrationBean}.
 *
 * @author Moritz Halbritter
 * @author Dmytro Danilenkov
 * @since 3.5.0
 * @see ServletRegistrationBean
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Order
public @interface ServletRegistration {

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
	 * Whether registration failures should be ignored. If set to true, a failure will be
	 * logged. If set to false, an {@link IllegalStateException} will be thrown.
	 * @return whether registration failures should be ignored
	 */
	boolean ignoreRegistrationFailure() default false;

	/**
	 * URL mappings for the servlet. If not specified the mapping will default to '/'.
	 * @return the url mappings
	 */
	String[] urlMappings() default {};

	/**
	 * The {@code loadOnStartup} priority. See
	 * {@link jakarta.servlet.ServletRegistration.Dynamic#setLoadOnStartup} for details.
	 * @return the {@code loadOnStartup} priority
	 */
	int loadOnStartup() default -1;

	/**
	 * Init parameters to be used with the servlet.
	 * @return the init parameters
	 */
	WebInitParam[] initParameters() default {};

	/**
	 * The multipart configuration.
	 * @return the multipart configuration
	 */
	MultipartConfig multipartConfig() default @MultipartConfig;

}
