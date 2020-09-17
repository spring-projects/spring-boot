/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.util.Instantiator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * A collection of {@link ConfigDataLocationResolver} instances loaded via
 * {@code spring.factories}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLocationResolvers {

	private final Log logger;

	private final ConfigDataLocationNotFoundAction locationNotFoundAction;

	private final List<ConfigDataLocationResolver<?>> resolvers;

	/**
	 * Create a new {@link ConfigDataLocationResolvers} instance.
	 * @param logFactory a {@link DeferredLogFactory} used to inject {@link Log} instances
	 * @param bootstrapContext the bootstrap context
	 * @param locationNotFoundAction the action to take if a
	 * {@link ConfigDataLocationNotFoundException} is thrown
	 * @param binder a binder providing values from the initial {@link Environment}
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 */
	ConfigDataLocationResolvers(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			ConfigDataLocationNotFoundAction locationNotFoundAction, Binder binder, ResourceLoader resourceLoader) {
		this(logFactory, bootstrapContext, locationNotFoundAction, binder, resourceLoader,
				SpringFactoriesLoader.loadFactoryNames(ConfigDataLocationResolver.class, null));
	}

	/**
	 * Create a new {@link ConfigDataLocationResolvers} instance.
	 * @param logFactory a {@link DeferredLogFactory} used to inject {@link Log} instances
	 * @param bootstrapContext the bootstrap context
	 * @param locationNotFoundAction the action to take if a
	 * {@link ConfigDataLocationNotFoundException} is thrown
	 * @param binder {@link Binder} providing values from the initial {@link Environment}
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 * @param names the {@link ConfigDataLocationResolver} class names
	 */
	ConfigDataLocationResolvers(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			ConfigDataLocationNotFoundAction locationNotFoundAction, Binder binder, ResourceLoader resourceLoader,
			List<String> names) {
		this.logger = logFactory.getLog(getClass());
		this.locationNotFoundAction = locationNotFoundAction;
		Instantiator<ConfigDataLocationResolver<?>> instantiator = new Instantiator<>(ConfigDataLocationResolver.class,
				(availableParameters) -> {
					availableParameters.add(Log.class, logFactory::getLog);
					availableParameters.add(Binder.class, binder);
					availableParameters.add(ResourceLoader.class, resourceLoader);
					availableParameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
					availableParameters.add(BootstrapContext.class, bootstrapContext);
					availableParameters.add(BootstrapRegistry.class, bootstrapContext);
				});
		this.resolvers = reorder(instantiator.instantiate(names));
	}

	private List<ConfigDataLocationResolver<?>> reorder(List<ConfigDataLocationResolver<?>> resolvers) {
		List<ConfigDataLocationResolver<?>> reordered = new ArrayList<>(resolvers.size());
		ResourceConfigDataLocationResolver resourceResolver = null;
		for (ConfigDataLocationResolver<?> resolver : resolvers) {
			if (resolver instanceof ResourceConfigDataLocationResolver) {
				resourceResolver = (ResourceConfigDataLocationResolver) resolver;
			}
			else {
				reordered.add(resolver);
			}
		}
		if (resourceResolver != null) {
			reordered.add(resourceResolver);
		}
		return Collections.unmodifiableList(reordered);
	}

	/**
	 * Resolve all location strings using the most appropriate
	 * {@link ConfigDataLocationResolver}.
	 * @param context the location resolver context
	 * @param locations the locations to resolve
	 * @param profiles the current profiles or {@code null}
	 * @return the resolved locations
	 */
	List<ConfigDataLocation> resolveAll(ConfigDataLocationResolverContext context, List<String> locations,
			Profiles profiles) {
		List<ConfigDataLocation> resolved = new ArrayList<>(locations.size());
		for (String location : locations) {
			resolved.addAll(resolveAll(context, location, profiles));
		}
		return resolved;
	}

	private List<ConfigDataLocation> resolveAll(ConfigDataLocationResolverContext context, String location,
			Profiles profiles) {
		boolean optional = location != null && location.startsWith(ConfigDataLocation.OPTIONAL_PREFIX);
		location = (!optional) ? location : location.substring(ConfigDataLocation.OPTIONAL_PREFIX.length());
		if (!StringUtils.hasText(location)) {
			return Collections.emptyList();
		}
		for (ConfigDataLocationResolver<?> resolver : getResolvers()) {
			if (resolver.isResolvable(context, location)) {
				return resolve(resolver, context, optional, location, profiles);
			}
		}
		throw new UnsupportedConfigDataLocationException(location);
	}

	private List<ConfigDataLocation> resolve(ConfigDataLocationResolver<?> resolver,
			ConfigDataLocationResolverContext context, boolean optional, String location, Profiles profiles) {
		List<ConfigDataLocation> resolved = resolve(location, optional,
				() -> resolver.resolve(context, location, optional));
		if (profiles == null) {
			return resolved;
		}
		List<ConfigDataLocation> profileSpecific = resolve(location, optional,
				() -> resolver.resolveProfileSpecific(context, location, optional, profiles));
		return merge(resolved, profileSpecific);
	}

	private List<ConfigDataLocation> resolve(String location, boolean optional,
			Supplier<List<? extends ConfigDataLocation>> resolveAction) {
		try {
			List<ConfigDataLocation> resolved = nonNullList(resolveAction.get());
			if (!resolved.isEmpty() && optional) {
				resolved = OptionalConfigDataLocation.wrapAll(resolved);
			}
			return resolved;
		}
		catch (ConfigDataLocationNotFoundException ex) {
			if (optional) {
				this.logger.trace(LogMessage.format("Skipping missing resource from optional location %s", location));
				return Collections.emptyList();
			}
			this.locationNotFoundAction.handle(this.logger, location, ex);
			return Collections.emptyList();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> nonNullList(List<? extends T> list) {
		return (list != null) ? (List<T>) list : Collections.emptyList();
	}

	private <T> List<T> merge(List<T> list1, List<T> list2) {
		List<T> merged = new ArrayList<>(list1.size() + list2.size());
		merged.addAll(list1);
		merged.addAll(list2);
		return merged;
	}

	/**
	 * Return the resolvers managed by this object.
	 * @return the resolvers
	 */
	List<ConfigDataLocationResolver<?>> getResolvers() {
		return this.resolvers;
	}

}
