/*
 * Copyright 2012-2023 the original author or authors.
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;

/**
 * A collection of {@link ConfigDataLocationResolver} instances loaded via
 * {@code spring.factories}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLocationResolvers {

	private final List<ConfigDataLocationResolver<?>> resolvers;

	/**
	 * Create a new {@link ConfigDataLocationResolvers} instance.
	 * @param logFactory a {@link DeferredLogFactory} used to inject {@link Log} instances
	 * @param bootstrapContext the bootstrap context
	 * @param binder a binder providing values from the initial {@link Environment}
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 * @param springFactoriesLoader to load {@link ConfigDataLocationResolver} instances
	 */
	ConfigDataLocationResolvers(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			Binder binder, ResourceLoader resourceLoader, SpringFactoriesLoader springFactoriesLoader) {
		ArgumentResolver argumentResolver = ArgumentResolver.of(DeferredLogFactory.class, logFactory);
		argumentResolver = argumentResolver.and(Binder.class, binder);
		argumentResolver = argumentResolver.and(ResourceLoader.class, resourceLoader);
		argumentResolver = argumentResolver.and(ConfigurableBootstrapContext.class, bootstrapContext);
		argumentResolver = argumentResolver.and(BootstrapContext.class, bootstrapContext);
		argumentResolver = argumentResolver.and(BootstrapRegistry.class, bootstrapContext);
		argumentResolver = argumentResolver.andSupplied(Log.class, () -> {
			throw new IllegalArgumentException("Log types cannot be injected, please use DeferredLogFactory");
		});
		this.resolvers = reorder(springFactoriesLoader.load(ConfigDataLocationResolver.class, argumentResolver));
	}

	/**
	 * Reorders the list of ConfigDataLocationResolver objects by moving the
	 * StandardConfigDataLocationResolver to the end of the list, if present.
	 * @param resolvers the list of ConfigDataLocationResolver objects to be reordered
	 * @return the reordered list of ConfigDataLocationResolver objects
	 */
	@SuppressWarnings("rawtypes")
	private List<ConfigDataLocationResolver<?>> reorder(List<ConfigDataLocationResolver> resolvers) {
		List<ConfigDataLocationResolver<?>> reordered = new ArrayList<>(resolvers.size());
		StandardConfigDataLocationResolver resourceResolver = null;
		for (ConfigDataLocationResolver<?> resolver : resolvers) {
			if (resolver instanceof StandardConfigDataLocationResolver configDataLocationResolver) {
				resourceResolver = configDataLocationResolver;
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
	 * Resolves the given configuration data location using the available resolvers.
	 * @param context the resolver context
	 * @param location the configuration data location to resolve
	 * @param profiles the active profiles
	 * @return a list of resolution results
	 * @throws UnsupportedConfigDataLocationException if the given location is not
	 * supported by any resolver
	 */
	List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location,
			Profiles profiles) {
		if (location == null) {
			return Collections.emptyList();
		}
		for (ConfigDataLocationResolver<?> resolver : getResolvers()) {
			if (resolver.isResolvable(context, location)) {
				return resolve(resolver, context, location, profiles);
			}
		}
		throw new UnsupportedConfigDataLocationException(location);
	}

	/**
	 * Resolves the given {@link ConfigDataLocation} using the provided
	 * {@link ConfigDataLocationResolver}, {@link ConfigDataLocationResolverContext}, and
	 * {@link Profiles}.
	 * @param resolver The {@link ConfigDataLocationResolver} used to resolve the
	 * location.
	 * @param context The {@link ConfigDataLocationResolverContext} used during
	 * resolution.
	 * @param location The {@link ConfigDataLocation} to be resolved.
	 * @param profiles The {@link Profiles} to be used for profile-specific resolution.
	 * @return A list of {@link ConfigDataResolutionResult} containing the resolved
	 * configuration data.
	 */
	private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolver<?> resolver,
			ConfigDataLocationResolverContext context, ConfigDataLocation location, Profiles profiles) {
		List<ConfigDataResolutionResult> resolved = resolve(location, false, () -> resolver.resolve(context, location));
		if (profiles == null) {
			return resolved;
		}
		List<ConfigDataResolutionResult> profileSpecific = resolve(location, true,
				() -> resolver.resolveProfileSpecific(context, location, profiles));
		return merge(resolved, profileSpecific);
	}

	/**
	 * Resolves the configuration data for the given location.
	 * @param location the location of the configuration data
	 * @param profileSpecific flag indicating whether the resolution is profile-specific
	 * @param resolveAction the supplier function to resolve the configuration data
	 * resources
	 * @return a list of ConfigDataResolutionResult objects representing the resolved
	 * configuration data
	 */
	private List<ConfigDataResolutionResult> resolve(ConfigDataLocation location, boolean profileSpecific,
			Supplier<List<? extends ConfigDataResource>> resolveAction) {
		List<ConfigDataResource> resources = nonNullList(resolveAction.get());
		List<ConfigDataResolutionResult> resolved = new ArrayList<>(resources.size());
		for (ConfigDataResource resource : resources) {
			resolved.add(new ConfigDataResolutionResult(location, resource, profileSpecific));
		}
		return resolved;
	}

	/**
	 * Returns a non-null list based on the given list.
	 * @param <T> the type of elements in the list
	 * @param list the list to be checked for null
	 * @return a non-null list if the given list is not null, otherwise an empty list
	 */
	@SuppressWarnings("unchecked")
	private <T> List<T> nonNullList(List<? extends T> list) {
		return (list != null) ? (List<T>) list : Collections.emptyList();
	}

	/**
	 * Merges two lists into a single list.
	 * @param <T> the type of elements in the lists
	 * @param list1 the first list to be merged
	 * @param list2 the second list to be merged
	 * @return a new list containing all elements from both input lists
	 */
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
