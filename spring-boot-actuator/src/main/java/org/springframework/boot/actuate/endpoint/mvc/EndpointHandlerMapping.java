/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs via {@link Endpoint#getPath()}.
 * Only endpoints that are annotated as <code>@FrameworkEndpoint</code> will be mapped,
 * and within that class only those methods with <code>@RequestMapping</code> will be
 * exposed. The semantics of <code>@RequestMapping</code> should be identical to a normal
 * <code>@Controller</code>, but the endpoints should not be annotated as
 * <code>@Controller</code> (otherwise they will be mapped by the normal MVC mechanisms).
 * 
 * <p>
 * One of the aims of the mapping is to support endpoints that work as HTTP endpoints but
 * can still provide useful service interfaces when there is no HTTP server (and no Spring
 * MVC on the classpath). Note that any endpoints having method signaturess will break in
 * a non-servlet environment.
 * </p>
 * 
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Dave Syer
 * 
 */
public class EndpointHandlerMapping extends RequestMappingHandlerMapping implements
		InitializingBean, ApplicationContextAware {

	private List<Endpoint<?>> endpoints;

	private String prefix = "";

	private boolean disabled = false;

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}.
	 */
	public EndpointHandlerMapping() {
		// By default the static resource handler mapping is LOWEST_PRECEDENCE - 1
		setOrder(LOWEST_PRECEDENCE - 2);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (this.endpoints == null) {
			this.endpoints = findEndpointBeans();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Endpoint<?>> findEndpointBeans() {
		return new ArrayList(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				getApplicationContext(), Endpoint.class).values());
	}

	/**
	 * Detects &#64;FrameworkEndpoint annotations in handler beans.
	 * 
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		if (this.disabled) {
			return false;
		}
		return AnnotationUtils.findAnnotation(beanType, FrameworkEndpoint.class) != null;
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
		if (bean instanceof Endpoint) {
			Endpoint<?> endpoint = (Endpoint<?>) bean;
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
	public List<Endpoint<?>> getEndpoints() {
		return Collections.unmodifiableList(this.endpoints);
	}
}
