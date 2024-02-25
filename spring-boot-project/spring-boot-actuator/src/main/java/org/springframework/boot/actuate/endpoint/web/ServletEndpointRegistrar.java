/*
 * Copyright 2012-2022 the original author or authors.
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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;
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

	/**
	 * Creates a new instance of ServletEndpointRegistrar with the specified base path and
	 * collection of servlet endpoints.
	 * @param basePath the base path for the servlet endpoints
	 * @param servletEndpoints the collection of servlet endpoints to be registered
	 * @throws IllegalArgumentException if servletEndpoints is null
	 */
	public ServletEndpointRegistrar(String basePath, Collection<ExposableServletEndpoint> servletEndpoints) {
		Assert.notNull(servletEndpoints, "ServletEndpoints must not be null");
		this.basePath = cleanBasePath(basePath);
		this.servletEndpoints = servletEndpoints;
	}

	/**
	 * Cleans the base path by removing the trailing slash if present.
	 * @param basePath the base path to be cleaned
	 * @return the cleaned base path
	 */
	private static String cleanBasePath(String basePath) {
		if (StringUtils.hasText(basePath) && basePath.endsWith("/")) {
			return basePath.substring(0, basePath.length() - 1);
		}
		return (basePath != null) ? basePath : "";
	}

	/**
	 * This method is called during the startup of the application. It registers all the
	 * servlet endpoints specified in the servletEndpoints list.
	 * @param servletContext the ServletContext object representing the application's
	 * servlet context
	 * @throws ServletException if there is an error during the registration of servlet
	 * endpoints
	 */
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		this.servletEndpoints.forEach((servletEndpoint) -> register(servletContext, servletEndpoint));
	}

	/**
	 * Registers a servlet endpoint with the given servlet context.
	 * @param servletContext The servlet context to register the endpoint with.
	 * @param endpoint The servlet endpoint to be registered.
	 */
	private void register(ServletContext servletContext, ExposableServletEndpoint endpoint) {
		String name = endpoint.getEndpointId().toLowerCaseString() + "-actuator-endpoint";
		String path = this.basePath + "/" + endpoint.getRootPath();
		String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
		EndpointServlet endpointServlet = endpoint.getEndpointServlet();
		Dynamic registration = servletContext.addServlet(name, endpointServlet.getServlet());
		registration.addMapping(urlMapping);
		registration.setInitParameters(endpointServlet.getInitParameters());
		registration.setLoadOnStartup(endpointServlet.getLoadOnStartup());
		logger.info("Registered '" + path + "' to " + name);
	}

}
