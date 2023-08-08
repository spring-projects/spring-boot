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

package org.springframework.boot.actuate.autoconfigure.endpoint.expose;

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
 * @since 2.2.7
 */
public class IncludeExcludeEndpointFilter<E extends ExposableEndpoint<?>> implements EndpointFilter<E> {

	private final Class<E> endpointType;

	private final EndpointPatterns include;

	private final EndpointPatterns defaultIncludes;

	private final EndpointPatterns exclude;

	/**
	 * Create a new {@link IncludeExcludeEndpointFilter} with include/exclude rules bound
	 * from the {@link Environment}.
	 * @param endpointType the endpoint type that should be considered (other types always
	 * match)
	 * @param environment the environment containing the properties
	 * @param prefix the property prefix to bind
	 * @param defaultIncludes the default {@code includes} to use when none are specified.
	 */
	public IncludeExcludeEndpointFilter(Class<E> endpointType, Environment environment, String prefix,
			String... defaultIncludes) {
		this(endpointType, environment, prefix, new EndpointPatterns(defaultIncludes));
	}

	/**
	 * Create a new {@link IncludeExcludeEndpointFilter} with specific include/exclude
	 * rules.
	 * @param endpointType the endpoint type that should be considered (other types always
	 * match)
	 * @param include the include patterns
	 * @param exclude the exclude patterns
	 * @param defaultIncludes the default {@code includes} to use when none are specified.
	 */
	public IncludeExcludeEndpointFilter(Class<E> endpointType, Collection<String> include, Collection<String> exclude,
			String... defaultIncludes) {
		this(endpointType, include, exclude, new EndpointPatterns(defaultIncludes));
	}

	private IncludeExcludeEndpointFilter(Class<E> endpointType, Environment environment, String prefix,
			EndpointPatterns defaultIncludes) {
		Assert.notNull(endpointType, "EndpointType must not be null");
		Assert.notNull(environment, "Environment must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		Assert.notNull(defaultIncludes, "DefaultIncludes must not be null");
		Binder binder = Binder.get(environment);
		this.endpointType = endpointType;
		this.include = new EndpointPatterns(bind(binder, prefix + ".include"));
		this.defaultIncludes = defaultIncludes;
		this.exclude = new EndpointPatterns(bind(binder, prefix + ".exclude"));
	}

	private IncludeExcludeEndpointFilter(Class<E> endpointType, Collection<String> include, Collection<String> exclude,
			EndpointPatterns defaultIncludes) {
		Assert.notNull(endpointType, "EndpointType Type must not be null");
		Assert.notNull(defaultIncludes, "DefaultIncludes must not be null");
		this.endpointType = endpointType;
		this.include = new EndpointPatterns(include);
		this.defaultIncludes = defaultIncludes;
		this.exclude = new EndpointPatterns(exclude);
	}

	private List<String> bind(Binder binder, String name) {
		return binder.bind(name, Bindable.listOf(String.class)).orElseGet(ArrayList::new);
	}

	@Override
	public boolean match(E endpoint) {
		if (!this.endpointType.isInstance(endpoint)) {
			// Leave non-matching types for other filters
			return true;
		}
		return match(endpoint.getEndpointId());
	}

	/**
	 * Return {@code true} if the filter matches.
	 * @param endpointId the endpoint ID to check
	 * @return {@code true} if the filter matches
	 * @since 2.6.0
	 */
	public final boolean match(EndpointId endpointId) {
		return isIncluded(endpointId) && !isExcluded(endpointId);
	}

	private boolean isIncluded(EndpointId endpointId) {
		if (this.include.isEmpty()) {
			return this.defaultIncludes.matches(endpointId);
		}
		return this.include.matches(endpointId);
	}

	private boolean isExcluded(EndpointId endpointId) {
		if (this.exclude.isEmpty()) {
			return false;
		}
		return this.exclude.matches(endpointId);
	}

	/**
	 * A set of endpoint patterns used to match IDs.
	 */
	private static class EndpointPatterns {

		private final boolean empty;

		private final boolean matchesAll;

		private final Set<EndpointId> endpointIds;

		EndpointPatterns(String[] patterns) {
			this((patterns != null) ? Arrays.asList(patterns) : null);
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

		boolean matches(EndpointId endpointId) {
			return this.matchesAll || this.endpointIds.contains(endpointId);
		}

	}

}
