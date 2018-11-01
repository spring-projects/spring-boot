/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

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
	public StaticResourceRequestMatcher at(StaticResourceLocation first,
			StaticResourceLocation... rest) {
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
		public StaticResourceRequestMatcher excluding(StaticResourceLocation first,
				StaticResourceLocation... rest) {
			return excluding(EnumSet.of(first, rest));
		}

		/**
		 * Return a new {@link StaticResourceRequestMatcher} based on this one but
		 * excluding the specified locations.
		 * @param locations the locations to exclude
		 * @return a new {@link StaticResourceRequestMatcher}
		 */
		public StaticResourceRequestMatcher excluding(
				Set<StaticResourceLocation> locations) {
			Assert.notNull(locations, "Locations must not be null");
			Set<StaticResourceLocation> subset = new LinkedHashSet<>(this.locations);
			subset.removeAll(locations);
			return new StaticResourceRequestMatcher(subset);
		}

		@Override
		protected void initialized(
				Supplier<DispatcherServletPath> dispatcherServletPath) {
			this.delegate = new OrRequestMatcher(
					getDelegateMatchers(dispatcherServletPath.get()));
		}

		private List<RequestMatcher> getDelegateMatchers(
				DispatcherServletPath dispatcherServletPath) {
			return getPatterns(dispatcherServletPath).map(AntPathRequestMatcher::new)
					.collect(Collectors.toList());
		}

		private Stream<String> getPatterns(DispatcherServletPath dispatcherServletPath) {
			return this.locations.stream().flatMap(StaticResourceLocation::getPatterns)
					.map(dispatcherServletPath::getRelativePath);
		}

		@Override
		protected boolean matches(HttpServletRequest request,
				Supplier<DispatcherServletPath> context) {
			return this.delegate.matches(request);
		}

	}

}
