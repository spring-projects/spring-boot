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

package org.springframework.boot.actuate.web.exchanges.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.boot.actuate.web.exchanges.reactive.HttpExchangesWebFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet {@link Filter} for recording {@link HttpExchange HTTP exchanges}.
 *
 * @author Dave Syer
 * @author Wallace Wadge
 * @author Andy Wilkinson
 * @author Venil Noronha
 * @author Madhura Bhave
 * @since 3.0.0
 */
public class HttpExchangesFilter extends OncePerRequestFilter implements Ordered {

	// Not LOWEST_PRECEDENCE, but near the end, so it has a good chance of catching all
	// enriched headers, but users can add stuff after this if they want to
	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	private final HttpExchangeRepository repository;

	private final Set<Include> includes;

	/**
	 * Create a new {@link HttpExchangesWebFilter} instance.
	 * @param repository the repository used to record events
	 * @param includes the include options
	 */
	public HttpExchangesFilter(HttpExchangeRepository repository, Set<Include> includes) {
		this.repository = repository;
		this.includes = includes;
	}

	/**
     * Returns the order of this HttpExchangesFilter.
     *
     * @return the order of this HttpExchangesFilter
     */
    @Override
	public int getOrder() {
		return this.order;
	}

	/**
     * Sets the order of the HTTP exchanges filter.
     * 
     * @param order the order of the filter
     */
    public void setOrder(int order) {
		this.order = order;
	}

	/**
     * This method is responsible for filtering incoming HTTP requests and recording the exchanges.
     * It overrides the doFilterInternal method from the parent class.
     * 
     * @param request The HttpServletRequest object representing the incoming request.
     * @param response The HttpServletResponse object representing the response to be sent.
     * @param filterChain The FilterChain object for invoking the next filter in the chain.
     * @throws ServletException If an exception occurs during the filtering process.
     * @throws IOException If an I/O exception occurs during the filtering process.
     */
    @Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isRequestValid(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		RecordableServletHttpRequest sourceRequest = new RecordableServletHttpRequest(request);
		HttpExchange.Started startedHttpExchange = HttpExchange.start(sourceRequest);
		int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
		try {
			filterChain.doFilter(request, response);
			status = response.getStatus();
		}
		finally {
			RecordableServletHttpResponse sourceResponse = new RecordableServletHttpResponse(response, status);
			HttpExchange finishedExchange = startedHttpExchange.finish(sourceResponse, request::getUserPrincipal,
					() -> getSessionId(request), this.includes);
			this.repository.add(finishedExchange);
		}
	}

	/**
     * Checks if the provided HttpServletRequest object represents a valid request.
     * 
     * @param request the HttpServletRequest object to be validated
     * @return true if the request is valid, false otherwise
     */
    private boolean isRequestValid(HttpServletRequest request) {
		try {
			new URI(request.getRequestURL().toString());
			return true;
		}
		catch (URISyntaxException ex) {
			return false;
		}
	}

	/**
     * Retrieves the session ID from the provided HttpServletRequest object.
     * 
     * @param request the HttpServletRequest object from which to retrieve the session ID
     * @return the session ID if a session exists, otherwise null
     */
    private String getSessionId(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return (session != null) ? session.getId() : null;
	}

}
