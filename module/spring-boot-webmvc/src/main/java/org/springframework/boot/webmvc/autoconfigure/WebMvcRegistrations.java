/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Interface to register key components of the {@link WebMvcConfigurationSupport} in place
 * of the default ones provided by Spring MVC.
 * <p>
 * All custom instances are later processed by Boot and Spring MVC configurations. To
 * participate in, and if desired, override that subsequent processing,
 * {@link WebMvcConfigurer} should be used.
 * <p>
 * A single instance of this component should be registered, otherwise making it
 * impossible to choose from redundant MVC components.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration.EnableWebMvcConfiguration
 */
public interface WebMvcRegistrations {

	/**
	 * Return the custom {@link RequestMappingHandlerMapping} that should be used and
	 * processed by the MVC configuration.
	 * @return the custom {@link RequestMappingHandlerMapping} instance
	 */
	default @Nullable RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		return null;
	}

	/**
	 * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
	 * processed by the MVC configuration.
	 * @return the custom {@link RequestMappingHandlerAdapter} instance
	 */
	default @Nullable RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return null;
	}

	/**
	 * Return the custom {@link ExceptionHandlerExceptionResolver} that should be used and
	 * processed by the MVC configuration.
	 * @return the custom {@link ExceptionHandlerExceptionResolver} instance
	 */
	default @Nullable ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
		return null;
	}

}
