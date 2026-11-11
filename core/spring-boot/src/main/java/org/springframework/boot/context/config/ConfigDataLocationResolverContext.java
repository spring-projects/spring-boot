/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.Binder;

/**
 * Context provided to {@link ConfigDataLocationResolver} methods.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public interface ConfigDataLocationResolverContext {

	/**
	 * Provides access to a binder that can be used to obtain previously contributed
	 * values.
	 * @return a binder instance
	 */
	Binder getBinder();

	/**
	 * Provides access to the parent {@link ConfigDataResource} that triggered the resolve
	 * or {@code null} if there is no available parent.
	 * @return the parent location
	 */
	@Nullable ConfigDataResource getParent();

	/**
	 * Provides access to the {@link ConfigurableBootstrapContext} shared across all
	 * {@link EnvironmentPostProcessor EnvironmentPostProcessors}.
	 * @return the bootstrap context
	 */
	ConfigurableBootstrapContext getBootstrapContext();

}
