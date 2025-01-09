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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@link HandlerMapping} that exposes
 * {@link org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint @ControllerEndpoint}
 * and
 * {@link org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint @RestControllerEndpoint}
 * annotated endpoints over Spring MVC.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.5 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@Deprecated(since = "3.3.5", forRemoval = true)
@SuppressWarnings("removal")
public class ControllerEndpointHandlerMapping extends RequestMappingHandlerMapping {

	private static final Set<RequestMethod> READ_ONLY_ACCESS_REQUEST_METHODS = EnumSet.of(RequestMethod.GET,
			RequestMethod.HEAD);

	private final EndpointMapping endpointMapping;

	private final CorsConfiguration corsConfiguration;

	private final Map<Object, ExposableControllerEndpoint> handlers;

	private final EndpointAccessResolver accessResolver;

	/**
	 * Create a new {@link ControllerEndpointHandlerMapping} instance providing mappings
	 * for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 */
	public ControllerEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableControllerEndpoint> endpoints, CorsConfiguration corsConfiguration) {
		this(endpointMapping, endpoints, corsConfiguration, (endpointId, defaultAccess) -> Access.NONE);
	}

	/**
	 * Create a new {@link ControllerEndpointHandlerMapping} instance providing mappings
	 * for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 * @param endpointAccessResolver resolver for endpoint access
	 */
	public ControllerEndpointHandlerMapping(EndpointMapping endpointMapping,
			Collection<ExposableControllerEndpoint> endpoints, CorsConfiguration corsConfiguration,
			EndpointAccessResolver endpointAccessResolver) {
		Assert.notNull(endpointMapping, "'endpointMapping' must not be null");
		Assert.notNull(endpoints, "'endpoints' must not be null");
		this.endpointMapping = endpointMapping;
		this.handlers = getHandlers(endpoints);
		this.corsConfiguration = corsConfiguration;
		this.accessResolver = endpointAccessResolver;
		setOrder(-100);
	}

	private Map<Object, ExposableControllerEndpoint> getHandlers(Collection<ExposableControllerEndpoint> endpoints) {
		Map<Object, ExposableControllerEndpoint> handlers = new LinkedHashMap<>();
		endpoints.forEach((endpoint) -> handlers.put(endpoint.getController(), endpoint));
		return Collections.unmodifiableMap(handlers);
	}

	@Override
	protected void initHandlerMethods() {
		this.handlers.keySet().forEach(this::detectHandlerMethods);
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		ExposableControllerEndpoint endpoint = this.handlers.get(handler);
		Access access = this.accessResolver.accessFor(endpoint.getEndpointId(), endpoint.getDefaultAccess());
		if (access == Access.NONE) {
			return;
		}
		if (access == Access.READ_ONLY) {
			mapping = withReadOnlyAccess(access, mapping);
			if (CollectionUtils.isEmpty(mapping.getMethodsCondition().getMethods())) {
				return;
			}
		}
		mapping = withEndpointMappedPatterns(endpoint, mapping);
		super.registerHandlerMethod(handler, method, mapping);
	}

	private RequestMappingInfo withReadOnlyAccess(Access access, RequestMappingInfo mapping) {
		Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
		Set<RequestMethod> modifiedMethods = new HashSet<>(methods);
		if (modifiedMethods.isEmpty()) {
			modifiedMethods.addAll(READ_ONLY_ACCESS_REQUEST_METHODS);
		}
		else {
			modifiedMethods.retainAll(READ_ONLY_ACCESS_REQUEST_METHODS);
		}
		return mapping.mutate().methods(modifiedMethods.toArray(new RequestMethod[0])).build();
	}

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

	private String getEndpointMappedPattern(ExposableControllerEndpoint endpoint, PathPattern pattern) {
		return this.endpointMapping.createSubPath(endpoint.getRootPath() + pattern);
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return this.corsConfiguration != null;
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

}
