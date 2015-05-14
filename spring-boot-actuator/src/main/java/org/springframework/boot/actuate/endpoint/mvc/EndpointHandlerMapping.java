/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
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
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Dave Syer
 */
public class EndpointHandlerMapping extends RequestMappingHandlerMapping implements
		ApplicationContextAware {

	private final Set<MvcEndpoint> endpoints;

	private final CorsConfiguration corsConfiguration;

	private String prefix = "";

	private boolean disabled = false;

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}. The endpoints will not accept CORS
	 * requests.
	 * @param endpoints the endpoints
	 */
	public EndpointHandlerMapping(Collection<? extends MvcEndpoint> endpoints) {
		this(endpoints, null);
	}

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}. The endpoints will accepts CORS
	 * requests based on the given {@code corsConfiguration}.
	 * @param endpoints the endpoints
	 * @param corsConfiguration the CORS configuration for the endpoints
	 * @since 1.3.0
	 */
	public EndpointHandlerMapping(Collection<? extends MvcEndpoint> endpoints,
			CorsConfiguration corsConfiguration) {
		this.endpoints = new HashSet<MvcEndpoint>(endpoints);
		this.corsConfiguration = corsConfiguration;
		// By default the static resource handler mapping is LOWEST_PRECEDENCE - 1
		// and the RequestMappingHandlerMapping is 0 (we ideally want to be before both)
		setOrder(-100);
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
	 * anything here
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method,
			RequestMappingInfo mapping) {
		if (mapping == null) {
			return;
		}
		String[] patterns = getPatterns(handler, mapping);
		super.registerHandlerMethod(handler, method, withNewPatterns(mapping, patterns));
	}

	private String[] getPatterns(Object handler, RequestMappingInfo mapping) {
		String path = getPath(handler);
		String prefix = StringUtils.hasText(this.prefix) ? this.prefix + path : path;
		Set<String> defaultPatterns = mapping.getPatternsCondition().getPatterns();
		if (defaultPatterns.isEmpty()) {
			return new String[] { prefix };
		}
		List<String> patterns = new ArrayList<String>(defaultPatterns);
		for (int i = 0; i < patterns.size(); i++) {
			patterns.set(i, prefix + patterns.get(i));
		}
		return patterns.toArray(new String[patterns.size()]);
	}

	private String getPath(Object handler) {
		if (handler instanceof String) {
			handler = getApplicationContext().getBean((String) handler);
		}
		if (handler instanceof MvcEndpoint) {
			return ((MvcEndpoint) handler).getPath();
		}
		return "";
	}

	private RequestMappingInfo withNewPatterns(RequestMappingInfo mapping,
			String[] patternStrings) {
		PatternsRequestCondition patterns = new PatternsRequestCondition(patternStrings);
		return new RequestMappingInfo(patterns, mapping.getMethodsCondition(),
				mapping.getParamsCondition(), mapping.getHeadersCondition(),
				mapping.getConsumesCondition(), mapping.getProducesCondition(),
				mapping.getCustomCondition());
	}

	/**
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String prefix) {
		Assert.isTrue("".equals(prefix) || StringUtils.startsWithIgnoreCase(prefix, "/"),
				"prefix must start with '/'");
		this.prefix = prefix;
	}

	/**
	 * @return the prefix used in mappings
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
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
	 * @return if the mapping is disabled
	 */
	public boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * Return the endpoints
	 * @return the endpoints
	 */
	public Set<? extends MvcEndpoint> getEndpoints() {
		return new HashSet<MvcEndpoint>(this.endpoints);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mappingInfo) {
		return this.corsConfiguration;
	}
}
