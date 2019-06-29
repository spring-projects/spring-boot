/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Servlet;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Contains details of a servlet that is exposed as an actuator endpoint.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class EndpointServlet {

	private final Servlet servlet;

	private final Map<String, String> initParameters;

	public EndpointServlet(Class<? extends Servlet> servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = BeanUtils.instantiateClass(servlet);
		this.initParameters = Collections.emptyMap();
	}

	public EndpointServlet(Servlet servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = servlet;
		this.initParameters = Collections.emptyMap();
	}

	private EndpointServlet(Servlet servlet, Map<String, String> initParameters) {
		this.servlet = servlet;
		this.initParameters = Collections.unmodifiableMap(initParameters);
	}

	public EndpointServlet withInitParameter(String name, String value) {
		Assert.hasText(name, "Name must not be empty");
		return withInitParameters(Collections.singletonMap(name, value));
	}

	public EndpointServlet withInitParameters(Map<String, String> initParameters) {
		Assert.notNull(initParameters, "InitParameters must not be null");
		boolean hasEmptyName = initParameters.keySet().stream().anyMatch((name) -> !StringUtils.hasText(name));
		Assert.isTrue(!hasEmptyName, "InitParameters must not contain empty names");
		Map<String, String> mergedInitParameters = new LinkedHashMap<>(this.initParameters);
		mergedInitParameters.putAll(initParameters);
		return new EndpointServlet(this.servlet, mergedInitParameters);
	}

	Servlet getServlet() {
		return this.servlet;
	}

	Map<String, String> getInitParameters() {
		return this.initParameters;
	}

}
