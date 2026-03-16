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

package org.springframework.boot.grpc.server.autoconfigure.security.web.reactive;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.boot.grpc.server.autoconfigure.security.web.servlet.GrpcRequest.GrpcServletRequestMatcher;
import org.springframework.boot.security.web.reactive.ApplicationContextServerWebExchangeMatcher;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Factory that can be used to create a {@link ServerWebExchangeMatcher} to match against
 * gRPC service locations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.1.0
 */
public final class GrpcRequest {

	private GrpcRequest() {
	}

	/**
	 * Returns a matcher that includes all gRPC services. The
	 * {@link GrpcReactiveRequestMatcher#excluding(String...) excluding} method can be
	 * used to remove specific services by name if required. For example:
	 *
	 * <pre class="code">
	 * GrpcReactiveRequest.toAnyService().excluding("my-service")
	 * </pre>
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public static GrpcReactiveRequestMatcher toAnyService() {
		return new GrpcReactiveRequestMatcher(Collections.emptySet());
	}

	/**
	 * The matcher used to match against service locations.
	 */
	public static final class GrpcReactiveRequestMatcher
			extends ApplicationContextServerWebExchangeMatcher<GrpcServiceDiscoverer> {

		private static final ServerWebExchangeMatcher EMPTY_MATCHER = (exchange) -> MatchResult.notMatch();

		private final Set<String> excludes;

		private volatile @Nullable ServerWebExchangeMatcher delegate;

		private GrpcReactiveRequestMatcher(Set<String> excludes) {
			super(GrpcServiceDiscoverer.class);
			this.excludes = excludes;
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param services additional services to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcReactiveRequestMatcher excluding(String... services) {
			return excluding(Set.of(services));
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param services additional service names to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcReactiveRequestMatcher excluding(Collection<String> services) {
			Assert.notNull(services, "'services' must not be null");
			Set<String> excludes = new LinkedHashSet<>(this.excludes);
			excludes.addAll(services);
			return new GrpcReactiveRequestMatcher(excludes);
		}

		@Override
		protected void initialized(Supplier<GrpcServiceDiscoverer> context) {
			this.delegate = createDelegate(context.get());

		}

		private ServerWebExchangeMatcher createDelegate(GrpcServiceDiscoverer serviceDiscoverer) {
			List<ServerWebExchangeMatcher> delegateMatchers = getDelegateMatchers(serviceDiscoverer);
			return (!CollectionUtils.isEmpty(delegateMatchers)) ? new OrServerWebExchangeMatcher(delegateMatchers)
					: EMPTY_MATCHER;
		}

		private List<ServerWebExchangeMatcher> getDelegateMatchers(GrpcServiceDiscoverer serviceDiscoverer) {
			return getPatterns(serviceDiscoverer).map(this::getDelegateMatcher).toList();
		}

		private Stream<String> getPatterns(GrpcServiceDiscoverer serviceDiscoverer) {
			return serviceDiscoverer.listServiceNames().stream().filter(this::isExcluded).map(this::getPath);
		}

		private boolean isExcluded(String service) {
			return !this.excludes.stream().anyMatch((candidate) -> candidate.equals(service));
		}

		private String getPath(String service) {
			return "/" + service + "/**";
		}

		private ServerWebExchangeMatcher getDelegateMatcher(String path) {
			Assert.hasText(path, "'path' must not be empty");
			return new PathPatternParserServerWebExchangeMatcher(path);
		}

		@Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<GrpcServiceDiscoverer> context) {
			Assert.state(this.delegate != null, "'delegate' must not be null");
			return this.delegate.matches(exchange);
		}

	}

}
