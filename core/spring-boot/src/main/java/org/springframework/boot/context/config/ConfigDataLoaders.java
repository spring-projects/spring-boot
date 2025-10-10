/*
 * Copyright 2012-present the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * A collection of {@link ConfigDataLoader} instances loaded through
 * {@code spring.factories}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoaders {

	private final Log logger;

	@SuppressWarnings("rawtypes")
	private final List<ConfigDataLoader> loaders;

	private final List<Class<?>> resourceTypes;

	/**
	 * Create a new {@link ConfigDataLoaders} instance.
	 * @param logFactory the deferred log factory
	 * @param bootstrapContext the bootstrap context
	 * @param springFactoriesLoader the loader to use
	 */
	ConfigDataLoaders(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			SpringFactoriesLoader springFactoriesLoader) {
		this.logger = logFactory.getLog(getClass());
		ArgumentResolver argumentResolver = ArgumentResolver.of(DeferredLogFactory.class, logFactory);
		argumentResolver = argumentResolver.and(ConfigurableBootstrapContext.class, bootstrapContext);
		argumentResolver = argumentResolver.and(BootstrapContext.class, bootstrapContext);
		argumentResolver = argumentResolver.and(BootstrapRegistry.class, bootstrapContext);
		argumentResolver = argumentResolver.andSupplied(Log.class, () -> {
			throw new IllegalArgumentException("Log types cannot be injected, please use DeferredLogFactory");
		});
		this.loaders = springFactoriesLoader.load(ConfigDataLoader.class, argumentResolver);
		this.resourceTypes = getResourceTypes(this.loaders);
	}

	@SuppressWarnings("rawtypes")
	private List<Class<?>> getResourceTypes(List<ConfigDataLoader> loaders) {
		List<Class<?>> resourceTypes = new ArrayList<>(loaders.size());
		for (ConfigDataLoader<?> loader : loaders) {
			resourceTypes.add(getResourceType(loader));
		}
		return Collections.unmodifiableList(resourceTypes);
	}

	private Class<?> getResourceType(ConfigDataLoader<?> loader) {
		Class<?> generic = ResolvableType.forClass(loader.getClass()).as(ConfigDataLoader.class).resolveGeneric();
		Assert.state(generic != null, "'generic' must not be null");
		return generic;
	}

	/**
	 * Load {@link ConfigData} using the first appropriate {@link ConfigDataLoader}.
	 * @param <R> the resource type
	 * @param context the loader context
	 * @param resource the resource to load
	 * @return the loaded {@link ConfigData}
	 * @throws IOException on IO error
	 */
	<R extends ConfigDataResource> @Nullable ConfigData load(ConfigDataLoaderContext context, R resource)
			throws IOException {
		ConfigDataLoader<R> loader = getLoader(context, resource);
		this.logger.trace(LogMessage.of(() -> "Loading " + resource + " using loader " + loader.getClass().getName()));
		return loader.load(context, resource);
	}

	@SuppressWarnings("unchecked")
	private <R extends ConfigDataResource> ConfigDataLoader<R> getLoader(ConfigDataLoaderContext context, R resource) {
		ConfigDataLoader<R> result = null;
		for (int i = 0; i < this.loaders.size(); i++) {
			ConfigDataLoader<R> candidate = this.loaders.get(i);
			if (this.resourceTypes.get(i).isInstance(resource)) {
				if (candidate.isLoadable(context, resource)) {
					if (result != null) {
						throw new IllegalStateException("Multiple loaders found for resource '" + resource + "' ["
								+ candidate.getClass().getName() + "," + result.getClass().getName() + "]");
					}
					result = candidate;
				}
			}
		}
		Assert.state(result != null, () -> "No loader found for resource '" + resource + "'");
		return result;
	}

}
