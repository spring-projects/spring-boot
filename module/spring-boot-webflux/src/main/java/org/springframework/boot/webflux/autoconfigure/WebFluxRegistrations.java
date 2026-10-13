/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * Interface to register key components of the {@link WebFluxAutoConfiguration} in place
 * of the default ones provided by Spring WebFlux.
 * <p>
 * All custom instances are later processed by Boot and Spring WebFlux configurations. A
 * single instance of this component should be registered, otherwise making it impossible
 * to choose from redundant WebFlux components.
 *
 * @author Artsiom Yudovin
 * @since 4.0.0
 * @see org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration.EnableWebFluxConfiguration
 */
public interface WebFluxRegistrations {

	/**
	 * Return the custom {@link RequestMappingHandlerMapping} that should be used and
	 * processed by the WebFlux configuration.
	 * @return the custom {@link RequestMappingHandlerMapping} instance
	 */
	default @Nullable RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		return null;
	}

	/**
	 * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
	 * processed by the WebFlux configuration.
	 * @return the custom {@link RequestMappingHandlerAdapter} instance
	 */
	default @Nullable RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return null;
	}

}
