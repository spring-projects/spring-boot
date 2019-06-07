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

import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ServletContextInitializer} to register {@link ExposableServletEndpoint servlet
 * endpoints}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ServletEndpointRegistrar implements ServletContextInitializer {

	private static final Log logger = LogFactory.getLog(ServletEndpointRegistrar.class);

	private final String basePath;

	private final Collection<ExposableServletEndpoint> servletEndpoints;

	public ServletEndpointRegistrar(String basePath, Collection<ExposableServletEndpoint> servletEndpoints) {
		Assert.notNull(servletEndpoints, "ServletEndpoints must not be null");
		this.basePath = cleanBasePath(basePath);
		this.servletEndpoints = servletEndpoints;
	}

	private static String cleanBasePath(String basePath) {
		if (StringUtils.hasText(basePath) && basePath.endsWith("/")) {
			return basePath.substring(0, basePath.length() - 1);
		}
		return (basePath != null) ? basePath : "";
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		this.servletEndpoints.forEach((servletEndpoint) -> register(servletContext, servletEndpoint));
	}

	private void register(ServletContext servletContext, ExposableServletEndpoint endpoint) {
		String name = endpoint.getEndpointId().toLowerCaseString() + "-actuator-endpoint";
		String path = this.basePath + "/" + endpoint.getRootPath();
		String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
		EndpointServlet endpointServlet = endpoint.getEndpointServlet();
		Dynamic registration = servletContext.addServlet(name, endpointServlet.getServlet());
		registration.addMapping(urlMapping);
		registration.setInitParameters(endpointServlet.getInitParameters());
		logger.info("Registered '" + path + "' to " + name);
	}

}
