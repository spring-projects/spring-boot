/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.mock.web.MockSessionCookieConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * A mock servlet {@link WebServer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class MockServletWebServer implements WebServer {

	private ServletContext servletContext;

	private final Initializer[] initializers;

	private final List<RegisteredServlet> registeredServlets = new ArrayList<>();

	private final List<RegisteredFilter> registeredFilters = new ArrayList<>();

	private final Map<String, FilterRegistration> filterRegistrations = new HashMap<>();

	private final Map<String, ServletRegistration> servletRegistrations = new HashMap<>();

	private final int port;

	MockServletWebServer(ServletContextInitializers initializers, int port) {
		this(StreamSupport.stream(initializers.spliterator(), false)
			.map((initializer) -> (Initializer) initializer::onStartup)
			.toArray(Initializer[]::new), port);
	}

	MockServletWebServer(Initializer[] initializers, int port) {
		this.initializers = initializers;
		this.port = port;
		initialize();
	}

	@SuppressWarnings("unchecked")
	private void initialize() {
		try {
			this.servletContext = mock(ServletContext.class);
			lenient().doAnswer((invocation) -> {
				RegisteredServlet registeredServlet = new RegisteredServlet(invocation.getArgument(1));
				MockServletWebServer.this.registeredServlets.add(registeredServlet);
				this.servletRegistrations.put(invocation.getArgument(0), registeredServlet.getRegistration());
				return registeredServlet.getRegistration();
			}).when(this.servletContext).addServlet(anyString(), any(Servlet.class));
			lenient().doAnswer((invocation) -> {
				RegisteredFilter registeredFilter = new RegisteredFilter(invocation.getArgument(1));
				MockServletWebServer.this.registeredFilters.add(registeredFilter);
				this.filterRegistrations.put(invocation.getArgument(0), registeredFilter.getRegistration());
				return registeredFilter.getRegistration();
			}).when(this.servletContext).addFilter(anyString(), any(Filter.class));
			final SessionCookieConfig sessionCookieConfig = new MockSessionCookieConfig();
			given(this.servletContext.getSessionCookieConfig()).willReturn(sessionCookieConfig);
			final Map<String, String> initParameters = new HashMap<>();
			lenient().doAnswer((invocation) -> {
				initParameters.put(invocation.getArgument(0), invocation.getArgument(1));
				return null;
			}).when(this.servletContext).setInitParameter(anyString(), anyString());
			given(this.servletContext.getInitParameterNames())
				.willReturn(Collections.enumeration(initParameters.keySet()));
			lenient().doAnswer((invocation) -> initParameters.get(invocation.getArgument(0)))
				.when(this.servletContext)
				.getInitParameter(anyString());
			given(this.servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
			lenient().when((Map<String, FilterRegistration>) this.servletContext.getFilterRegistrations())
				.thenReturn(this.filterRegistrations);
			lenient().when((Map<String, ServletRegistration>) this.servletContext.getServletRegistrations())
				.thenReturn(this.servletRegistrations);
			for (Initializer initializer : this.initializers) {
				initializer.onStartup(this.servletContext);
			}
		}
		catch (ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void start() throws WebServerException {
	}

	@Override
	public void stop() {
		this.servletContext = null;
		this.registeredServlets.clear();
		this.filterRegistrations.clear();
		this.registeredFilters.clear();
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	public Servlet[] getServlets() {
		Servlet[] servlets = new Servlet[this.registeredServlets.size()];
		Arrays.setAll(servlets, (i) -> this.registeredServlets.get(i).getServlet());
		return servlets;
	}

	public RegisteredServlet getRegisteredServlet(int index) {
		return getRegisteredServlets().get(index);
	}

	public List<RegisteredServlet> getRegisteredServlets() {
		return this.registeredServlets;
	}

	public RegisteredFilter getRegisteredFilters(int index) {
		return getRegisteredFilters().get(index);
	}

	public List<RegisteredFilter> getRegisteredFilters() {
		return this.registeredFilters;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	/**
	 * A registered servlet.
	 */
	public static class RegisteredServlet {

		private final Servlet servlet;

		private final ServletRegistration.Dynamic registration;

		public RegisteredServlet(Servlet servlet) {
			this.servlet = servlet;
			this.registration = mock(ServletRegistration.Dynamic.class);
		}

		public ServletRegistration.Dynamic getRegistration() {
			return this.registration;
		}

		public Servlet getServlet() {
			return this.servlet;
		}

	}

	/**
	 * A registered filter.
	 */
	public static class RegisteredFilter {

		private final Filter filter;

		private final FilterRegistration.Dynamic registration;

		public RegisteredFilter(Filter filter) {
			this.filter = filter;
			this.registration = mock(FilterRegistration.Dynamic.class);
		}

		public FilterRegistration.Dynamic getRegistration() {
			return this.registration;
		}

		public Filter getFilter() {
			return this.filter;
		}

	}

	/**
	 * Initializer (usually implement by adapting {@code ServletContextInitializer}).
	 */
	@FunctionalInterface
	protected interface Initializer {

		void onStartup(ServletContext context) throws ServletException;

	}

}
