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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Strategy interface used to resolve {@link ConfigDataLocation locations} from a String
 * based location address. Implementations should be added as a {@code spring.factories}
 * entries. The following constructor parameter types are supported:
 * <ul>
 * <li>{@link Log} - if the resolver needs deferred logging</li>
 * <li>{@link Binder} - if the resolver needs to obtain values from the initial
 * {@link Environment}</li>
 * <li>{@link ResourceLoader} - if the resolver needs a resource loader</li>
 * <li>{@link ConfigurableBootstrapContext} - A bootstrap context that can be used to
 * store objects that may be expensive to create, or need to be shared
 * ({@link BootstrapContext} or {@link BootstrapRegistry} may also be used).</li>
 * </ul>
 * <p>
 * Resolvers may implement {@link Ordered} or use the {@link Order @Order} annotation. The
 * first resolver that supports the given location will be used.
 *
 * @param <L> the location type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public interface ConfigDataLocationResolver<L extends ConfigDataLocation> {

	/**
	 * Returns if the specified location address can be resolved by this resolver.
	 * @param context the location resolver context
	 * @param location the location to check.
	 * @return if the location is supported by this resolver
	 */
	boolean isResolvable(ConfigDataLocationResolverContext context, String location);

	/**
	 * Resolve a location string into one or more {@link ConfigDataLocation} instances.
	 * @param context the location resolver context
	 * @param location the location that should be resolved
	 * @param optional if the location is optional
	 * @return a list of resolved locations in ascending priority order. If the same key
	 * is contained in more than one of the location, then the later source will win.
	 * @throws ConfigDataLocationNotFoundException on a non-optional location that cannot
	 * be found
	 */
	List<L> resolve(ConfigDataLocationResolverContext context, String location, boolean optional)
			throws ConfigDataLocationNotFoundException;

	/**
	 * Resolve a location string into one or more {@link ConfigDataLocation} instances
	 * based on available profiles. This method is called once profiles have been deduced
	 * from the contributed values. By default this method returns an empty list.
	 * @param context the location resolver context
	 * @param location the location that should be resolved
	 * @param optional if the location is optional
	 * @param profiles profile information
	 * @return a list of resolved locations in ascending priority order.If the same key is
	 * contained in more than one of the location, then the later source will win.
	 * @throws ConfigDataLocationNotFoundException on a non-optional location that cannot
	 * be found
	 */
	default List<L> resolveProfileSpecific(ConfigDataLocationResolverContext context, String location, boolean optional,
			Profiles profiles) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

}
