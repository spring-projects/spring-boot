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

package org.springframework.boot.autoconfigure.security.reactive;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Used to create a {@link ServerWebExchangeMatcher} for static resources in commonly used
 * locations. Returned by {@link PathRequest#toStaticResources()}.
 *
 * @author Madhura Bhave
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
	 * {@link StaticResourceServerWebExchange#excluding(StaticResourceLocation, StaticResourceLocation...)
	 * excluding} method can be used to remove specific locations if required. For
	 * example: <pre class="code">
	 * PathRequest.toStaticResources().atCommonLocations().excluding(StaticResourceLocation.CSS)
	 * </pre>
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public StaticResourceServerWebExchange atCommonLocations() {
		return at(EnumSet.allOf(StaticResourceLocation.class));
	}

	/**
	 * Returns a matcher that includes the specified {@link StaticResourceLocation
	 * Locations}. For example: <pre class="code">
	 * PathRequest.toStaticResources().at(StaticResourceLocation.CSS, StaticResourceLocation.JAVA_SCRIPT)
	 * </pre>
	 * @param first the first location to include
	 * @param rest additional locations to include
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public StaticResourceServerWebExchange at(StaticResourceLocation first, StaticResourceLocation... rest) {
		return at(EnumSet.of(first, rest));
	}

	/**
	 * Returns a matcher that includes the specified {@link StaticResourceLocation
	 * Locations}. For example: <pre class="code">
	 * PathRequest.toStaticResources().at(locations)
	 * </pre>
	 * @param locations the locations to include
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public StaticResourceServerWebExchange at(Set<StaticResourceLocation> locations) {
		Assert.notNull(locations, "Locations must not be null");
		return new StaticResourceServerWebExchange(new LinkedHashSet<>(locations));
	}

	/**
	 * The server web exchange matcher used to match against resource
	 * {@link StaticResourceLocation locations}.
	 */
	public static final class StaticResourceServerWebExchange implements ServerWebExchangeMatcher {

		private final Set<StaticResourceLocation> locations;

		private StaticResourceServerWebExchange(Set<StaticResourceLocation> locations) {
			this.locations = locations;
		}

		/**
		 * Return a new {@link StaticResourceServerWebExchange} based on this one but
		 * excluding the specified locations.
		 * @param first the first location to exclude
		 * @param rest additional locations to exclude
		 * @return a new {@link StaticResourceServerWebExchange}
		 */
		public StaticResourceServerWebExchange excluding(StaticResourceLocation first, StaticResourceLocation... rest) {
			return excluding(EnumSet.of(first, rest));
		}

		/**
		 * Return a new {@link StaticResourceServerWebExchange} based on this one but
		 * excluding the specified locations.
		 * @param locations the locations to exclude
		 * @return a new {@link StaticResourceServerWebExchange}
		 */
		public StaticResourceServerWebExchange excluding(Set<StaticResourceLocation> locations) {
			Assert.notNull(locations, "Locations must not be null");
			Set<StaticResourceLocation> subset = new LinkedHashSet<>(this.locations);
			subset.removeAll(locations);
			return new StaticResourceServerWebExchange(subset);
		}

		private List<ServerWebExchangeMatcher> getDelegateMatchers() {
			return getPatterns().map(PathPatternParserServerWebExchangeMatcher::new).collect(Collectors.toList());
		}

		private Stream<String> getPatterns() {
			return this.locations.stream().flatMap(StaticResourceLocation::getPatterns)
					.map((pattern) -> pattern.replace("/**/", "/*/"));
		}

		@Override
		public Mono<MatchResult> matches(ServerWebExchange exchange) {
			OrServerWebExchangeMatcher matcher = new OrServerWebExchangeMatcher(getDelegateMatchers());
			return matcher.matches(exchange);
		}

	}

}
