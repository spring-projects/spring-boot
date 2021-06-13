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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.util.Instantiator;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * A collection of {@link ConfigDataLoader} instances loaded via {@code spring.factories}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoaders {

	private final Log logger;

	private final List<ConfigDataLoader<?>> loaders;

	private final List<Class<?>> resourceTypes;

	/**
	 * Create a new {@link ConfigDataLoaders} instance.
	 * @param logFactory the deferred log factory
	 * @param bootstrapContext the bootstrap context
	 * @param classLoader the class loader used when loading from {@code spring.factories}
	 */
	ConfigDataLoaders(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			ClassLoader classLoader) {
		this(logFactory, bootstrapContext, SpringFactoriesLoader.loadFactoryNames(ConfigDataLoader.class, classLoader));
	}

	/**
	 * Create a new {@link ConfigDataLoaders} instance.
	 * @param logFactory the deferred log factory
	 * @param bootstrapContext the bootstrap context
	 * @param names the {@link ConfigDataLoader} class names instantiate
	 */
	ConfigDataLoaders(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			List<String> names) {
		this.logger = logFactory.getLog(getClass());
		Instantiator<ConfigDataLoader<?>> instantiator = new Instantiator<>(ConfigDataLoader.class,
				(availableParameters) -> {
					availableParameters.add(Log.class, logFactory::getLog);
					availableParameters.add(DeferredLogFactory.class, logFactory);
					availableParameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
					availableParameters.add(BootstrapContext.class, bootstrapContext);
					availableParameters.add(BootstrapRegistry.class, bootstrapContext);
				});
		this.loaders = instantiator.instantiate(names);
		this.resourceTypes = getResourceTypes(this.loaders);
	}

	private List<Class<?>> getResourceTypes(List<ConfigDataLoader<?>> loaders) {
		List<Class<?>> resourceTypes = new ArrayList<>(loaders.size());
		for (ConfigDataLoader<?> loader : loaders) {
			resourceTypes.add(getResourceType(loader));
		}
		return Collections.unmodifiableList(resourceTypes);
	}

	private Class<?> getResourceType(ConfigDataLoader<?> loader) {
		return ResolvableType.forClass(loader.getClass()).as(ConfigDataLoader.class).resolveGeneric();
	}

	/**
	 * Load {@link ConfigData} using the first appropriate {@link ConfigDataLoader}.
	 * @param <R> the resource type
	 * @param context the loader context
	 * @param resource the resource to load
	 * @return the loaded {@link ConfigData}
	 * @throws IOException on IO error
	 */
	<R extends ConfigDataResource> ConfigData load(ConfigDataLoaderContext context, R resource) throws IOException {
		ConfigDataLoader<R> loader = getLoader(context, resource);
		this.logger.trace(LogMessage.of(() -> "Loading " + resource + " using loader " + loader.getClass().getName()));
		return loader.load(context, resource);
	}

	@SuppressWarnings("unchecked")
	private <R extends ConfigDataResource> ConfigDataLoader<R> getLoader(ConfigDataLoaderContext context, R resource) {
		ConfigDataLoader<R> result = null;
		for (int i = 0; i < this.loaders.size(); i++) {
			ConfigDataLoader<?> candidate = this.loaders.get(i);
			if (this.resourceTypes.get(i).isInstance(resource)) {
				ConfigDataLoader<R> loader = (ConfigDataLoader<R>) candidate;
				if (loader.isLoadable(context, resource)) {
					if (result != null) {
						throw new IllegalStateException("Multiple loaders found for resource '" + resource + "' ["
								+ candidate.getClass().getName() + "," + result.getClass().getName() + "]");
					}
					result = loader;
				}
			}
		}
		Assert.state(result != null, () -> "No loader found for resource '" + resource + "'");
		return result;
	}

}
