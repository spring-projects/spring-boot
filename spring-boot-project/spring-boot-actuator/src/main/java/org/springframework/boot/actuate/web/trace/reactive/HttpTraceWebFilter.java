/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.web.trace.reactive;

import java.security.Principal;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.trace.http.HttpExchangeTracer;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.Include;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;

/**
 * A {@link WebFilter} for tracing HTTP requests.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class HttpTraceWebFilter implements WebFilter, Ordered {

	private static final Object NONE = new Object();

	// Not LOWEST_PRECEDENCE, but near the end, so it has a good chance of catching all
	// enriched headers, but users can add stuff after this if they want to
	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	private final HttpTraceRepository repository;

	private final HttpExchangeTracer tracer;

	private final Set<Include> includes;

	public HttpTraceWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer,
			Set<Include> includes) {
		this.repository = repository;
		this.tracer = tracer;
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
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		Mono<?> principal = (this.includes.contains(Include.PRINCIPAL)
				? exchange.getPrincipal().cast(Object.class).defaultIfEmpty(NONE)
				: Mono.just(NONE));
		Mono<?> session = (this.includes.contains(Include.SESSION_ID)
				? exchange.getSession() : Mono.just(NONE));
		return Mono.zip(principal, session)
				.flatMap((tuple) -> filter(exchange, chain,
						asType(tuple.getT1(), Principal.class),
						asType(tuple.getT2(), WebSession.class)));
	}

	private <T> T asType(Object object, Class<T> type) {
		if (type.isInstance(object)) {
			return type.cast(object);
		}
		return null;
	}

	private Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain,
			Principal principal, WebSession session) {
		ServerWebExchangeTraceableRequest request = new ServerWebExchangeTraceableRequest(
				exchange);
		final HttpTrace trace = this.tracer.receivedRequest(request);
		exchange.getResponse().beforeCommit(() -> {
			TraceableServerHttpResponse response = new TraceableServerHttpResponse(
					exchange.getResponse());
			this.tracer.sendingResponse(trace, response, () -> principal,
					() -> getStartedSessionId(session));
			this.repository.add(trace);
			return Mono.empty();
		});
		return chain.filter(exchange);
	}

	private String getStartedSessionId(WebSession session) {
		return (session != null && session.isStarted()) ? session.getId() : null;
	}

}
