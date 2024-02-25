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

	/**
     * Constructs a new IncludeExcludeEndpointFilter with the specified parameters.
     *
     * @param endpointType the class representing the type of endpoint
     * @param environment the environment in which the filter is being used
     * @param prefix the prefix used for binding configuration properties
     * @param defaultIncludes the default patterns to include
     * @throws IllegalArgumentException if any of the parameters are null or empty
     */
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

	/**
     * Constructs a new IncludeExcludeEndpointFilter with the specified parameters.
     * 
     * @param endpointType the type of endpoint to filter
     * @param include the collection of patterns to include
     * @param exclude the collection of patterns to exclude
     * @param defaultIncludes the default patterns to include
     * @throws IllegalArgumentException if endpointType or defaultIncludes is null
     */
    private IncludeExcludeEndpointFilter(Class<E> endpointType, Collection<String> include, Collection<String> exclude,
			EndpointPatterns defaultIncludes) {
		Assert.notNull(endpointType, "EndpointType Type must not be null");
		Assert.notNull(defaultIncludes, "DefaultIncludes must not be null");
		this.endpointType = endpointType;
		this.include = new EndpointPatterns(include);
		this.defaultIncludes = defaultIncludes;
		this.exclude = new EndpointPatterns(exclude);
	}

	/**
     * Binds the given name to a list of strings using the provided binder.
     * If the binding is successful, returns the bound list of strings.
     * If the binding fails, returns an empty ArrayList.
     *
     * @param binder the binder to use for binding
     * @param name the name to bind
     * @return the bound list of strings, or an empty ArrayList if binding fails
     */
    private List<String> bind(Binder binder, String name) {
		return binder.bind(name, Bindable.listOf(String.class)).orElseGet(ArrayList::new);
	}

	/**
     * Determines if the given endpoint matches the filter criteria.
     * 
     * @param endpoint the endpoint to be matched
     * @return true if the endpoint matches the filter criteria, false otherwise
     */
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

	/**
     * Checks if the given endpointId is included in the filter.
     * 
     * @param endpointId the endpointId to check
     * @return true if the endpointId is included, false otherwise
     */
    private boolean isIncluded(EndpointId endpointId) {
		if (this.include.isEmpty()) {
			return this.defaultIncludes.matches(endpointId);
		}
		return this.include.matches(endpointId);
	}

	/**
     * Checks if the given endpointId is excluded based on the exclude pattern.
     * 
     * @param endpointId the endpointId to check
     * @return true if the endpointId is excluded, false otherwise
     */
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

		/**
         * Constructs a new instance of the EndpointPatterns class using the provided array of patterns.
         * 
         * @param patterns an array of patterns to be used for constructing the EndpointPatterns object
         */
        EndpointPatterns(String[] patterns) {
			this((patterns != null) ? Arrays.asList(patterns) : null);
		}

		/**
         * Constructs an EndpointPatterns object with the given collection of patterns.
         * 
         * @param patterns the collection of patterns to be used for constructing the EndpointPatterns object
         */
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

		/**
         * Returns a boolean value indicating whether the endpoint patterns are empty.
         * 
         * @return {@code true} if the endpoint patterns are empty, {@code false} otherwise.
         */
        boolean isEmpty() {
			return this.empty;
		}

		/**
         * Checks if the given endpointId matches the patterns.
         * 
         * @param endpointId the endpointId to be checked
         * @return true if the endpointId matches the patterns, false otherwise
         */
        boolean matches(EndpointId endpointId) {
			return this.matchesAll || this.endpointIds.contains(endpointId);
		}

	}

}
