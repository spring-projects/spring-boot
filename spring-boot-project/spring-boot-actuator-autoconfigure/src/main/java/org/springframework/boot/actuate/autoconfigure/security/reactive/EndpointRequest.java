/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.security.reactive.ApplicationContextServerWebExchangeMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Factory that can be used to create a {@link ServerWebExchangeMatcher} for actuator
 * endpoint locations.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public final class EndpointRequest {

	private static final ServerWebExchangeMatcher EMPTY_MATCHER = (request) -> MatchResult.notMatch();

	private EndpointRequest() {
	}

	/**
	 * Returns a matcher that includes all {@link Endpoint actuator endpoints}. It also
	 * includes the links endpoint which is present at the base path of the actuator
	 * endpoints. The {@link EndpointServerWebExchangeMatcher#excluding(Class...)
	 * excluding} method can be used to further remove specific endpoints if required. For
	 * example: <pre class="code">
	 * EndpointRequest.toAnyEndpoint().excluding(ShutdownEndpoint.class)
	 * </pre>
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public static EndpointServerWebExchangeMatcher toAnyEndpoint() {
		return new EndpointServerWebExchangeMatcher(true);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to(ShutdownEndpoint.class, HealthEndpoint.class)
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public static EndpointServerWebExchangeMatcher to(Class<?>... endpoints) {
		return new EndpointServerWebExchangeMatcher(endpoints, false);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to("shutdown", "health")
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public static EndpointServerWebExchangeMatcher to(String... endpoints) {
		return new EndpointServerWebExchangeMatcher(endpoints, false);
	}

	/**
	 * Returns a matcher that matches only on the links endpoint. It can be used when
	 * security configuration for the links endpoint is different from the other
	 * {@link Endpoint actuator endpoints}. The
	 * {@link EndpointServerWebExchangeMatcher#excludingLinks() excludingLinks} method can
	 * be used in combination with this to remove the links endpoint from
	 * {@link EndpointRequest#toAnyEndpoint() toAnyEndpoint}. For example:
	 * <pre class="code">
	 * EndpointRequest.toLinks()
	 * </pre>
	 * @return the configured {@link ServerWebExchangeMatcher}
	 */
	public static LinksServerWebExchangeMatcher toLinks() {
		return new LinksServerWebExchangeMatcher();
	}

	/**
	 * The {@link ServerWebExchangeMatcher} used to match against {@link Endpoint actuator
	 * endpoints}.
	 */
	public static final class EndpointServerWebExchangeMatcher
			extends ApplicationContextServerWebExchangeMatcher<PathMappedEndpoints> {

		private final List<Object> includes;

		private final List<Object> excludes;

		private final boolean includeLinks;

		private volatile ServerWebExchangeMatcher delegate;

		private EndpointServerWebExchangeMatcher(boolean includeLinks) {
			this(Collections.emptyList(), Collections.emptyList(), includeLinks);
		}

		private EndpointServerWebExchangeMatcher(Class<?>[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks);
		}

		private EndpointServerWebExchangeMatcher(String[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks);
		}

		private EndpointServerWebExchangeMatcher(List<Object> includes, List<Object> excludes, boolean includeLinks) {
			super(PathMappedEndpoints.class);
			this.includes = includes;
			this.excludes = excludes;
			this.includeLinks = includeLinks;
		}

		public EndpointServerWebExchangeMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointServerWebExchangeMatcher(this.includes, excludes, this.includeLinks);
		}

		public EndpointServerWebExchangeMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointServerWebExchangeMatcher(this.includes, excludes, this.includeLinks);
		}

		public EndpointServerWebExchangeMatcher excludingLinks() {
			return new EndpointServerWebExchangeMatcher(this.includes, this.excludes, false);
		}

		@Override
		protected void initialized(Supplier<PathMappedEndpoints> pathMappedEndpoints) {
			this.delegate = createDelegate(pathMappedEndpoints);
		}

		private ServerWebExchangeMatcher createDelegate(Supplier<PathMappedEndpoints> pathMappedEndpoints) {
			try {
				return createDelegate(pathMappedEndpoints.get());
			}
			catch (NoSuchBeanDefinitionException ex) {
				return EMPTY_MATCHER;
			}
		}

		private ServerWebExchangeMatcher createDelegate(PathMappedEndpoints pathMappedEndpoints) {
			Set<String> paths = new LinkedHashSet<>();
			if (this.includes.isEmpty()) {
				paths.addAll(pathMappedEndpoints.getAllPaths());
			}
			streamPaths(this.includes, pathMappedEndpoints).forEach(paths::add);
			streamPaths(this.excludes, pathMappedEndpoints).forEach(paths::remove);
			List<ServerWebExchangeMatcher> delegateMatchers = getDelegateMatchers(paths);
			if (this.includeLinks && StringUtils.hasText(pathMappedEndpoints.getBasePath())) {
				delegateMatchers.add(new PathPatternParserServerWebExchangeMatcher(pathMappedEndpoints.getBasePath()));
			}
			return new OrServerWebExchangeMatcher(delegateMatchers);
		}

		private Stream<String> streamPaths(List<Object> source, PathMappedEndpoints pathMappedEndpoints) {
			return source.stream().filter(Objects::nonNull).map(this::getEndpointId).map(pathMappedEndpoints::getPath);
		}

		private EndpointId getEndpointId(Object source) {
			if (source instanceof EndpointId) {
				return (EndpointId) source;
			}
			if (source instanceof String) {
				return (EndpointId.of((String) source));
			}
			if (source instanceof Class) {
				return getEndpointId((Class<?>) source);
			}
			throw new IllegalStateException("Unsupported source " + source);
		}

		private EndpointId getEndpointId(Class<?> source) {
			Endpoint annotation = AnnotatedElementUtils.getMergedAnnotation(source, Endpoint.class);
			Assert.state(annotation != null, () -> "Class " + source + " is not annotated with @Endpoint");
			return EndpointId.of(annotation.id());
		}

		private List<ServerWebExchangeMatcher> getDelegateMatchers(Set<String> paths) {
			return paths.stream().map((path) -> new PathPatternParserServerWebExchangeMatcher(path + "/**"))
					.collect(Collectors.toList());
		}

		@Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<PathMappedEndpoints> context) {
			return this.delegate.matches(exchange);
		}

	}

	/**
	 * The {@link ServerWebExchangeMatcher} used to match against the links endpoint.
	 */
	public static final class LinksServerWebExchangeMatcher
			extends ApplicationContextServerWebExchangeMatcher<WebEndpointProperties> {

		private volatile ServerWebExchangeMatcher delegate;

		private LinksServerWebExchangeMatcher() {
			super(WebEndpointProperties.class);
		}

		@Override
		protected void initialized(Supplier<WebEndpointProperties> properties) {
			this.delegate = createDelegate(properties.get());
		}

		private ServerWebExchangeMatcher createDelegate(WebEndpointProperties properties) {
			if (StringUtils.hasText(properties.getBasePath())) {
				return new PathPatternParserServerWebExchangeMatcher(properties.getBasePath());
			}
			return EMPTY_MATCHER;
		}

		@Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<WebEndpointProperties> context) {
			return this.delegate.matches(exchange);
		}

	}

}
