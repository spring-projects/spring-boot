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

package org.springframework.boot.actuate.endpoint.web;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
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
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@Deprecated(since = "3.3.0", forRemoval = true)
@SuppressWarnings("removal")
public class ServletEndpointRegistrar implements ServletContextInitializer {

	private static final Set<String> READ_ONLY_ACCESS_REQUEST_METHODS = Set.of("GET", "HEAD");

	private static final Log logger = LogFactory.getLog(ServletEndpointRegistrar.class);

	private final String basePath;

	private final Collection<ExposableServletEndpoint> servletEndpoints;

	private final EndpointAccessResolver endpointAccessResolver;

	public ServletEndpointRegistrar(String basePath, Collection<ExposableServletEndpoint> servletEndpoints) {
		this(basePath, servletEndpoints, (endpointId, defaultAccess) -> Access.NONE);
	}

	public ServletEndpointRegistrar(String basePath, Collection<ExposableServletEndpoint> servletEndpoints,
			EndpointAccessResolver endpointAccessResolver) {
		Assert.notNull(servletEndpoints, "'servletEndpoints' must not be null");
		this.basePath = cleanBasePath(basePath);
		this.servletEndpoints = servletEndpoints;
		this.endpointAccessResolver = endpointAccessResolver;
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
		Access access = this.endpointAccessResolver.accessFor(endpoint.getEndpointId(), endpoint.getDefaultAccess());
		if (access == Access.NONE) {
			return;
		}
		String name = endpoint.getEndpointId().toLowerCaseString() + "-actuator-endpoint";
		String path = this.basePath + "/" + endpoint.getRootPath();
		String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
		EndpointServlet endpointServlet = endpoint.getEndpointServlet();
		Dynamic registration = servletContext.addServlet(name, endpointServlet.getServlet());
		registration.addMapping(urlMapping);
		registration.setInitParameters(endpointServlet.getInitParameters());
		registration.setLoadOnStartup(endpointServlet.getLoadOnStartup());
		if (access == Access.READ_ONLY) {
			servletContext.addFilter(name + "-access-filter", new ReadOnlyAccessFilter())
				.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), false, name);
		}
		logger.info("Registered '" + path + "' to " + name);
	}

	static class ReadOnlyAccessFilter implements Filter {

		private static final int METHOD_NOT_ALLOWED = 405;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			if (request instanceof HttpServletRequest httpRequest
					&& response instanceof HttpServletResponse httpResponse) {
				doFilter(httpRequest, httpResponse, chain);
			}
			else {
				throw new ServletException();
			}
		}

		private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			if (isReadOnlyAccessMethod(request)) {
				chain.doFilter(request, response);
			}
			else {
				response.sendError(METHOD_NOT_ALLOWED);
			}
		}

		private boolean isReadOnlyAccessMethod(HttpServletRequest request) {
			return READ_ONLY_ACCESS_REQUEST_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT));
		}

	}

}
