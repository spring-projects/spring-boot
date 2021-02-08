/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.config;

import java.io.IOException;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;

/**
 * Strategy class that can be used used to load {@link ConfigData} for a given
 * {@link ConfigDataResource}. Implementations should be added as a
 * {@code spring.factories} entries. The following constructor parameter types are
 * supported:
 * <ul>
 * <li>{@link Log} or {@link DeferredLogFactory} - if the loader needs deferred
 * logging</li>
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
	ConfigData load(ConfigDataLoaderContext context, R resource)
			throws IOException, ConfigDataResourceNotFoundException;

}
