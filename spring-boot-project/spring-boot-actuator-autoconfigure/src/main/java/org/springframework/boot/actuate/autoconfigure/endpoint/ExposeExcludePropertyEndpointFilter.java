/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link EndpointFilter} that will filter endpoints based on {@code expose} and
 * {@code exclude} properties.
 *
 * @param <T> The operation type
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ExposeExcludePropertyEndpointFilter<T extends Operation>
		implements EndpointFilter<T> {

	private final Class<? extends EndpointDiscoverer<T>> discovererType;

	private final Set<String> expose;

	private final Set<String> exclude;

	private final Set<String> exposeDefaults;

	public ExposeExcludePropertyEndpointFilter(
			Class<? extends EndpointDiscoverer<T>> discovererType,
			Environment environment, String prefix, String... exposeDefaults) {
		Assert.notNull(discovererType, "Discoverer Type must not be null");
		Assert.notNull(environment, "Environment must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		Binder binder = Binder.get(environment);
		this.discovererType = discovererType;
		this.expose = bind(binder, prefix + ".expose");
		this.exclude = bind(binder, prefix + ".exclude");
		this.exposeDefaults = asSet(Arrays.asList(exposeDefaults));
	}

	public ExposeExcludePropertyEndpointFilter(
			Class<? extends EndpointDiscoverer<T>> discovererType,
			Collection<String> expose, Collection<String> exclude,
			String... exposeDefaults) {
		Assert.notNull(discovererType, "Discoverer Type must not be null");
		this.discovererType = discovererType;
		this.expose = asSet(expose);
		this.exclude = asSet(exclude);
		this.exposeDefaults = asSet(Arrays.asList(exposeDefaults));
	}

	private Set<String> bind(Binder binder, String name) {
		return asSet(binder.bind(name, Bindable.listOf(String.class))
				.orElseGet(ArrayList::new));
	}

	private Set<String> asSet(Collection<String> items) {
		if (items == null) {
			return Collections.emptySet();
		}
		return items.stream().map(String::toLowerCase)
				.collect(Collectors.toCollection(HashSet::new));
	}

	@Override
	public boolean match(EndpointInfo<T> info, EndpointDiscoverer<T> discoverer) {
		if (this.discovererType.isInstance(discoverer)) {
			return isExposed(info) && !isExcluded(info);
		}
		return true;
	}

	private boolean isExposed(EndpointInfo<T> info) {
		if (this.expose.isEmpty()) {
			return this.exposeDefaults.contains("*")
					|| contains(this.exposeDefaults, info);
		}
		return this.expose.contains("*") || contains(this.expose, info);
	}

	private boolean isExcluded(EndpointInfo<T> info) {
		if (this.exclude.isEmpty()) {
			return false;
		}
		return this.exclude.contains("*") || contains(this.exclude, info);
	}

	private boolean contains(Set<String> items, EndpointInfo<T> info) {
		return items.contains(info.getId().toLowerCase());
	}

}
