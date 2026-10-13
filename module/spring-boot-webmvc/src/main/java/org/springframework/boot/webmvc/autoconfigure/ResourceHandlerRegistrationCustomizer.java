/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;

/**
 * Callback interface that can be used to customize {@link ResourceHandlerRegistration}.
 *
 * @author Brian Clozel
 * @since 4.2.0
 */
@FunctionalInterface
public interface ResourceHandlerRegistrationCustomizer {

	/**
	 * Customize the given {@link ResourceHandlerRegistration}.
	 * @param registration the registration to customize
	 */
	void customize(ResourceHandlerRegistration registration);

}
