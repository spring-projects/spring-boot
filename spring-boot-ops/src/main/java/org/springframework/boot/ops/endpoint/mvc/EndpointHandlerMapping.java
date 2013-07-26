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

package org.springframework.boot.ops.endpoint.mvc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ops.endpoint.ActionEndpoint;
import org.springframework.boot.ops.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs via {@link Endpoint#getPath()}.
 * Standard {@link Endpoint}s are mapped to GET requests, {@link ActionEndpoint}s are
 * mapped to POST requests.
 * 
 * @author Phillip Webb
 * @see EndpointHandlerAdapter
 */
public class EndpointHandlerMapping extends AbstractUrlHandlerMapping implements
		InitializingBean, ApplicationContextAware {

	private List<Endpoint<?>> endpoints;

	private String prefix = "";

	private boolean disabled = false;

	/**
	 * Create a new {@link EndpointHandlerMapping} instance. All {@link Endpoint}s will be
	 * detected from the {@link ApplicationContext}.
	 */
	public EndpointHandlerMapping() {
		setOrder(HIGHEST_PRECEDENCE);
	}

	/**
	 * Create a new {@link EndpointHandlerMapping} with the specified endpoints.
	 * @param endpoints the endpoints
	 */
	public EndpointHandlerMapping(Collection<? extends Endpoint<?>> endpoints) {
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.endpoints = new ArrayList<Endpoint<?>>(endpoints);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.endpoints == null) {
			this.endpoints = findEndpointBeans();
		}
		if (!this.disabled) {
			for (Endpoint<?> endpoint : this.endpoints) {
				registerHandler(this.prefix + endpoint.getPath(), endpoint);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Endpoint<?>> findEndpointBeans() {
		return new ArrayList(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				getApplicationContext(), Endpoint.class).values());
	}

	@Override
	protected Object lookupHandler(String urlPath, HttpServletRequest request)
			throws Exception {
		Object handler = super.lookupHandler(urlPath, request);
		if (handler != null) {
			Object endpoint = (handler instanceof HandlerExecutionChain ? ((HandlerExecutionChain) handler)
					.getHandler() : handler);
			String method = (endpoint instanceof ActionEndpoint<?> ? "POST" : "GET");
			if (request.getMethod().equals(method)) {
				return endpoint;
			}
		}
		return null;
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
