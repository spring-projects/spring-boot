/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure.security;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.boot.grpc.server.autoconfigure.security.GrpcServletRequest.GrpcServletRequestMatcher;
import org.springframework.boot.security.reactive.ApplicationContextServerWebExchangeMatcher;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Factory for a request matcher used to match against resource locations for gRPC
 * services.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
public final class GrpcReactiveRequest {

	private GrpcReactiveRequest() {
	}

	/**
	 * Returns a matcher that includes all gRPC services from the application context. The
	 * {@link GrpcReactiveRequestMatcher#excluding(String...) excluding} method can be
	 * used to remove specific services by name if required. For example:
	 *
	 * <pre class="code">
	 * GrpcReactiveRequest.all().excluding("my-service")
	 * </pre>
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public static GrpcReactiveRequestMatcher all() {
		return new GrpcReactiveRequestMatcher();
	}

	/**
	 * The request matcher used to match against resource locations.
	 */
	public static final class GrpcReactiveRequestMatcher
			extends ApplicationContextServerWebExchangeMatcher<GrpcServiceDiscoverer> {

		private final Set<String> exclusions;

		private volatile ServerWebExchangeMatcher delegate;

		private GrpcReactiveRequestMatcher() {
			this(new HashSet<>());
		}

		private GrpcReactiveRequestMatcher(Set<String> exclusions) {
			super(GrpcServiceDiscoverer.class);
			this.exclusions = exclusions;
			this.delegate = (request) -> MatchResult.notMatch();
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param rest additional services to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcReactiveRequestMatcher excluding(String... rest) {
			return excluding(Set.of(rest));
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param exclusions additional service names to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcReactiveRequestMatcher excluding(Set<String> exclusions) {
			Assert.notNull(exclusions, "Exclusions must not be null");
			Set<String> subset = new LinkedHashSet<>(this.exclusions);
			subset.addAll(exclusions);
			return new GrpcReactiveRequestMatcher(subset);
		}

		@Override
		protected void initialized(Supplier<GrpcServiceDiscoverer> context) {
			List<ServerWebExchangeMatcher> matchers = getDelegateMatchers(context.get()).toList();
			this.delegate = matchers.isEmpty() ? (request) -> MatchResult.notMatch()
					: new OrServerWebExchangeMatcher(matchers);
		}

		private Stream<ServerWebExchangeMatcher> getDelegateMatchers(GrpcServiceDiscoverer context) {
			return getPatterns(context).map(PathPatternParserServerWebExchangeMatcher::new);
		}

		private Stream<String> getPatterns(GrpcServiceDiscoverer context) {
			return context.listServiceNames()
				.stream()
				.filter((service) -> !this.exclusions.stream().anyMatch((type) -> type.equals(service)))
				.map((service) -> "/" + service + "/**");
		}

		@Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<GrpcServiceDiscoverer> context) {
			return this.delegate.matches(exchange);
		}

	}

}
