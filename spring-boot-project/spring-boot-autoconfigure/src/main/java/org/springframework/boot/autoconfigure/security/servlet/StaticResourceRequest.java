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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Used to create a {@link RequestMatcher} for static resources in commonly used
 * locations. Returned by {@link PathRequest#toStaticResources()}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see PathRequest
 */
public final class StaticResourceRequest {

	static final StaticResourceRequest INSTANCE = new StaticResourceRequest();

	/**
     * Private constructor for the StaticResourceRequest class.
     * This constructor is used to prevent the instantiation of the class.
     */
    private StaticResourceRequest() {
	}

	/**
	 * Returns a matcher that includes all commonly used {@link StaticResourceLocation
	 * Locations}. The
	 * {@link StaticResourceRequestMatcher#excluding(StaticResourceLocation, StaticResourceLocation...)
	 * excluding} method can be used to remove specific locations if required. For
	 * example: <pre class="code">
	 * PathRequest.toStaticResources().atCommonLocations().excluding(StaticResourceLocation.CSS)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public StaticResourceRequestMatcher atCommonLocations() {
		return at(EnumSet.allOf(StaticResourceLocation.class));
	}

	/**
	 * Returns a matcher that includes the specified {@link StaticResourceLocation
	 * Locations}. For example: <pre class="code">
	 * PathRequest.toStaticResources().at(StaticResourceLocation.CSS, StaticResourceLocation.JAVA_SCRIPT)
	 * </pre>
	 * @param first the first location to include
	 * @param rest additional locations to include
	 * @return the configured {@link RequestMatcher}
	 */
	public StaticResourceRequestMatcher at(StaticResourceLocation first, StaticResourceLocation... rest) {
		return at(EnumSet.of(first, rest));
	}

	/**
	 * Returns a matcher that includes the specified {@link StaticResourceLocation
	 * Locations}. For example: <pre class="code">
	 * PathRequest.toStaticResources().at(locations)
	 * </pre>
	 * @param locations the locations to include
	 * @return the configured {@link RequestMatcher}
	 */
	public StaticResourceRequestMatcher at(Set<StaticResourceLocation> locations) {
		Assert.notNull(locations, "Locations must not be null");
		return new StaticResourceRequestMatcher(new LinkedHashSet<>(locations));
	}

	/**
	 * The request matcher used to match against resource {@link StaticResourceLocation
	 * Locations}.
	 */
	public static final class StaticResourceRequestMatcher
			extends ApplicationContextRequestMatcher<DispatcherServletPath> {

		private final Set<StaticResourceLocation> locations;

		private volatile RequestMatcher delegate;

		/**
         * Constructs a new StaticResourceRequestMatcher with the specified set of static resource locations.
         * 
         * @param locations the set of static resource locations to be used for matching requests
         */
        private StaticResourceRequestMatcher(Set<StaticResourceLocation> locations) {
			super(DispatcherServletPath.class);
			this.locations = locations;
		}

		/**
		 * Return a new {@link StaticResourceRequestMatcher} based on this one but
		 * excluding the specified locations.
		 * @param first the first location to exclude
		 * @param rest additional locations to exclude
		 * @return a new {@link StaticResourceRequestMatcher}
		 */
		public StaticResourceRequestMatcher excluding(StaticResourceLocation first, StaticResourceLocation... rest) {
			return excluding(EnumSet.of(first, rest));
		}

		/**
		 * Return a new {@link StaticResourceRequestMatcher} based on this one but
		 * excluding the specified locations.
		 * @param locations the locations to exclude
		 * @return a new {@link StaticResourceRequestMatcher}
		 */
		public StaticResourceRequestMatcher excluding(Set<StaticResourceLocation> locations) {
			Assert.notNull(locations, "Locations must not be null");
			Set<StaticResourceLocation> subset = new LinkedHashSet<>(this.locations);
			subset.removeAll(locations);
			return new StaticResourceRequestMatcher(subset);
		}

		/**
         * Initializes the StaticResourceRequestMatcher with the given dispatcherServletPath.
         * 
         * @param dispatcherServletPath the supplier of the DispatcherServletPath
         */
        @Override
		protected void initialized(Supplier<DispatcherServletPath> dispatcherServletPath) {
			this.delegate = new OrRequestMatcher(getDelegateMatchers(dispatcherServletPath.get()).toList());
		}

		/**
         * Returns a stream of RequestMatchers based on the given DispatcherServletPath.
         *
         * @param dispatcherServletPath the DispatcherServletPath to generate RequestMatchers from
         * @return a stream of RequestMatchers
         */
        private Stream<RequestMatcher> getDelegateMatchers(DispatcherServletPath dispatcherServletPath) {
			return getPatterns(dispatcherServletPath).map(AntPathRequestMatcher::new);
		}

		/**
         * Returns a stream of patterns for static resource locations based on the given dispatcher servlet path.
         *
         * @param dispatcherServletPath the dispatcher servlet path to be used for generating relative paths
         * @return a stream of patterns for static resource locations
         */
        private Stream<String> getPatterns(DispatcherServletPath dispatcherServletPath) {
			return this.locations.stream()
				.flatMap(StaticResourceLocation::getPatterns)
				.map(dispatcherServletPath::getRelativePath);
		}

		/**
         * Determines whether to ignore the given WebApplicationContext based on its server namespace.
         * 
         * @param applicationContext the WebApplicationContext to check
         * @return true if the WebApplicationContext should be ignored, false otherwise
         */
        @Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext) {
			return WebServerApplicationContext.hasServerNamespace(applicationContext, "management");
		}

		/**
         * Determines if the given HttpServletRequest matches the request pattern of this StaticResourceRequestMatcher.
         * 
         * @param request the HttpServletRequest to be matched
         * @param context a Supplier providing the DispatcherServletPath context
         * @return true if the request matches the pattern, false otherwise
         */
        @Override
		protected boolean matches(HttpServletRequest request, Supplier<DispatcherServletPath> context) {
			return this.delegate.matches(request);
		}

	}

}
