/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link EndpointFilter} that will filter endpoints based on {@code include} and
 * {@code exclude} properties.
 *
 * @param <E> the endpoint type
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ExposeExcludePropertyEndpointFilter<E extends ExposableEndpoint<?>> implements EndpointFilter<E> {

	private final Class<E> endpointType;

	private final Set<String> include;

	private final Set<String> exclude;

	private final Set<String> exposeDefaults;

	public ExposeExcludePropertyEndpointFilter(Class<E> endpointType, Environment environment, String prefix,
			String... exposeDefaults) {
		Assert.notNull(endpointType, "EndpointType must not be null");
		Assert.notNull(environment, "Environment must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		Binder binder = Binder.get(environment);
		this.endpointType = endpointType;
		this.include = bind(binder, prefix + ".include");
		this.exclude = bind(binder, prefix + ".exclude");
		this.exposeDefaults = asSet(Arrays.asList(exposeDefaults));
	}

	public ExposeExcludePropertyEndpointFilter(Class<E> endpointType, Collection<String> include,
			Collection<String> exclude, String... exposeDefaults) {
		Assert.notNull(endpointType, "EndpointType Type must not be null");
		this.endpointType = endpointType;
		this.include = asSet(include);
		this.exclude = asSet(exclude);
		this.exposeDefaults = asSet(Arrays.asList(exposeDefaults));
	}

	private Set<String> bind(Binder binder, String name) {
		return asSet(binder.bind(name, Bindable.listOf(String.class)).map(this::cleanup).orElseGet(ArrayList::new));
	}

	private List<String> cleanup(List<String> values) {
		return values.stream().map(this::cleanup).collect(Collectors.toList());
	}

	private String cleanup(String value) {
		return "*".equals(value) ? "*" : EndpointId.fromPropertyValue(value).toLowerCaseString();
	}

	private Set<String> asSet(Collection<String> items) {
		if (items == null) {
			return Collections.emptySet();
		}
		return items.stream().map((item) -> item.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
	}

	@Override
	public boolean match(E endpoint) {
		if (this.endpointType.isInstance(endpoint)) {
			return isExposed(endpoint) && !isExcluded(endpoint);
		}
		return true;
	}

	private boolean isExposed(ExposableEndpoint<?> endpoint) {
		if (this.include.isEmpty()) {
			return this.exposeDefaults.contains("*") || contains(this.exposeDefaults, endpoint);
		}
		return this.include.contains("*") || contains(this.include, endpoint);
	}

	private boolean isExcluded(ExposableEndpoint<?> endpoint) {
		if (this.exclude.isEmpty()) {
			return false;
		}
		return this.exclude.contains("*") || contains(this.exclude, endpoint);
	}

	private boolean contains(Set<String> items, ExposableEndpoint<?> endpoint) {
		return items.contains(endpoint.getEndpointId().toLowerCaseString());
	}

}
