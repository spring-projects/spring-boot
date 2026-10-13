/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Callback interface that can be used to customize auto-configured {@link MeterRegistry
 * MeterRegistries}.
 * <p>
 * Customizers are guaranteed to be applied before any {@link Meter} is registered with
 * the registry.
 *
 * @param <T> the registry type to customize
 * @author Jon Schneider
 * @since 4.0.0
 */
@FunctionalInterface
public interface MeterRegistryCustomizer<T extends MeterRegistry> {

	/**
	 * Customize the given {@code registry}.
	 * @param registry the registry to customize
	 */
	void customize(T registry);

}
