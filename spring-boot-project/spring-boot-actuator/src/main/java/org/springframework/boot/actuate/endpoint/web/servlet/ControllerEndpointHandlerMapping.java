/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@link HandlerMapping} that exposes {@link ControllerEndpoint @ControllerEndpoint} and
 * {@link RestControllerEndpoint @RestControllerEndpoint} annotated endpoints over Spring
 * MVC.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ControllerEndpointHandlerMapping extends RequestMappingHandlerMapping {

	private final EndpointMapping endpointMapping;

	private final CorsConfiguration corsConfiguration;

	private final Map<Object, ExposableControllerEndpoint> handlers;

	/**
	 * Create a new {@link ControllerEndpointHandlerMapping} instance providing mappings
	 * for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 */
	public ControllerEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableControllerEndpoint> endpoints, CorsConfiguration corsConfiguration) {
		Assert.notNull(endpointMapping, "EndpointMapping must not be null");
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.endpointMapping = endpointMapping;
		this.handlers = getHandlers(endpoints);
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
	}

	/**
	 * Returns a map of handlers for the given collection of exposable controller
	 * endpoints. The map is created by iterating over the endpoints and mapping each
	 * endpoint's controller to the endpoint itself.
	 * @param endpoints the collection of exposable controller endpoints
	 * @return an unmodifiable map of handlers, where the key is the controller and the
	 * value is the endpoint
	 */
	private Map<Object, ExposableControllerEndpoint> getHandlers(Collection<ExposableControllerEndpoint> endpoints) {
		Map<Object, ExposableControllerEndpoint> handlers = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> handlers.put(endpoint.getController(), endpoint));
		return Collections.unmodifiableMap(handlers);
	}

	/**
	 * Initializes the handler methods for the ControllerEndpointHandlerMapping class.
	 * This method detects the handler methods for each key in the handlers map.
	 */
	@Override
	protected void initHandlerMethods() {
		this.handlers.keySet().forEach(this::detectHandlerMethods);
	}

	/**
	 * Registers a handler method for a ControllerEndpoint.
	 * @param handler the handler object
	 * @param method the method to be registered
	 * @param mapping the RequestMappingInfo for the method
	 */
	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		ExposableControllerEndpoint endpoint = this.handlers.get(handler);
		mapping = withEndpointMappedPatterns(endpoint, mapping);
		super.registerHandlerMethod(handler, method, mapping);
	}

	/**
	 * Returns a new RequestMappingInfo object with the endpoint mapped patterns. If the
	 * mapping does not have any patterns, a default pattern is created. The endpoint
	 * mapped patterns are obtained by mapping each pattern in the original mapping to the
	 * endpoint mapped pattern using the getEndpointMappedPattern method.
	 * @param endpoint the ExposableControllerEndpoint object representing the endpoint
	 * @param mapping the RequestMappingInfo object representing the original mapping
	 * @return a new RequestMappingInfo object with the endpoint mapped patterns
	 */
	private RequestMappingInfo withEndpointMappedPatterns(ExposableControllerEndpoint endpoint,
			RequestMappingInfo mapping) {
		Set<PathPattern> patterns = mapping.getPathPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			patterns = Collections.singleton(getPatternParser().parse(""));
		}
		String[] endpointMappedPatterns = patterns.stream()
			.map((pattern) -> getEndpointMappedPattern(endpoint, pattern))
			.toArray(String[]::new);
		return mapping.mutate().paths(endpointMappedPatterns).build();
	}

	/**
	 * Returns the mapped pattern for the given endpoint and path pattern.
	 * @param endpoint the exposable controller endpoint
	 * @param pattern the path pattern
	 * @return the mapped pattern
	 */
	private String getEndpointMappedPattern(ExposableControllerEndpoint endpoint, PathPattern pattern) {
		return this.endpointMapping.createSubPath(endpoint.getRootPath() + pattern);
	}

	/**
	 * Determines if the specified handler has a CORS configuration source.
	 * @param handler the handler object to check
	 * @return true if the handler has a CORS configuration source, false otherwise
	 */
	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return this.corsConfiguration != null;
	}

	/**
	 * Initializes the CORS configuration for the specified handler method and mapping.
	 * @param handler the handler object
	 * @param method the handler method
	 * @param mapping the mapping information
	 * @return the CORS configuration
	 */
	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	/**
	 * Extends the list of interceptors for the ControllerEndpointHandlerMapping. Adds a
	 * SkipPathExtensionContentNegotiation interceptor to the list.
	 * @param interceptors the list of interceptors to extend
	 */
	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

}
