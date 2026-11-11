/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * A customizer for a {@link ConfigurableMockMvcBuilder}. Any
 * {@code MockMvcBuilderCustomizer} beans found in the application context will be
 * {@link #customize called} to customize the auto-configured {@link MockMvcBuilder}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @see MockMvcAutoConfiguration
 */
@FunctionalInterface
public interface MockMvcBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder
	 */
	void customize(ConfigurableMockMvcBuilder<?> builder);

}
