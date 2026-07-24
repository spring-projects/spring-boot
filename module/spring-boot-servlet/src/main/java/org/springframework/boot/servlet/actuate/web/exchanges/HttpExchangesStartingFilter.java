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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A high-priority Servlet {@link jakarta.servlet.Filter} that starts recording an
 * {@link HttpExchange} at the very beginning of the filter chain. It coordinates with
 * {@link HttpExchangesFilter}, which runs later with low priority and finishes recording
 * for requests that reach it. For requests that are short-circuited by high-priority
 * filters (such as Spring Security rejecting unauthenticated requests), this filter acts
 * as a fallback and records the exchange in the {@code finally} block after the chain
 * returns.
 *
 * <p>
 * This filter works together with {@link HttpExchangesFilter}:
 * <ul>
 * <li>This filter stores the {@link HttpExchange.Started} instance as a request attribute
 * ({@value #ATTRIBUTE_STARTED}).</li>
 * <li>{@link HttpExchangesFilter} detects that attribute and uses it to finish the
 * exchange, setting the {@value #ATTRIBUTE_FINISHED} attribute to prevent
 * double-recording.</li>
 * <li>If {@link HttpExchangesFilter} is never reached (e.g. request rejected by Spring
 * Security), this filter's {@code finally} block records the exchange.</li>
 * </ul>
 *
 * @author Spring Boot Team
 * @since 4.0.0
 * @see HttpExchangesFilter
 */
public class HttpExchangesStartingFilter extends OncePerRequestFilter implements Ordered {

	/**
	 * Name of the request attribute holding the {@link HttpExchange.Started} created by
	 * this filter.
	 */
	static final String ATTRIBUTE_STARTED = HttpExchangesStartingFilter.class.getName() + ".started";

	/**
	 * Name of the request attribute set to {@code Boolean.TRUE} by
	 * {@link HttpExchangesFilter} once it has finished and recorded the exchange,
	 * preventing this filter from recording it a second time.
	 */
	static final String ATTRIBUTE_FINISHED = HttpExchangesStartingFilter.class.getName() + ".finished";

	// Run as early as possible so we capture even security-rejected requests
	private int order = Ordered.HIGHEST_PRECEDENCE + 1;

	private final HttpExchangeRepository repository;

	private final Set<Include> includes;

	/**
	 * Create a new {@link HttpExchangesStartingFilter} instance.
	 * @param repository the repository used to record events
	 * @param includes the include options
	 */
	public HttpExchangesStartingFilter(HttpExchangeRepository repository, Set<Include> includes) {
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
		RecordableServletHttpRequest sourceRequest = new RecordableServletHttpRequest(request);
		HttpExchange.Started startedExchange = HttpExchange.start(sourceRequest);
		request.setAttribute(ATTRIBUTE_STARTED, startedExchange);
		int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
		try {
			filterChain.doFilter(request, response);
			status = response.getStatus();
		}
		finally {
			// Only record the exchange here if HttpExchangesFilter hasn't already done
			// so.
			// HttpExchangesFilter sets ATTRIBUTE_FINISHED when it records the exchange.
			if (!Boolean.TRUE.equals(request.getAttribute(ATTRIBUTE_FINISHED))) {
				RecordableServletHttpResponse sourceResponse = new RecordableServletHttpResponse(response, status);
				HttpExchange finishedExchange = startedExchange.finish(sourceResponse, request::getUserPrincipal,
						() -> {
							HttpSession session = request.getSession(false);
							return (session != null) ? session.getId() : null;
						}, this.includes);
				this.repository.add(finishedExchange);
			}
		}
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

}
