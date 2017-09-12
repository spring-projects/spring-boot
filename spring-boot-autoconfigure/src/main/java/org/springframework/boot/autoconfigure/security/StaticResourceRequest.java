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

package org.springframework.boot.autoconfigure.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.security.ApplicationContextRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

/**
 * Factory that can be used to create a {@link RequestMatcher} for static resources in
 * commonly used locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class StaticResourceRequest {

	private StaticResourceRequest() {
	}

	/**
	 * Returns a matcher that includes all commonly used {@link Location Locations}. The
	 * {@link StaticResourceRequestMatcher#excluding(Location, Location...) excluding}
	 * method can be used to remove specific locations if required. For example:
	 * <pre class="code">
	 * StaticResourceRequest.toCommonLocations().excluding(Location.CSS)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static StaticResourceRequestMatcher toCommonLocations() {
		return to(EnumSet.allOf(Location.class));
	}

	/**
	 * Returns a matcher that includes the specified {@link Location Locations}. For
	 * example: <pre class="code">
	 * StaticResourceRequest.to(Location.CSS, Location.JAVA_SCRIPT)
	 * </pre>
	 * @param first the first location to include
	 * @param rest additional locations to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static StaticResourceRequestMatcher to(Location first, Location... rest) {
		return to(EnumSet.of(first, rest));
	}

	/**
	 * Returns a matcher that includes the specified {@link Location Locations}. For
	 * example: <pre class="code">
	 * StaticResourceRequest.to(locations)
	 * </pre>
	 * @param locations the locations to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static StaticResourceRequestMatcher to(Set<Location> locations) {
		Assert.notNull(locations, "Locations must not be null");
		return new StaticResourceRequestMatcher(new LinkedHashSet<>(locations));
	}

	public enum Location {

		/**
		 * Resources under {@code "/css"}.
		 */
		CSS("/css/**"),

		/**
		 * Resources under {@code "/js"}.
		 */
		JAVA_SCRIPT("/js/**"),

		/**
		 * Resources under {@code "/images"}.
		 */
		IMAGES("/images/**"),

		/**
		 * Resources under {@code "/webjars"}.
		 */
		WEB_JARS("/webjars/**"),

		/**
		 * The {@code "favicon.ico"} resource.
		 */
		FAVICON("/**/favicon.ico");

		private String[] patterns;

		Location(String... patterns) {
			this.patterns = patterns;
		}

		Stream<String> getPatterns() {
			return Arrays.stream(this.patterns);
		}

	}

	/**
	 * The request matcher used to match against resource {@link Location Locations}.
	 */
	public final static class StaticResourceRequestMatcher
			extends ApplicationContextRequestMatcher<ServerProperties> {

		private final Set<Location> locations;

		private RequestMatcher delegate;

		private StaticResourceRequestMatcher(Set<Location> locations) {
			super(ServerProperties.class);
			this.locations = locations;
		}

		/**
		 * Return a new {@link StaticResourceRequestMatcher} based on this one but
		 * excluding the specified locations.
		 * @param first the first location to exclude
		 * @param rest additional locations to exclude
		 * @return a new {@link StaticResourceRequestMatcher}
		 */
		public StaticResourceRequestMatcher excluding(Location first, Location... rest) {
			return excluding(EnumSet.of(first, rest));
		}

		/**
		 * Return a new {@link StaticResourceRequestMatcher} based on this one but
		 * excluding the specified locations.
		 * @param locations the locations to exclude
		 * @return a new {@link StaticResourceRequestMatcher}
		 */
		public StaticResourceRequestMatcher excluding(Set<Location> locations) {
			Assert.notNull(locations, "Locations must not be null");
			Set<Location> subset = new LinkedHashSet<>(this.locations);
			subset.removeAll(locations);
			return new StaticResourceRequestMatcher(subset);
		}

		@Override
		protected void initialized(ServerProperties serverProperties) {
			this.delegate = new OrRequestMatcher(getDelegateMatchers(serverProperties));
		}

		private List<RequestMatcher> getDelegateMatchers(
				ServerProperties serverProperties) {
			return getPatterns(serverProperties).map(AntPathRequestMatcher::new)
					.collect(Collectors.toList());
		}

		private Stream<String> getPatterns(ServerProperties serverProperties) {
			return this.locations.stream().flatMap(Location::getPatterns)
					.map(serverProperties.getServlet()::getPath);
		}

		@Override
		protected boolean matches(HttpServletRequest request, ServerProperties context) {
			return this.delegate.matches(request);
		}

	}

}
