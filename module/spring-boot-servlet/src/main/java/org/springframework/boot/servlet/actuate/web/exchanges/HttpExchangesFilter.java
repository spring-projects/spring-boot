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

package org.springframework.boot.servlet.actuate.web.exchanges;

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
import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet {@link Filter} for recording {@link HttpExchange HTTP exchanges}.
 *
 * <p>
 * This filter runs near the end of the filter chain (at {@code LOWEST_PRECEDENCE - 10})
 * so it captures enriched headers added by earlier filters. When used together with
 * {@link HttpExchangesStartingFilter}, it coordinates recording to ensure that requests
 * short-circuited by high-priority filters (such as Spring Security) are still captured
 * by the starting filter.
 *
 * <p>
 * When {@link HttpExchangesStartingFilter} is present:
 * <ul>
 * <li>This filter reuses the {@link HttpExchange.Started} instance that the starting
 * filter stored as a request attribute.</li>
 * <li>After recording, this filter sets the {@code finished} attribute so the starting
 * filter does not record the exchange a second time.</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Wallace Wadge
 * @author Andy Wilkinson
 * @author Venil Noronha
 * @author Madhura Bhave
 * @since 4.0.0
 * @see HttpExchangesStartingFilter
 */
public class HttpExchangesFilter extends OncePerRequestFilter implements Ordered {

	// Not LOWEST_PRECEDENCE, but near the end, so it has a good chance of catching all
	// enriched headers, but users can add stuff after this if they want to
	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	private final HttpExchangeRepository repository;

	private final Set<Include> includes;

	/**
	 * Create a new {@link HttpExchangesFilter} instance.
	 * @param repository the repository used to record events
	 * @param includes the include options
	 */
	public HttpExchangesFilter(HttpExchangeRepository repository, Set<Include> includes) {
		this.repository = repository;
		this.includes = includes;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isRequestValid(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		// Reuse the Started exchange created by HttpExchangesStartingFilter if present,
		// otherwise create a new one (standalone use without
		// HttpExchangesStartingFilter).
		HttpExchange.Started startedExchange = getOrCreateStartedExchange(request);
		int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
		try {
			filterChain.doFilter(request, response);
			status = response.getStatus();
		}
		finally {
			RecordableServletHttpResponse sourceResponse = new RecordableServletHttpResponse(response, status);
			HttpExchange finishedExchange = startedExchange.finish(sourceResponse, request::getUserPrincipal,
					() -> getSessionId(request), this.includes);
			this.repository.add(finishedExchange);
			// Signal HttpExchangesStartingFilter not to record the exchange again.
			request.setAttribute(HttpExchangesStartingFilter.ATTRIBUTE_FINISHED, Boolean.TRUE);
		}
	}

	/**
	 * Returns the {@link HttpExchange.Started} stored by
	 * {@link HttpExchangesStartingFilter}, or creates a fresh one if the starting filter
	 * is not in the chain.
	 * @param request the source request
	 * @return the started exchange to use for recording
	 */
	private HttpExchange.Started getOrCreateStartedExchange(HttpServletRequest request) {
		Object attribute = request.getAttribute(HttpExchangesStartingFilter.ATTRIBUTE_STARTED);
		if (attribute instanceof HttpExchange.Started started) {
			return started;
		}
		return HttpExchange.start(new RecordableServletHttpRequest(request));
	}

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
	 * Return the session id for the given request, or {@code null} if the request does
	 * not have a session.
	 * @param request the source request
	 * @return the session id or {@code null} if there is no session
	 */
	private @Nullable String getSessionId(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return (session != null) ? session.getId() : null;
	}

}
