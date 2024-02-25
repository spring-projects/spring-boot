/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.boot.security.reactive.ApplicationContextServerWebExchangeMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
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

	/**
     * Constructs a new EndpointRequest object.
     */
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
	 * Base class for supported request matchers.
	 */
	private abstract static class AbstractWebExchangeMatcher<T> extends ApplicationContextServerWebExchangeMatcher<T> {

		private ManagementPortType managementPortType;

		/**
         * Constructs a new AbstractWebExchangeMatcher with the specified context class.
         *
         * @param contextClass the class representing the context for the matcher
         */
        AbstractWebExchangeMatcher(Class<? extends T> contextClass) {
			super(contextClass);
		}

		/**
         * Determines whether to ignore the application context based on the management port type.
         * 
         * @param applicationContext the application context to be checked
         * @return true if the application context should be ignored, false otherwise
         */
        @Override
		protected boolean ignoreApplicationContext(ApplicationContext applicationContext) {
			if (this.managementPortType == null) {
				this.managementPortType = ManagementPortType.get(applicationContext.getEnvironment());
			}
			if (this.managementPortType == ManagementPortType.DIFFERENT) {
				if (applicationContext.getParent() == null) {
					return true;
				}
				String managementContextId = applicationContext.getParent().getId() + ":management";
				return !managementContextId.equals(applicationContext.getId());
			}
			return false;
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

		private volatile ServerWebExchangeMatcher delegate;

		/**
         * Constructs a new EndpointServerWebExchangeMatcher with the specified includeLinks flag.
         *
         * @param includeLinks a boolean value indicating whether to include links
         */
        private EndpointServerWebExchangeMatcher(boolean includeLinks) {
			this(Collections.emptyList(), Collections.emptyList(), includeLinks);
		}

		/**
         * Constructs a new EndpointServerWebExchangeMatcher with the specified endpoints and includeLinks flag.
         *
         * @param endpoints the array of endpoint classes to match against
         * @param includeLinks the flag indicating whether to include links in the matching process
         */
        private EndpointServerWebExchangeMatcher(Class<?>[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks);
		}

		/**
         * Constructs a new EndpointServerWebExchangeMatcher with the specified endpoints and includeLinks flag.
         *
         * @param endpoints the array of endpoints to match against
         * @param includeLinks the flag indicating whether to include links in the matching process
         */
        private EndpointServerWebExchangeMatcher(String[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks);
		}

		/**
         * Constructs a new EndpointServerWebExchangeMatcher with the specified includes, excludes, and includeLinks.
         *
         * @param includes the list of objects to include in the matching process
         * @param excludes the list of objects to exclude from the matching process
         * @param includeLinks whether to include links in the matching process
         */
        private EndpointServerWebExchangeMatcher(List<Object> includes, List<Object> excludes, boolean includeLinks) {
			super(PathMappedEndpoints.class);
			this.includes = includes;
			this.excludes = excludes;
			this.includeLinks = includeLinks;
		}

		/**
         * Creates a new EndpointServerWebExchangeMatcher with additional endpoints to exclude.
         *
         * @param endpoints the endpoints to exclude
         * @return a new EndpointServerWebExchangeMatcher with the updated excludes list
         */
        public EndpointServerWebExchangeMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointServerWebExchangeMatcher(this.includes, excludes, this.includeLinks);
		}

		/**
         * Returns a new EndpointServerWebExchangeMatcher with additional endpoints excluded from matching.
         *
         * @param endpoints the endpoints to exclude from matching
         * @return a new EndpointServerWebExchangeMatcher with additional endpoints excluded
         */
        public EndpointServerWebExchangeMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointServerWebExchangeMatcher(this.includes, excludes, this.includeLinks);
		}

		/**
         * Creates a new EndpointServerWebExchangeMatcher with the specified includes and excludes.
         * 
         * @return a new EndpointServerWebExchangeMatcher
         */
        public EndpointServerWebExchangeMatcher excludingLinks() {
			return new EndpointServerWebExchangeMatcher(this.includes, this.excludes, false);
		}

		/**
         * Initializes the EndpointServerWebExchangeMatcher with the given pathMappedEndpoints supplier.
         * 
         * @param pathMappedEndpoints the supplier of PathMappedEndpoints
         */
        @Override
		protected void initialized(Supplier<PathMappedEndpoints> pathMappedEndpoints) {
			this.delegate = createDelegate(pathMappedEndpoints);
		}

		/**
         * Creates a delegate {@link ServerWebExchangeMatcher} using the provided {@link PathMappedEndpoints}.
         * If the {@link PathMappedEndpoints} bean is not found, an empty matcher is returned.
         *
         * @param pathMappedEndpoints the supplier of {@link PathMappedEndpoints} to create the delegate matcher
         * @return the delegate {@link ServerWebExchangeMatcher} created using the {@link PathMappedEndpoints},
         *         or an empty matcher if the {@link PathMappedEndpoints} bean is not found
         */
        private ServerWebExchangeMatcher createDelegate(Supplier<PathMappedEndpoints> pathMappedEndpoints) {
			try {
				return createDelegate(pathMappedEndpoints.get());
			}
			catch (NoSuchBeanDefinitionException ex) {
				return EMPTY_MATCHER;
			}
		}

		/**
         * Creates a delegate ServerWebExchangeMatcher based on the provided PathMappedEndpoints.
         * 
         * @param pathMappedEndpoints the PathMappedEndpoints containing the paths to be matched
         * @return the created delegate ServerWebExchangeMatcher
         */
        private ServerWebExchangeMatcher createDelegate(PathMappedEndpoints pathMappedEndpoints) {
			Set<String> paths = new LinkedHashSet<>();
			if (this.includes.isEmpty()) {
				paths.addAll(pathMappedEndpoints.getAllPaths());
			}
			streamPaths(this.includes, pathMappedEndpoints).forEach(paths::add);
			streamPaths(this.excludes, pathMappedEndpoints).forEach(paths::remove);
			List<ServerWebExchangeMatcher> delegateMatchers = getDelegateMatchers(paths);
			if (this.includeLinks && StringUtils.hasText(pathMappedEndpoints.getBasePath())) {
				delegateMatchers.add(new LinksServerWebExchangeMatcher());
			}
			return new OrServerWebExchangeMatcher(delegateMatchers);
		}

		/**
         * Returns a stream of paths based on the given source list and pathMappedEndpoints.
         * 
         * @param source the list of objects to filter and map
         * @param pathMappedEndpoints the pathMappedEndpoints object to retrieve paths from
         * @return a stream of paths
         */
        private Stream<String> streamPaths(List<Object> source, PathMappedEndpoints pathMappedEndpoints) {
			return source.stream().filter(Objects::nonNull).map(this::getEndpointId).map(pathMappedEndpoints::getPath);
		}

		/**
         * Determines if the given server web exchange matches the endpoint.
         *
         * @param exchange the server web exchange to be matched
         * @param context  the supplier of path mapped endpoints
         * @return a Mono emitting the match result
         */
        @Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<PathMappedEndpoints> context) {
			return this.delegate.matches(exchange);
		}

		/**
         * Returns a list of delegate matchers for the given set of paths.
         *
         * @param paths the set of paths for which to generate delegate matchers
         * @return a list of delegate matchers
         */
        private List<ServerWebExchangeMatcher> getDelegateMatchers(Set<String> paths) {
			return paths.stream().map(this::getDelegateMatcher).collect(Collectors.toCollection(ArrayList::new));
		}

		/**
         * Returns a delegate matcher for the given path.
         *
         * @param path the path to be matched
         * @return the delegate matcher
         */
        private PathPatternParserServerWebExchangeMatcher getDelegateMatcher(String path) {
			return new PathPatternParserServerWebExchangeMatcher(path + "/**");
		}

		/**
         * Returns a string representation of the EndpointRequestMatcher object.
         * 
         * @return a string representation of the EndpointRequestMatcher object
         */
        @Override
		public String toString() {
			return String.format("EndpointRequestMatcher includes=%s, excludes=%s, includeLinks=%s",
					toString(this.includes, "[*]"), toString(this.excludes, "[]"), this.includeLinks);
		}

		/**
         * Returns a string representation of the given list of endpoints.
         * 
         * @param endpoints the list of endpoints to be converted to a string
         * @param emptyValue the value to be returned if the list is empty
         * @return a string representation of the list of endpoints
         */
        private String toString(List<Object> endpoints, String emptyValue) {
			return (!endpoints.isEmpty()) ? endpoints.stream()
				.map(this::getEndpointId)
				.map(Object::toString)
				.collect(Collectors.joining(", ", "[", "]")) : emptyValue;
		}

		/**
         * Returns the EndpointId based on the given source.
         *
         * @param source the source object to determine the EndpointId from
         * @return the EndpointId
         * @throws IllegalStateException if the source is unsupported
         */
        private EndpointId getEndpointId(Object source) {
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

		/**
         * Retrieves the endpoint ID for a given source class.
         * 
         * @param source the source class
         * @return the endpoint ID
         * @throws IllegalStateException if the source class is not annotated with @Endpoint
         */
        private EndpointId getEndpointId(Class<?> source) {
			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(source).get(Endpoint.class);
			Assert.state(annotation.isPresent(), () -> "Class " + source + " is not annotated with @Endpoint");
			return EndpointId.of(annotation.getString("id"));
		}

	}

	/**
	 * The {@link ServerWebExchangeMatcher} used to match against the links endpoint.
	 */
	public static final class LinksServerWebExchangeMatcher extends AbstractWebExchangeMatcher<WebEndpointProperties> {

		private volatile ServerWebExchangeMatcher delegate;

		/**
         * Constructs a new LinksServerWebExchangeMatcher.
         * 
         * @param properties the WebEndpointProperties used for configuration
         */
        private LinksServerWebExchangeMatcher() {
			super(WebEndpointProperties.class);
		}

		/**
         * Initializes the LinksServerWebExchangeMatcher.
         * 
         * @param properties the supplier of WebEndpointProperties
         */
        @Override
		protected void initialized(Supplier<WebEndpointProperties> properties) {
			this.delegate = createDelegate(properties.get());
		}

		/**
         * Creates a delegate ServerWebExchangeMatcher based on the provided WebEndpointProperties.
         * If the base path is specified in the properties, it creates an OrServerWebExchangeMatcher
         * with two PathPatternParserServerWebExchangeMatchers: one for the base path and one for the
         * base path appended with a forward slash.
         * If the base path is not specified, it returns an EMPTY_MATCHER.
         *
         * @param properties the WebEndpointProperties containing the base path
         * @return the created delegate ServerWebExchangeMatcher
         */
        private ServerWebExchangeMatcher createDelegate(WebEndpointProperties properties) {
			if (StringUtils.hasText(properties.getBasePath())) {
				return new OrServerWebExchangeMatcher(
						new PathPatternParserServerWebExchangeMatcher(properties.getBasePath()),
						new PathPatternParserServerWebExchangeMatcher(properties.getBasePath() + "/"));
			}
			return EMPTY_MATCHER;
		}

		/**
         * Determines if the given server web exchange matches the specified endpoint properties.
         *
         * @param exchange the server web exchange to be matched
         * @param context the supplier of web endpoint properties
         * @return a Mono emitting the match result
         */
        @Override
		protected Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<WebEndpointProperties> context) {
			return this.delegate.matches(exchange);
		}

	}

}
