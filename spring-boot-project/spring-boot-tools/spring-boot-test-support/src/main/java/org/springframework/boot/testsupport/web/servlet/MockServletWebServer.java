/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testsupport.web.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;

import org.springframework.mock.web.MockSessionCookieConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Base class for Mock {@code ServletWebServer} implementations. Reduces the amount of
 * code that would otherwise be duplicated in {@code spring-boot},
 * {@code spring-boot-autoconfigure} and {@code spring-boot-actuator}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class MockServletWebServer {

	private ServletContext servletContext;

	private final Initializer[] initializers;

	private final List<RegisteredServlet> registeredServlets = new ArrayList<>();

	private final List<RegisteredFilter> registeredFilters = new ArrayList<>();

	private final int port;

	/**
	 * Creates a new instance of MockServletWebServer with the specified initializers and
	 * port.
	 * @param initializers an array of Initializer objects to be used for initializing the
	 * server
	 * @param port the port number on which the server will listen for incoming requests
	 */
	public MockServletWebServer(Initializer[] initializers, int port) {
		this.initializers = initializers;
		this.port = port;
		initialize();
	}

	/**
	 * Initializes the MockServletWebServer.
	 *
	 * This method sets up the necessary configurations and mock objects for the
	 * MockServletWebServer. It mocks the behavior of the ServletContext by using
	 * Mockito's mock() method. It sets up mock behaviors for adding servlets, filters,
	 * and initializing parameters. It also sets up a mock SessionCookieConfig and
	 * initializes a map for storing init parameters. Finally, it calls the onStartup()
	 * method of each Initializer in the initializers list.
	 * @throws RuntimeException if a ServletException occurs during initialization
	 */
	private void initialize() {
		try {
			this.servletContext = mock(ServletContext.class);
			lenient().doAnswer((invocation) -> {
				RegisteredServlet registeredServlet = new RegisteredServlet(invocation.getArgument(1));
				MockServletWebServer.this.registeredServlets.add(registeredServlet);
				return registeredServlet.getRegistration();
			}).when(this.servletContext).addServlet(anyString(), any(Servlet.class));
			lenient().doAnswer((invocation) -> {
				RegisteredFilter registeredFilter = new RegisteredFilter(invocation.getArgument(1));
				MockServletWebServer.this.registeredFilters.add(registeredFilter);
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
			for (Initializer initializer : this.initializers) {
				initializer.onStartup(this.servletContext);
			}
		}
		catch (ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Stops the MockServletWebServer by clearing the servlet context and registered
	 * servlets.
	 */
	public void stop() {
		this.servletContext = null;
		this.registeredServlets.clear();
	}

	/**
	 * Returns the ServletContext associated with this MockServletWebServer.
	 * @return the ServletContext associated with this MockServletWebServer
	 */
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * Returns an array of all registered servlets.
	 * @return an array of Servlet objects representing the registered servlets
	 */
	public Servlet[] getServlets() {
		Servlet[] servlets = new Servlet[this.registeredServlets.size()];
		Arrays.setAll(servlets, (i) -> this.registeredServlets.get(i).getServlet());
		return servlets;
	}

	/**
	 * Returns the RegisteredServlet object at the specified index from the list of
	 * registered servlets.
	 * @param index the index of the RegisteredServlet object to retrieve
	 * @return the RegisteredServlet object at the specified index
	 */
	public RegisteredServlet getRegisteredServlet(int index) {
		return getRegisteredServlets().get(index);
	}

	/**
	 * Returns a list of registered servlets.
	 * @return the list of registered servlets
	 */
	public List<RegisteredServlet> getRegisteredServlets() {
		return this.registeredServlets;
	}

	/**
	 * Retrieves the registered filter at the specified index from the list of registered
	 * filters.
	 * @param index the index of the filter to retrieve
	 * @return the registered filter at the specified index
	 */
	public RegisteredFilter getRegisteredFilters(int index) {
		return getRegisteredFilters().get(index);
	}

	/**
	 * Returns the list of registered filters.
	 * @return the list of registered filters
	 */
	public List<RegisteredFilter> getRegisteredFilters() {
		return this.registeredFilters;
	}

	/**
	 * Returns the port number on which the server is running.
	 * @return the port number
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * A registered servlet.
	 */
	public static class RegisteredServlet {

		private final Servlet servlet;

		private final ServletRegistration.Dynamic registration;

		/**
		 * Constructs a new RegisteredServlet object with the given servlet.
		 * @param servlet the servlet to be registered
		 */
		public RegisteredServlet(Servlet servlet) {
			this.servlet = servlet;
			this.registration = mock(ServletRegistration.Dynamic.class);
		}

		/**
		 * Returns the dynamic registration of the servlet.
		 * @return the dynamic registration of the servlet
		 */
		public ServletRegistration.Dynamic getRegistration() {
			return this.registration;
		}

		/**
		 * Returns the servlet associated with this RegisteredServlet.
		 * @return the servlet associated with this RegisteredServlet
		 */
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

		/**
		 * Constructs a new RegisteredFilter object with the specified filter.
		 * @param filter the filter to be registered
		 */
		public RegisteredFilter(Filter filter) {
			this.filter = filter;
			this.registration = mock(FilterRegistration.Dynamic.class);
		}

		/**
		 * Returns the dynamic registration of the filter.
		 * @return the dynamic registration of the filter
		 */
		public FilterRegistration.Dynamic getRegistration() {
			return this.registration;
		}

		/**
		 * Returns the filter associated with this RegisteredFilter object.
		 * @return the filter associated with this RegisteredFilter object
		 */
		public Filter getFilter() {
			return this.filter;
		}

	}

	/**
	 * Initializer (usually implement by adapting {@code Initializer}).
	 */
	@FunctionalInterface
	protected interface Initializer {

		void onStartup(ServletContext context) throws ServletException;

	}

}
