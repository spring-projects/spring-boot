/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs via {@link Endpoint#getId()}.
 * The semantics of {@code @RequestMapping} should be identical to a normal
 * {@code @Controller}, but the endpoints should not be annotated as {@code @Controller}
 * (otherwise they will be mapped by the normal MVC mechanisms).
 * <p>
 * One of the aims of the mapping is to support endpoints that work as HTTP endpoints but
 * can still provide useful service interfaces when there is no HTTP server (and no Spring
 * MVC on the classpath). Note that any endpoints having method signatures will break in a
 * non-servlet environment.
 *
 * @param <E> the endpoint type
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Madhura Bhave
 */
public abstract class AbstractEndpointHandlerMapping<E extends MvcEndpoint>
		extends RequestMappingHandlerMapping {

	private final Set<E> endpoints;

	private HandlerInterceptor securityInterceptor;

	private final CorsConfiguration corsConfiguration;

	private String prefix = "";

	private boolean disabled = false;

	/**
	 * Create a new {@link AbstractEndpointHandlerMapping} instance. All {@link Endpoint}s
	 * will be detected from the {@link ApplicationContext}. The endpoints will not accept
	 * CORS requests.
	 * @param endpoints the endpoints
	 */
	public AbstractEndpointHandlerMapping(Collection<? extends E> endpoints) {
		this(endpoints, null);
	}

	/**
	 * Create a new {@link AbstractEndpointHandlerMapping} instance. All {@link Endpoint}s
	 * will be detected from the {@link ApplicationContext}. The endpoints will accepts
	 * CORS requests based on the given {@code corsConfiguration}.
	 * @param endpoints the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 * @since 1.3.0
	 */
	public AbstractEndpointHandlerMapping(Collection<? extends E> endpoints,
			CorsConfiguration corsConfiguration) {
		this.endpoints = new HashSet<E>(endpoints);
		postProcessEndpoints(this.endpoints);
		this.corsConfiguration = corsConfiguration;
		// By default the static resource handler mapping is LOWEST_PRECEDENCE - 1
		// and the RequestMappingHandlerMapping is 0 (we ideally want to be before both)
		setOrder(-100);
		setUseSuffixPatternMatch(false);
	}

	/**
	 * Post process the endpoint setting before they are used. Subclasses can add or
	 * modify the endpoints as necessary.
	 * @param endpoints the endpoints to post process
	 */
	protected void postProcessEndpoints(Set<E> endpoints) {
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (!this.disabled) {
			for (MvcEndpoint endpoint : this.endpoints) {
				detectHandlerMethods(endpoint);
			}
		}
	}

	/**
	 * Since all handler beans are passed into the constructor there is no need to detect
	 * anything here.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	@Override
	@Deprecated
	protected void registerHandlerMethod(Object handler, Method method,
			RequestMappingInfo mapping) {
		if (mapping == null) {
			return;
		}
		String[] patterns = getPatterns(handler, mapping);
		if (!ObjectUtils.isEmpty(patterns)) {
			super.registerHandlerMethod(handler, method,
					withNewPatterns(mapping, patterns));
		}
	}

	private String[] getPatterns(Object handler, RequestMappingInfo mapping) {
		if (handler instanceof String) {
			handler = getApplicationContext().getBean((String) handler);
		}
		Assert.state(handler instanceof MvcEndpoint, "Only MvcEndpoints are supported");
		String path = getPath((MvcEndpoint) handler);
		return (path != null) ? getEndpointPatterns(path, mapping) : null;
	}

	/**
	 * Return the path that should be used to map the given {@link MvcEndpoint}.
	 * @param endpoint the endpoint to map
	 * @return the path to use for the endpoint or {@code null} if no mapping is required
	 */
	protected String getPath(MvcEndpoint endpoint) {
		return endpoint.getPath();
	}

	private String[] getEndpointPatterns(String path, RequestMappingInfo mapping) {
		String patternPrefix = (StringUtils.hasText(this.prefix) ? this.prefix + path
				: path);
		Set<String> defaultPatterns = mapping.getPatternsCondition().getPatterns();
		if (defaultPatterns.isEmpty()) {
			return new String[] { patternPrefix, patternPrefix + ".json" };
		}
		List<String> patterns = new ArrayList<String>(defaultPatterns);
		for (int i = 0; i < patterns.size(); i++) {
			patterns.set(i, patternPrefix + patterns.get(i));
		}
		return patterns.toArray(new String[patterns.size()]);
	}

	private RequestMappingInfo withNewPatterns(RequestMappingInfo mapping,
			String[] patternStrings) {
		PatternsRequestCondition patterns = new PatternsRequestCondition(patternStrings,
				null, null, useSuffixPatternMatch(), useTrailingSlashMatch(), null);
		return new RequestMappingInfo(patterns, mapping.getMethodsCondition(),
				mapping.getParamsCondition(), mapping.getHeadersCondition(),
				mapping.getConsumesCondition(), mapping.getProducesCondition(),
				mapping.getCustomCondition());
	}

	@Override
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler,
			HttpServletRequest request) {
		HandlerExecutionChain chain = super.getHandlerExecutionChain(handler, request);
		if (this.securityInterceptor == null || CorsUtils.isCorsRequest(request)) {
			return chain;
		}
		return addSecurityInterceptor(chain);
	}

	@Override
	protected HandlerExecutionChain getCorsHandlerExecutionChain(
			HttpServletRequest request, HandlerExecutionChain chain,
			CorsConfiguration config) {
		chain = super.getCorsHandlerExecutionChain(request, chain, config);
		if (this.securityInterceptor == null) {
			return chain;
		}
		return addSecurityInterceptor(chain);
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

	private HandlerExecutionChain addSecurityInterceptor(HandlerExecutionChain chain) {
		List<HandlerInterceptor> interceptors = new ArrayList<HandlerInterceptor>();
		if (chain.getInterceptors() != null) {
			interceptors.addAll(Arrays.asList(chain.getInterceptors()));
		}
		interceptors.add(this.securityInterceptor);
		return new HandlerExecutionChain(chain.getHandler(),
				interceptors.toArray(new HandlerInterceptor[interceptors.size()]));
	}

	/**
	 * Set the handler interceptor that will be used for security.
	 * @param securityInterceptor the security handler interceptor
	 */
	public void setSecurityInterceptor(HandlerInterceptor securityInterceptor) {
		this.securityInterceptor = securityInterceptor;
	}

	/**
	 * Set the prefix used in mappings.
	 * @param prefix the prefix
	 */
	public void setPrefix(String prefix) {
		Assert.isTrue("".equals(prefix) || StringUtils.startsWithIgnoreCase(prefix, "/"),
				"prefix must start with '/'");
		this.prefix = prefix;
	}

	/**
	 * Get the prefix used in mappings.
	 * @return the prefix
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Get the path of the endpoint.
	 * @param endpoint the endpoint
	 * @return the path used in mappings
	 */
	public String getPath(String endpoint) {
		return this.prefix + endpoint;
	}

	/**
	 * Sets if this mapping is disabled.
	 * @param disabled if the mapping is disabled
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Returns if this mapping is disabled.
	 * @return {@code true} if the mapping is disabled
	 */
	public boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * Return the endpoints.
	 * @return the endpoints
	 */
	public Set<E> getEndpoints() {
		return Collections.unmodifiableSet(this.endpoints);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mappingInfo) {
		return this.corsConfiguration;
	}

	/**
	 * {@link HandlerInterceptorAdapter} to ensure that
	 * {@link PathExtensionContentNegotiationStrategy} is skipped for actuator endpoints.
	 */
	private static final class SkipPathExtensionContentNegotiation
			extends HandlerInterceptorAdapter {

		private static final String SKIP_ATTRIBUTE = PathExtensionContentNegotiationStrategy.class
				.getName() + ".SKIP";

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
				Object handler) throws Exception {
			request.setAttribute(SKIP_ATTRIBUTE, Boolean.TRUE);
			return true;
		}

	}

}
