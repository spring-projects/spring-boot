/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.bootstrap;

/**
 * Callback interface that can be used to initialize a {@link BootstrapRegistry} before it
 * is used.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see BootstrapRegistry
 */
@FunctionalInterface
public interface BootstrapRegistryInitializer {

	/**
	 * Initialize the given {@link BootstrapRegistry} with any required registrations.
	 * @param registry the registry to initialize
	 */
	void initialize(BootstrapRegistry registry);

}
