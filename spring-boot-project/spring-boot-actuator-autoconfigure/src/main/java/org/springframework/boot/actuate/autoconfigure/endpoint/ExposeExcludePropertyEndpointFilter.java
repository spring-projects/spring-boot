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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link EndpointFilter} that will filter endpoints based on {@code include} and
 * {@code exclude} patterns.
 *
 * @param <E> the endpoint type
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ExposeExcludePropertyEndpointFilter<E extends ExposableEndpoint<?>> implements EndpointFilter<E> {

	private final Class<E> endpointType;

	private final EndpointPatterns include;

	private final EndpointPatterns exclude;

	private final EndpointPatterns exposeDefaults;

	public ExposeExcludePropertyEndpointFilter(Class<E> endpointType, Environment environment, String prefix,
			String... exposeDefaults) {
		Assert.notNull(endpointType, "EndpointType must not be null");
		Assert.notNull(environment, "Environment must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		Binder binder = Binder.get(environment);
		this.endpointType = endpointType;
		this.include = new EndpointPatterns(bind(binder, prefix + ".include"));
		this.exclude = new EndpointPatterns(bind(binder, prefix + ".exclude"));
		this.exposeDefaults = new EndpointPatterns(exposeDefaults);
	}

	public ExposeExcludePropertyEndpointFilter(Class<E> endpointType, Collection<String> include,
			Collection<String> exclude, String... exposeDefaults) {
		Assert.notNull(endpointType, "EndpointType Type must not be null");
		this.endpointType = endpointType;
		this.include = new EndpointPatterns(include);
		this.exclude = new EndpointPatterns(exclude);
		this.exposeDefaults = new EndpointPatterns(exposeDefaults);
	}

	private List<String> bind(Binder binder, String name) {
		return binder.bind(name, Bindable.listOf(String.class)).orElseGet(ArrayList::new);
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
			return this.exposeDefaults.matchesAll() || this.exposeDefaults.matches(endpoint);
		}
		return this.include.matchesAll() || this.include.matches(endpoint);
	}

	private boolean isExcluded(ExposableEndpoint<?> endpoint) {
		if (this.exclude.isEmpty()) {
			return false;
		}
		return this.exclude.matchesAll() || this.exclude.matches(endpoint);
	}

	/**
	 * A set of endpoint patterns used to match IDs.
	 */
	private static class EndpointPatterns {

		private final boolean empty;

		private final boolean matchesAll;

		private final Set<EndpointId> endpointIds;

		EndpointPatterns(String[] patterns) {
			this((patterns != null) ? Arrays.asList(patterns) : (Collection<String>) null);
		}

		EndpointPatterns(Collection<String> patterns) {
			patterns = (patterns != null) ? patterns : Collections.emptySet();
			boolean matchesAll = false;
			Set<EndpointId> endpointIds = new LinkedHashSet<>();
			for (String pattern : patterns) {
				if ("*".equals(pattern)) {
					matchesAll = true;
				}
				else {
					endpointIds.add(EndpointId.fromPropertyValue(pattern));
				}
			}
			this.empty = patterns.isEmpty();
			this.matchesAll = matchesAll;
			this.endpointIds = endpointIds;
		}

		boolean isEmpty() {
			return this.empty;
		}

		boolean matchesAll() {
			return this.matchesAll;
		}

		boolean matches(ExposableEndpoint<?> endpoint) {
			return this.endpointIds.contains(endpoint.getEndpointId());
		}

	}

}
