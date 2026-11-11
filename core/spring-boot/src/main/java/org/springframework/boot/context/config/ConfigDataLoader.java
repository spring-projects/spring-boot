/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;

/**
 * Strategy class that can be used to load {@link ConfigData} for a given
 * {@link ConfigDataResource}. Implementations should be added as {@code spring.factories}
 * entries. The following constructor parameter types are supported:
 * <ul>
 * <li>{@link DeferredLogFactory} - if the loader needs deferred logging</li>
 * <li>{@link ConfigurableBootstrapContext} - A bootstrap context that can be used to
 * store objects that may be expensive to create, or need to be shared
 * ({@link BootstrapContext} or {@link BootstrapRegistry} may also be used).</li>
 * </ul>
 * <p>
 * Multiple loaders cannot claim the same resource.
 *
 * @param <R> the resource type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public interface ConfigDataLoader<R extends ConfigDataResource> {

	/**
	 * Returns if the specified resource can be loaded by this instance.
	 * @param context the loader context
	 * @param resource the resource to check.
	 * @return if the resource is supported by this loader
	 */
	default boolean isLoadable(ConfigDataLoaderContext context, R resource) {
		return true;
	}

	/**
	 * Load {@link ConfigData} for the given resource.
	 * @param context the loader context
	 * @param resource the resource to load
	 * @return the loaded config data or {@code null} if the location should be skipped
	 * @throws IOException on IO error
	 * @throws ConfigDataResourceNotFoundException if the resource cannot be found
	 */
	@Nullable ConfigData load(ConfigDataLoaderContext context, R resource)
			throws IOException, ConfigDataResourceNotFoundException;

}
