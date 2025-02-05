/*
 * Copyright 2012-2025 the original author or authors.
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
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.security.reactive.ApplicationContextServerWebExchangeMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Factory that can be used to create a {@link ServerWebExchangeMatcher} for actuator
 * endpoint locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Chris Bono
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
	 * Returns a matcher that includes additional paths under a {@link WebServerNamespace}
	 * for the specified {@link Endpoint actuator endpoints}. For example:
	 * <pre class="code">
	 * EndpointRequest.toAdditionalPaths(WebServerNamespace.SERVER, "health")
	 * </pre>
	 * @param webServerNamespace the web server namespace
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 * @since 3.4.0
	 */
	public static AdditionalPathsEndpointServerWebExchangeMatcher toAdditionalPaths(
			WebServerNamespace webServerNamespace, Class<?>... endpoints) {
		return new AdditionalPathsEndpointServerWebExchangeMatcher(webServerNamespace, endpoints);
	}

	/**
	 * Returns a matcher that includes additional paths under a {@link WebServerNamespace}
	 * for the specified {@link Endpoint actuator endpoints}. For example:
	 * <pre class="code">
	 * EndpointRequest.toAdditionalPaths(WebServerNamespace.SERVER, HealthEndpoint.class)
	 * </pre>
	 * @param webServerNamespace the web server namespace
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 * @since 3.4.0
	 */
	public static AdditionalPathsEndpointServerWebExchangeMatcher toAdditionalPaths(
			WebServerNamespace webServerNamespace, String... endpoints) {
		return new AdditionalPathsEndpointServerWebExchangeMatcher(webServerNamespace, endpoints);
	}

	/**
	 * Base class for supported request matchers.
	 */
	private abstract static class AbstractWebExchangeMatcher<C> extends ApplicationContextServerWebExchangeMatcher<C> {

		private volatile ServerWebExchangeMatcher delegate;

		private volatile ManagementPortType managementPortType;

		AbstractWebExchangeMatcher(Class<? extends C> contextClass) {
			super(contextClass);
		}

		@Override
		protected void initialized(Supplier<C> supplier) {
			this.delegate = createDelegate(supplier);
		}

		private ServerWebExchangeMatcher createDelegate(Supplier<C> context) {
			try {
				return createDelegate(context.get());
			}
			catch (NoSuchBeanDefinitionException ex) {
				return EMPTY_MATCHER;
			}
		}

		protected abstract ServerWebExchangeMatcher createDelegate(C context);

		protected final List<ServerWebExchangeMatcher> getDelegateMatchers(Set<String> paths, HttpMethod httpMethod) {
			return paths.stream()
				.map((path) -> getDelegateMatcher(path, httpMethod))
				.collect(Collectors.toCollection(ArrayList::new));
		}

		private PathPatternParserServerWebExchangeMatcher getDelegateMatcher(String path, HttpMethod httpMethod) {
			return new PathPatternParserServerWebExchangeMatcher(path + "/**", httpMethod);
		}

		@Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<C> context) {
			return this.delegate.matches(exchange);
		}

		@Override
		protected boolean ignoreApplicationContext(ApplicationContext applicationContext) {
			ManagementPortType managementPortType = this.managementPortType;
			if (managementPortType == null) {
				managementPortType = ManagementPortType.get(applicationContext.getEnvironment());
				this.managementPortType = managementPortType;
			}
			return ignoreApplicationContext(applicationContext, managementPortType);
		}

		protected boolean ignoreApplicationContext(ApplicationContext applicationContext,
				ManagementPortType managementPortType) {
			return managementPortType == ManagementPortType.DIFFERENT
					&& !hasWebServerNamespace(applicationContext, WebServerNamespace.MANAGEMENT);
		}

		protected final boolean hasWebServerNamespace(ApplicationContext applicationContext,
				WebServerNamespace webServerNamespace) {
			if (applicationContext.getParent() == null) {
				return WebServerNamespace.SERVER.equals(webServerNamespace);
			}
			String parentContextId = applicationContext.getParent().getId();
			return applicationContext.getId().equals(parentContextId + ":" + webServerNamespace);
		}

		protected final String toString(List<Object> endpoints, String emptyValue) {
			return (!endpoints.isEmpty()) ? endpoints.stream()
				.map(this::getEndpointId)
				.map(Object::toString)
				.collect(Collectors.joining(", ", "[", "]")) : emptyValue;
		}

		protected final EndpointId getEndpointId(Object source) {
			if (source instanceof EndpointId endpointId) {
				return endpointId;
			}
			if (source instanceof String string) {
				return EndpointId.of(string);
			}
			if (source instanceof Class) {
				return getEndpointId((Class<?>) source);
			}
			throw new IllegalStateException("Unsupported source " + source);
		}

		private EndpointId getEndpointId(Class<?> source) {
			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(source).get(Endpoint.class);
			Assert.state(annotation.isPresent(), () -> "Class " + source + " is not annotated with @Endpoint");
			return EndpointId.of(annotation.getString("id"));
		}

	}

	/**
	 * The {@link ServerWebExchangeMatcher} used to match against {@link Endpoint actuator
	 * endpoints}.
	 */
	public static final class EndpointServerWebExchangeMatcher extends AbstractWebExchangeMatcher<PathMappedEndpoints> {

		private final List<Object> includes;

		private final List<Object> excludes;

		private final boolean includeLinks;

		private final HttpMethod httpMethod;

		private EndpointServerWebExchangeMatcher(boolean includeLinks) {
			this(Collections.emptyList(), Collections.emptyList(), includeLinks, null);
		}

		private EndpointServerWebExchangeMatcher(Class<?>[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks, null);
		}

		private EndpointServerWebExchangeMatcher(String[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks, null);
		}

		private EndpointServerWebExchangeMatcher(List<Object> includes, List<Object> excludes, boolean includeLinks,
				HttpMethod httpMethod) {
			super(PathMappedEndpoints.class);
			this.includes = includes;
			this.excludes = excludes;
			this.includeLinks = includeLinks;
			this.httpMethod = httpMethod;
		}

		public EndpointServerWebExchangeMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointServerWebExchangeMatcher(this.includes, excludes, this.includeLinks, null);
		}

		public EndpointServerWebExchangeMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointServerWebExchangeMatcher(this.includes, excludes, this.includeLinks, null);
		}

		public EndpointServerWebExchangeMatcher excludingLinks() {
			return new EndpointServerWebExchangeMatcher(this.includes, this.excludes, false, null);
		}

		/**
		 * Restricts the matcher to only consider requests with a particular http method.
		 * @param httpMethod the http method to include
		 * @return a copy of the matcher further restricted to only match requests with
		 * the specified http method
		 */
		public EndpointServerWebExchangeMatcher withHttpMethod(HttpMethod httpMethod) {
			return new EndpointServerWebExchangeMatcher(this.includes, this.excludes, this.includeLinks, httpMethod);
		}

		@Override
		protected ServerWebExchangeMatcher createDelegate(PathMappedEndpoints endpoints) {
			Set<String> paths = new LinkedHashSet<>();
			if (this.includes.isEmpty()) {
				paths.addAll(endpoints.getAllPaths());
			}
			streamPaths(this.includes, endpoints).forEach(paths::add);
			streamPaths(this.excludes, endpoints).forEach(paths::remove);
			List<ServerWebExchangeMatcher> delegateMatchers = getDelegateMatchers(paths, this.httpMethod);
			if (this.includeLinks && StringUtils.hasText(endpoints.getBasePath())) {
				delegateMatchers.add(new LinksServerWebExchangeMatcher());
			}
			return new OrServerWebExchangeMatcher(delegateMatchers);
		}

		private Stream<String> streamPaths(List<Object> source, PathMappedEndpoints endpoints) {
			return source.stream().filter(Objects::nonNull).map(this::getEndpointId).map(endpoints::getPath);
		}

		@Override
		public String toString() {
			return String.format("EndpointRequestMatcher includes=%s, excludes=%s, includeLinks=%s",
					toString(this.includes, "[*]"), toString(this.excludes, "[]"), this.includeLinks);
		}

	}

	/**
	 * The {@link ServerWebExchangeMatcher} used to match against the links endpoint.
	 */
	public static final class LinksServerWebExchangeMatcher extends AbstractWebExchangeMatcher<WebEndpointProperties> {

		private LinksServerWebExchangeMatcher() {
			super(WebEndpointProperties.class);
		}

		@Override
		protected ServerWebExchangeMatcher createDelegate(WebEndpointProperties properties) {
			if (StringUtils.hasText(properties.getBasePath())) {
				return new OrServerWebExchangeMatcher(
						new PathPatternParserServerWebExchangeMatcher(properties.getBasePath()),
						new PathPatternParserServerWebExchangeMatcher(properties.getBasePath() + "/"));
			}
			return EMPTY_MATCHER;
		}

		@Override
		public String toString() {
			return String.format("LinksServerWebExchangeMatcher");
		}

	}

	/**
	 * The {@link ServerWebExchangeMatcher} used to match against additional paths for
	 * {@link Endpoint actuator endpoints}.
	 */
	public static class AdditionalPathsEndpointServerWebExchangeMatcher
			extends AbstractWebExchangeMatcher<PathMappedEndpoints> {

		private final WebServerNamespace webServerNamespace;

		private final List<Object> endpoints;

		private final HttpMethod httpMethod;

		AdditionalPathsEndpointServerWebExchangeMatcher(WebServerNamespace webServerNamespace, String... endpoints) {
			this(webServerNamespace, Arrays.asList((Object[]) endpoints), null);
		}

		AdditionalPathsEndpointServerWebExchangeMatcher(WebServerNamespace webServerNamespace, Class<?>... endpoints) {
			this(webServerNamespace, Arrays.asList((Object[]) endpoints), null);
		}

		private AdditionalPathsEndpointServerWebExchangeMatcher(WebServerNamespace webServerNamespace,
				List<Object> endpoints, HttpMethod httpMethod) {
			super(PathMappedEndpoints.class);
			Assert.notNull(webServerNamespace, "'webServerNamespace' must not be null");
			Assert.notNull(endpoints, "'endpoints' must not be null");
			Assert.notEmpty(endpoints, "'endpoints' must not be empty");
			this.webServerNamespace = webServerNamespace;
			this.endpoints = endpoints;
			this.httpMethod = httpMethod;
		}

		/**
		 * Restricts the matcher to only consider requests with a particular HTTP method.
		 * @param httpMethod the HTTP method to include
		 * @return a copy of the matcher further restricted to only match requests with
		 * the specified HTTP method
		 * @since 3.5.0
		 */
		public AdditionalPathsEndpointServerWebExchangeMatcher withHttpMethod(HttpMethod httpMethod) {
			return new AdditionalPathsEndpointServerWebExchangeMatcher(this.webServerNamespace, this.endpoints,
					httpMethod);
		}

		@Override
		protected boolean ignoreApplicationContext(ApplicationContext applicationContext,
				ManagementPortType managementPortType) {
			return !hasWebServerNamespace(applicationContext, this.webServerNamespace);
		}

		@Override
		protected ServerWebExchangeMatcher createDelegate(PathMappedEndpoints endpoints) {
			Set<String> paths = this.endpoints.stream()
				.filter(Objects::nonNull)
				.map(this::getEndpointId)
				.flatMap((endpointId) -> streamAdditionalPaths(endpoints, endpointId))
				.collect(Collectors.toCollection(LinkedHashSet::new));
			List<ServerWebExchangeMatcher> delegateMatchers = getDelegateMatchers(paths, this.httpMethod);
			return (!CollectionUtils.isEmpty(delegateMatchers)) ? new OrServerWebExchangeMatcher(delegateMatchers)
					: EMPTY_MATCHER;
		}

		private Stream<String> streamAdditionalPaths(PathMappedEndpoints pathMappedEndpoints, EndpointId endpointId) {
			return pathMappedEndpoints.getAdditionalPaths(this.webServerNamespace, endpointId).stream();
		}

		@Override
		public String toString() {
			return String.format("AdditionalPathsEndpointServerWebExchangeMatcher endpoints=%s, webServerNamespace=%s",
					toString(this.endpoints, ""), this.webServerNamespace);
		}

	}

}
