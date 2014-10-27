/*
 * Copyright 2012-2014 the original author or authors.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs via {@link Endpoint#getId()}.
 * The semantics of {@code @RequestMapping} should be identical to a normal
 * {@code @Controller}, but the endpoints should not be annotated as {@code @Controller}
 * (otherwise they will be mapped by the normal MVC mechanisms).
 *
 * <p>
 * One of the aims of the mapping is to support endpoints that work as HTTP endpoints but
 * can still provide useful service interfaces when there is no HTTP server (and no Spring
 * MVC on the classpath). Note that any endpoints having method signaturess will break in
 * a non-servlet environment.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Dave Syer
 */
public class EndpointHandlerMapping extends RequestMappingHandlerMapping implements
		ApplicationContextAware {

	private final Map<String, MvcEndpoint> endpoints = new HashMap<String, MvcEndpoint>();

	private String prefix = "";

	private boolean disabled = false;

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}.
	 * @param endpoints
	 */
	public EndpointHandlerMapping(Collection<? extends MvcEndpoint> endpoints) {
		HashMap<String, MvcEndpoint> map = (HashMap<String, MvcEndpoint>) this.endpoints;
		for (MvcEndpoint endpoint : endpoints) {
			map.put(endpoint.getPath(), endpoint);
		}
		// By default the static resource handler mapping is LOWEST_PRECEDENCE - 1
		// and the RequestMappingHandlerMapping is 0 (we ideally want to be before both)
		setOrder(-100);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (!this.disabled) {
			for (MvcEndpoint endpoint : this.endpoints.values()) {
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

		Set<String> defaultPatterns = mapping.getPatternsCondition().getPatterns();
		String[] patterns = new String[defaultPatterns.isEmpty() ? 1 : defaultPatterns
				.size()];

		String path = "";
		Object bean = handler;
		if (bean instanceof String) {
			bean = getApplicationContext().getBean((String) handler);
		}
		if (bean instanceof MvcEndpoint) {
			MvcEndpoint endpoint = (MvcEndpoint) bean;
			path = endpoint.getPath();
		}

		int i = 0;
		String prefix = StringUtils.hasText(this.prefix) ? this.prefix + path : path;
		if (defaultPatterns.isEmpty()) {
			patterns[0] = prefix;
		}
		else {
			for (String pattern : defaultPatterns) {
				patterns[i] = prefix + pattern;
				i++;
			}
		}
		PatternsRequestCondition patternsInfo = new PatternsRequestCondition(patterns);

		RequestMappingInfo modified = new RequestMappingInfo(patternsInfo,
				mapping.getMethodsCondition(), mapping.getParamsCondition(),
				mapping.getHeadersCondition(), mapping.getConsumesCondition(),
				mapping.getProducesCondition(), mapping.getCustomCondition());

		super.registerHandlerMethod(handler, method, modified);
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
	 * @return the path used in mappings
	 */
	public String getPath(String endpoint) {
		return this.prefix + endpoint;
	}

	/**
	 * Sets if this mapping is disabled.
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Returns if this mapping is disabled.
	 */
	public boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * Return the endpoints
	 */
	public Set<? extends MvcEndpoint> getEndpoints() {
		return new HashSet<MvcEndpoint>(this.endpoints.values());
	}

}
