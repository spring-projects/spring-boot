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

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Factory for a request matcher used to match against resource locations for gRPC
 * services.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
public final class GrpcServletRequest {

	private GrpcServletRequest() {
	}

	/**
	 * Returns a matcher that includes all gRPC services from the application context. The
	 * {@link GrpcServletRequestMatcher#excluding(String...) excluding} method can be used
	 * to remove specific services by name if required. For example:
	 *
	 * <pre class="code">
	 * GrpcServletRequest.all().excluding("my-service")
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static GrpcServletRequestMatcher all() {
		return new GrpcServletRequestMatcher();
	}

	/**
	 * The request matcher used to match against resource locations.
	 */
	public static final class GrpcServletRequestMatcher
			extends ApplicationContextRequestMatcher<GrpcServiceDiscoverer> {

		private final Set<String> exclusions;

		private volatile RequestMatcher delegate;

		private GrpcServletRequestMatcher() {
			this(new HashSet<>());
		}

		private GrpcServletRequestMatcher(Set<String> exclusions) {
			super(GrpcServiceDiscoverer.class);
			this.exclusions = exclusions;
			this.delegate = (request) -> false;
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param rest additional services to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcServletRequestMatcher excluding(String... rest) {
			return excluding(Set.of(rest));
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param exclusions additional service names to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcServletRequestMatcher excluding(Set<String> exclusions) {
			Assert.notNull(exclusions, "Exclusions must not be null");
			Set<String> subset = new LinkedHashSet<>(this.exclusions);
			subset.addAll(exclusions);
			return new GrpcServletRequestMatcher(subset);
		}

		@Override
		protected void initialized(Supplier<GrpcServiceDiscoverer> context) {
			List<RequestMatcher> matchers = getDelegateMatchers(context.get()).toList();
			this.delegate = matchers.isEmpty() ? (request) -> false : new OrRequestMatcher(matchers);
		}

		@Override
		protected boolean ignoreApplicationContext(WebApplicationContext context) {
			return context.getBeanNamesForType(GrpcServiceDiscoverer.class).length != 1;
		}

		private Stream<RequestMatcher> getDelegateMatchers(GrpcServiceDiscoverer context) {
			return getPatterns(context).map((path) -> {
				Assert.hasText(path, "Path must not be empty");
				return PathPatternRequestMatcher.withDefaults().matcher(path);
			});
		}

		private Stream<String> getPatterns(GrpcServiceDiscoverer context) {
			return context.listServiceNames()
				.stream()
				.filter((service) -> !this.exclusions.stream().anyMatch((type) -> type.equals(service)))
				.map((service) -> "/" + service + "/**");
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<GrpcServiceDiscoverer> context) {
			return this.delegate.matches(request);
		}

	}

}
