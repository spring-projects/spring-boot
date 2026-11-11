/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;

/**
 * Context provided to {@link ConfigDataLoader} methods.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public interface ConfigDataLoaderContext {

	/**
	 * Provides access to the {@link ConfigurableBootstrapContext} shared across all
	 * {@link EnvironmentPostProcessor EnvironmentPostProcessors}.
	 * @return the bootstrap context
	 */
	ConfigurableBootstrapContext getBootstrapContext();

}
