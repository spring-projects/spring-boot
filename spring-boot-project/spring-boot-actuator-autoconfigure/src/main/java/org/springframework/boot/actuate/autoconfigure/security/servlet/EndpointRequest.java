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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

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

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Factory that can be used to create a {@link RequestMatcher} for actuator endpoint
 * locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class EndpointRequest {

	private static final RequestMatcher EMPTY_MATCHER = (request) -> false;

	/**
	 * Constructs a new EndpointRequest object.
	 */
	private EndpointRequest() {
	}

	/**
	 * Returns a matcher that includes all {@link Endpoint actuator endpoints}. It also
	 * includes the links endpoint which is present at the base path of the actuator
	 * endpoints. The {@link EndpointRequestMatcher#excluding(Class...) excluding} method
	 * can be used to further remove specific endpoints if required. For example:
	 * <pre class="code">
	 * EndpointRequest.toAnyEndpoint().excluding(ShutdownEndpoint.class)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher toAnyEndpoint() {
		return new EndpointRequestMatcher(true);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to(ShutdownEndpoint.class, HealthEndpoint.class)
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(Class<?>... endpoints) {
		return new EndpointRequestMatcher(endpoints, false);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to("shutdown", "health")
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(String... endpoints) {
		return new EndpointRequestMatcher(endpoints, false);
	}

	/**
	 * Returns a matcher that matches only on the links endpoint. It can be used when
	 * security configuration for the links endpoint is different from the other
	 * {@link Endpoint actuator endpoints}. The
	 * {@link EndpointRequestMatcher#excludingLinks() excludingLinks} method can be used
	 * in combination with this to remove the links endpoint from
	 * {@link EndpointRequest#toAnyEndpoint() toAnyEndpoint}. For example:
	 * <pre class="code">
	 * EndpointRequest.toLinks()
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static LinksRequestMatcher toLinks() {
		return new LinksRequestMatcher();
	}

	/**
	 * Base class for supported request matchers.
	 */
	private abstract static class AbstractRequestMatcher
			extends ApplicationContextRequestMatcher<WebApplicationContext> {

		private volatile RequestMatcher delegate;

		private ManagementPortType managementPortType;

		/**
		 * Constructs a new instance of the AbstractRequestMatcher class.
		 *
		 * This constructor initializes the AbstractRequestMatcher by calling the
		 * constructor of its superclass, passing a WebApplicationContext as a parameter.
		 * @param context the WebApplicationContext to be passed to the superclass
		 * constructor
		 */
		AbstractRequestMatcher() {
			super(WebApplicationContext.class);
		}

		/**
		 * Determines whether to ignore the application context based on the management
		 * port type and server namespace.
		 * @param applicationContext the web application context
		 * @return true if the application context should be ignored, false otherwise
		 */
		@Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext) {
			if (this.managementPortType == null) {
				this.managementPortType = ManagementPortType.get(applicationContext.getEnvironment());
			}
			return this.managementPortType == ManagementPortType.DIFFERENT
					&& !WebServerApplicationContext.hasServerNamespace(applicationContext, "management");
		}

		/**
		 * Initializes the request matcher by creating a delegate object using the
		 * provided context.
		 * @param context the supplier of the web application context
		 */
		@Override
		protected final void initialized(Supplier<WebApplicationContext> context) {
			this.delegate = createDelegate(context.get());
		}

		/**
		 * Determines if the given HttpServletRequest matches the request criteria defined
		 * by this RequestMatcher.
		 * @param request the HttpServletRequest to be matched
		 * @param context a Supplier providing the WebApplicationContext
		 * @return true if the request matches the criteria, false otherwise
		 */
		@Override
		protected final boolean matches(HttpServletRequest request, Supplier<WebApplicationContext> context) {
			return this.delegate.matches(request);
		}

		/**
		 * Creates a delegate RequestMatcher using the provided WebApplicationContext and
		 * RequestMatcherFactory.
		 * @param context the WebApplicationContext to use for creating the delegate
		 * RequestMatcher
		 * @return the delegate RequestMatcher
		 * @throws NoSuchBeanDefinitionException if the RequestMatcherFactory bean is not
		 * found in the context
		 */
		private RequestMatcher createDelegate(WebApplicationContext context) {
			try {
				return createDelegate(context, new RequestMatcherFactory());
			}
			catch (NoSuchBeanDefinitionException ex) {
				return EMPTY_MATCHER;
			}
		}

		/**
		 * Creates a delegate RequestMatcher using the provided WebApplicationContext and
		 * RequestMatcherFactory.
		 * @param context the WebApplicationContext used to create the delegate
		 * RequestMatcher
		 * @param requestMatcherFactory the RequestMatcherFactory used to create the
		 * delegate RequestMatcher
		 * @return the delegate RequestMatcher created using the provided context and
		 * requestMatcherFactory
		 */
		protected abstract RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory);

		/**
		 * Returns a list of request matchers for links.
		 * @param requestMatcherFactory the factory for creating request matchers
		 * @param matcherProvider the provider for request matchers
		 * @param basePath the base path for the request matchers
		 * @return a list of request matchers for links
		 */
		protected List<RequestMatcher> getLinksMatchers(RequestMatcherFactory requestMatcherFactory,
				RequestMatcherProvider matcherProvider, String basePath) {
			List<RequestMatcher> linksMatchers = new ArrayList<>();
			linksMatchers.add(requestMatcherFactory.antPath(matcherProvider, basePath));
			linksMatchers.add(requestMatcherFactory.antPath(matcherProvider, basePath, "/"));
			return linksMatchers;
		}

		/**
		 * Retrieves the RequestMatcherProvider bean from the given WebApplicationContext.
		 * If the bean is not found, a default AntPathRequestMatcher is returned.
		 * @param context the WebApplicationContext from which to retrieve the
		 * RequestMatcherProvider bean
		 * @return the RequestMatcherProvider bean if found, otherwise a default
		 * AntPathRequestMatcher
		 */
		protected RequestMatcherProvider getRequestMatcherProvider(WebApplicationContext context) {
			try {
				return context.getBean(RequestMatcherProvider.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return AntPathRequestMatcher::new;
			}
		}

	}

	/**
	 * The request matcher used to match against {@link Endpoint actuator endpoints}.
	 */
	public static final class EndpointRequestMatcher extends AbstractRequestMatcher {

		private final List<Object> includes;

		private final List<Object> excludes;

		private final boolean includeLinks;

		/**
		 * Constructs a new EndpointRequestMatcher with the specified includeLinks
		 * parameter.
		 * @param includeLinks a boolean indicating whether to include links
		 */
		private EndpointRequestMatcher(boolean includeLinks) {
			this(Collections.emptyList(), Collections.emptyList(), includeLinks);
		}

		/**
		 * Constructs a new EndpointRequestMatcher with the specified endpoints and
		 * includeLinks flag.
		 * @param endpoints the array of endpoint classes to match against
		 * @param includeLinks the flag indicating whether to include links in the
		 * matching process
		 */
		private EndpointRequestMatcher(Class<?>[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks);
		}

		/**
		 * Constructs a new EndpointRequestMatcher with the specified endpoints and
		 * includeLinks flag.
		 * @param endpoints the array of endpoints to match against
		 * @param includeLinks the flag indicating whether to include links in the
		 * matching process
		 */
		private EndpointRequestMatcher(String[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks);
		}

		/**
		 * Constructs a new EndpointRequestMatcher with the specified includes, excludes,
		 * and includeLinks.
		 * @param includes a list of objects to include in the matching process
		 * @param excludes a list of objects to exclude from the matching process
		 * @param includeLinks a boolean indicating whether to include links in the
		 * matching process
		 */
		private EndpointRequestMatcher(List<Object> includes, List<Object> excludes, boolean includeLinks) {
			this.includes = includes;
			this.excludes = excludes;
			this.includeLinks = includeLinks;
		}

		/**
		 * Creates a new EndpointRequestMatcher that excludes the specified endpoints.
		 * @param endpoints the endpoints to be excluded
		 * @return a new EndpointRequestMatcher with the specified endpoints excluded
		 */
		public EndpointRequestMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes, this.includeLinks);
		}

		/**
		 * Creates a new EndpointRequestMatcher with additional endpoints excluded from
		 * matching.
		 * @param endpoints the endpoints to exclude from matching
		 * @return a new EndpointRequestMatcher with the specified endpoints excluded
		 */
		public EndpointRequestMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes, this.includeLinks);
		}

		/**
		 * Creates a new EndpointRequestMatcher with the same includes and excludes as the
		 * current instance, but with the option to exclude links from the matching
		 * process.
		 * @return a new EndpointRequestMatcher instance with the excludes and includes of
		 * the current instance, but with the option to exclude links
		 */
		public EndpointRequestMatcher excludingLinks() {
			return new EndpointRequestMatcher(this.includes, this.excludes, false);
		}

		/**
		 * Creates a delegate RequestMatcher based on the provided WebApplicationContext
		 * and RequestMatcherFactory.
		 * @param context The WebApplicationContext used to retrieve the
		 * PathMappedEndpoints bean.
		 * @param requestMatcherFactory The RequestMatcherFactory used to create the
		 * delegate RequestMatcher.
		 * @return The created delegate RequestMatcher.
		 */
		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory) {
			PathMappedEndpoints pathMappedEndpoints = context.getBean(PathMappedEndpoints.class);
			RequestMatcherProvider matcherProvider = getRequestMatcherProvider(context);
			Set<String> paths = new LinkedHashSet<>();
			if (this.includes.isEmpty()) {
				paths.addAll(pathMappedEndpoints.getAllPaths());
			}
			streamPaths(this.includes, pathMappedEndpoints).forEach(paths::add);
			streamPaths(this.excludes, pathMappedEndpoints).forEach(paths::remove);
			List<RequestMatcher> delegateMatchers = getDelegateMatchers(requestMatcherFactory, matcherProvider, paths);
			String basePath = pathMappedEndpoints.getBasePath();
			if (this.includeLinks && StringUtils.hasText(basePath)) {
				delegateMatchers.addAll(getLinksMatchers(requestMatcherFactory, matcherProvider, basePath));
			}
			return new OrRequestMatcher(delegateMatchers);
		}

		/**
		 * Returns a stream of paths based on the given source list and
		 * pathMappedEndpoints.
		 * @param source the list of objects to filter and map
		 * @param pathMappedEndpoints the pathMappedEndpoints object to retrieve paths
		 * from
		 * @return a stream of paths
		 */
		private Stream<String> streamPaths(List<Object> source, PathMappedEndpoints pathMappedEndpoints) {
			return source.stream().filter(Objects::nonNull).map(this::getEndpointId).map(pathMappedEndpoints::getPath);
		}

		/**
		 * Returns a list of delegate matchers based on the provided paths.
		 * @param requestMatcherFactory The factory used to create request matchers.
		 * @param matcherProvider The provider used to provide request matchers.
		 * @param paths The set of paths to create delegate matchers for.
		 * @return A list of delegate matchers.
		 */
		private List<RequestMatcher> getDelegateMatchers(RequestMatcherFactory requestMatcherFactory,
				RequestMatcherProvider matcherProvider, Set<String> paths) {
			return paths.stream()
				.map((path) -> requestMatcherFactory.antPath(matcherProvider, path, "/**"))
				.collect(Collectors.toCollection(ArrayList::new));
		}

		/**
		 * Returns a string representation of the EndpointRequestMatcher object.
		 * @return a string representation of the EndpointRequestMatcher object
		 */
		@Override
		public String toString() {
			return String.format("EndpointRequestMatcher includes=%s, excludes=%s, includeLinks=%s",
					toString(this.includes, "[*]"), toString(this.excludes, "[]"), this.includeLinks);
		}

		/**
		 * Returns a string representation of the given list of endpoints.
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
		 * @param source the source object to get the EndpointId from
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
		 * @param source the source class
		 * @return the endpoint ID
		 * @throws IllegalStateException if the source class is not annotated
		 * with @Endpoint
		 */
		private EndpointId getEndpointId(Class<?> source) {
			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(source).get(Endpoint.class);
			Assert.state(annotation.isPresent(), () -> "Class " + source + " is not annotated with @Endpoint");
			return EndpointId.of(annotation.getString("id"));
		}

	}

	/**
	 * The request matcher used to match against the links endpoint.
	 */
	public static final class LinksRequestMatcher extends AbstractRequestMatcher {

		/**
		 * Creates a delegate RequestMatcher based on the provided WebApplicationContext
		 * and RequestMatcherFactory.
		 * @param context The WebApplicationContext used to retrieve the
		 * WebEndpointProperties bean.
		 * @param requestMatcherFactory The RequestMatcherFactory used to create the
		 * RequestMatchers.
		 * @return The created RequestMatcher.
		 */
		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory) {
			WebEndpointProperties properties = context.getBean(WebEndpointProperties.class);
			String basePath = properties.getBasePath();
			if (StringUtils.hasText(basePath)) {
				return new OrRequestMatcher(
						getLinksMatchers(requestMatcherFactory, getRequestMatcherProvider(context), basePath));
			}
			return EMPTY_MATCHER;
		}

	}

	/**
	 * Factory used to create a {@link RequestMatcher}.
	 */
	private static final class RequestMatcherFactory {

		/**
		 * Creates a RequestMatcher using the provided RequestMatcherProvider and parts.
		 * @param matcherProvider the RequestMatcherProvider used to create the
		 * RequestMatcher
		 * @param parts the parts used to construct the pattern for the RequestMatcher
		 * @return the created RequestMatcher
		 */
		RequestMatcher antPath(RequestMatcherProvider matcherProvider, String... parts) {
			StringBuilder pattern = new StringBuilder();
			for (String part : parts) {
				pattern.append(part);
			}
			return matcherProvider.getRequestMatcher(pattern.toString());
		}

	}

}
