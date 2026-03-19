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

package org.springframework.boot.grpc.server.autoconfigure.security.web.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.security.web.servlet.ApplicationContextRequestMatcher;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Factory that can be used to create a {@link RequestMatcher} to match against gRPC
 * service locations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.1.0
 */
public final class GrpcRequest {

	private static final RequestMatcher EMPTY_MATCHER = (request) -> false;

	private static final GrpcServletRequestMatcher TO_ANY_SERVICE = new GrpcServletRequestMatcher(
			Collections.emptySet());

	private GrpcRequest() {
	}

	/**
	 * Returns a matcher that includes all gRPC services. The
	 * {@link GrpcServletRequestMatcher#excluding(String...) excluding} method can be used
	 * to remove specific services by name if required. For example:
	 *
	 * <pre class="code">
	 * GrpcServletRequest.toAnyService().excluding("my-service")
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static GrpcServletRequestMatcher toAnyService() {
		return TO_ANY_SERVICE;
	}

	/**
	 * The matcher used to match against service locations.
	 */
	public static final class GrpcServletRequestMatcher
			extends ApplicationContextRequestMatcher<GrpcServiceDiscoverer> {

		private final Set<String> excludes;

		private volatile @Nullable RequestMatcher delegate;

		private GrpcServletRequestMatcher(Set<String> exclusions) {
			super(GrpcServiceDiscoverer.class);
			this.excludes = exclusions;
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param services additional services to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcServletRequestMatcher excluding(String... services) {
			return excluding(Set.of(services));
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param services additional service names to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcServletRequestMatcher excluding(Collection<String> services) {
			Assert.notNull(services, "'services' must not be null");
			Set<String> excludes = new LinkedHashSet<>(this.excludes);
			excludes.addAll(services);
			return new GrpcServletRequestMatcher(excludes);
		}

		@Override
		protected void initialized(Supplier<GrpcServiceDiscoverer> context) {
			this.delegate = createDelegate(context.get());
		}

		private @Nullable RequestMatcher createDelegate(GrpcServiceDiscoverer grpcServiceDiscoverer) {
			List<RequestMatcher> delegateMatchers = getDelegateMatchers(grpcServiceDiscoverer);
			return (!CollectionUtils.isEmpty(delegateMatchers)) ? new OrRequestMatcher(delegateMatchers)
					: EMPTY_MATCHER;
		}

		private List<RequestMatcher> getDelegateMatchers(GrpcServiceDiscoverer serviceDiscoverer) {
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

		private RequestMatcher getDelegateMatcher(String path) {
			Assert.hasText(path, "'path' must not be empty");
			return PathPatternRequestMatcher.withDefaults().matcher(path);
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<GrpcServiceDiscoverer> context) {
			Assert.state(this.delegate != null, "'delegate' must not be null");
			return this.delegate.matches(request);
		}

	}

}
