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

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Simple example Filter used for testing.
 *
 * @author Phillip Webb
 */
public class ExampleFilter implements Filter {

	/**
	 * Initializes the filter.
	 * @param filterConfig the filter configuration object
	 * @throws ServletException if an exception occurs during initialization
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	/**
	 * This method is called by the web container to indicate to a filter that it is being
	 * taken out of service. It is called after all filter methods have been completed.
	 * This method gives the filter an opportunity to clean up any resources that it is
	 * holding (for example, memory, file handles, threads) and make sure that any
	 * persistent state is synchronized with the filter's current state in memory.
	 */
	@Override
	public void destroy() {
	}

	/**
	 * This method is used to filter the incoming requests and responses. It writes a "["
	 * character to the response writer before passing the request and response to the
	 * next filter in the chain. After the request and response have been processed by the
	 * next filter, it writes a "]" character to the response writer.
	 * @param request the ServletRequest object representing the incoming request
	 * @param response the ServletResponse object representing the outgoing response
	 * @param chain the FilterChain object used to invoke the next filter in the chain
	 * @throws IOException if an I/O error occurs during the filtering process
	 * @throws ServletException if a servlet-specific error occurs during the filtering
	 * process
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		response.getWriter().write("[");
		chain.doFilter(request, response);
		response.getWriter().write("]");
	}

}
