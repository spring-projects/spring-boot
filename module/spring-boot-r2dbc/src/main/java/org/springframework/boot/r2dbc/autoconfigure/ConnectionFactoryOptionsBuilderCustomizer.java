/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder} to fine-tune its auto-configuration before it creates a
 * {@link ConnectionFactoryOptions}.
 *
 * @author Mark Paluch
 * @since 4.0.0
 */
@FunctionalInterface
public interface ConnectionFactoryOptionsBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param builder the builder to customize
	 */
	void customize(Builder builder);

}
