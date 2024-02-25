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

package org.springframework.boot.web.servlet.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@link OncePerRequestFilter} to add an {@literal X-Application-Context} header that
 * contains the {@link ApplicationContext#getId() ApplicationContext ID}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @since 2.0.0
 */
public class ApplicationContextHeaderFilter extends OncePerRequestFilter {

	/**
	 * Public constant for {@literal X-Application-Context}.
	 */
	public static final String HEADER_NAME = "X-Application-Context";

	private final ApplicationContext applicationContext;

	/**
	 * Constructs a new ApplicationContextHeaderFilter with the specified
	 * ApplicationContext.
	 * @param context the ApplicationContext to be set
	 */
	public ApplicationContextHeaderFilter(ApplicationContext context) {
		this.applicationContext = context;
	}

	/**
	 * Adds the application context ID as a header to the response and passes the request
	 * and response to the next filter in the chain.
	 * @param request the HTTP servlet request
	 * @param response the HTTP servlet response
	 * @param filterChain the filter chain
	 * @throws ServletException if a servlet exception occurs
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		response.addHeader(HEADER_NAME, this.applicationContext.getId());
		filterChain.doFilter(request, response);
	}

}
