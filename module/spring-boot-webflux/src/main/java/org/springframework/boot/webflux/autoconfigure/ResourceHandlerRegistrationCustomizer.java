/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import org.springframework.web.reactive.config.ResourceHandlerRegistration;

/**
 * Callback interface that can be used to customize {@link ResourceHandlerRegistration}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@FunctionalInterface
public interface ResourceHandlerRegistrationCustomizer {

	/**
	 * Customize the given {@link ResourceHandlerRegistration}.
	 * @param registration the registration to customize
	 */
	void customize(ResourceHandlerRegistration registration);

}
