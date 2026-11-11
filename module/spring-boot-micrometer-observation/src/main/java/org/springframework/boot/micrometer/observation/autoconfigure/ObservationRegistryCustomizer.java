/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import io.micrometer.observation.ObservationRegistry;

/**
 * Callback interface that can be used to customize auto-configured
 * {@link ObservationRegistry observation registries}.
 *
 * @param <T> the registry type to customize
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@FunctionalInterface
public interface ObservationRegistryCustomizer<T extends ObservationRegistry> {

	/**
	 * Customize the given {@code registry}.
	 * @param registry the registry to customize
	 */
	void customize(T registry);

}
