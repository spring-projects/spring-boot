/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.devtools.remote.server;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Servlet filter providing integration with the remote server {@link Dispatcher}.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 */
public class DispatcherFilter implements Filter {

	private final Dispatcher dispatcher;

	/**
	 * Constructs a new DispatcherFilter with the specified Dispatcher.
	 * @param dispatcher the Dispatcher to be used by the filter (must not be null)
	 * @throws IllegalArgumentException if the dispatcher is null
	 */
	public DispatcherFilter(Dispatcher dispatcher) {
		Assert.notNull(dispatcher, "Dispatcher must not be null");
		this.dispatcher = dispatcher;
	}

	/**
	 * Initializes the filter.
	 * @param filterConfig the filter configuration object
	 * @throws ServletException if an exception occurs during initialization
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	/**
	 * This method is responsible for filtering the incoming requests and responses. It
	 * checks if the request and response objects are of type HttpServletRequest and
	 * HttpServletResponse, and if so, it calls the overloaded doFilter method with the
	 * appropriate types. If the objects are not of the expected types, it simply calls
	 * the doFilter method of the FilterChain object.
	 * @param request the ServletRequest object representing the incoming request
	 * @param response the ServletResponse object representing the outgoing response
	 * @param chain the FilterChain object representing the chain of filters to be
	 * executed
	 * @throws IOException if an I/O error occurs during the filtering process
	 * @throws ServletException if a servlet-specific error occurs during the filtering
	 * process
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	/**
	 * Filters the incoming HTTP request and response.
	 * @param request the HttpServletRequest object representing the incoming request
	 * @param response the HttpServletResponse object representing the outgoing response
	 * @param chain the FilterChain object for invoking the next filter in the chain
	 * @throws IOException if an I/O error occurs during the filtering process
	 * @throws ServletException if a servlet error occurs during the filtering process
	 */
	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		ServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		if (!this.dispatcher.handle(serverRequest, serverResponse)) {
			chain.doFilter(request, response);
		}
	}

	/**
	 * This method is called by the servlet container to indicate to a filter that it is
	 * being taken out of service. This method is only called once all threads within the
	 * filter's doFilter method have exited or after a timeout period has passed. After
	 * the servlet container calls this method, it will not call the doFilter method again
	 * on this instance of the filter.
	 */
	@Override
	public void destroy() {
	}

}
