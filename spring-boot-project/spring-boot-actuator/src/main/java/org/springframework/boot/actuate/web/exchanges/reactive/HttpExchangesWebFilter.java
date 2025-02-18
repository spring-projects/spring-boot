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

package org.springframework.boot.actuate.web.exchanges.reactive;

import java.security.Principal;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;

/**
 * A {@link WebFilter} for recording {@link HttpExchange HTTP exchanges}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public class HttpExchangesWebFilter implements WebFilter, Ordered {

	private static final Object NONE = new Object();

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
	public HttpExchangesWebFilter(HttpExchangeRepository repository, Set<Include> includes) {
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
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		Mono<?> principal = exchange.getPrincipal().cast(Object.class).defaultIfEmpty(NONE);
		Mono<Object> session = exchange.getSession().cast(Object.class).defaultIfEmpty(NONE);
		return Mono.zip(PrincipalAndSession::new, principal, session)
			.flatMap((principalAndSession) -> filter(exchange, chain, principalAndSession));
	}

	private Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain,
			PrincipalAndSession principalAndSession) {
		return Mono.fromRunnable(() -> addExchangeOnCommit(exchange, principalAndSession)).and(chain.filter(exchange));
	}

	private void addExchangeOnCommit(ServerWebExchange exchange, PrincipalAndSession principalAndSession) {
		RecordableServerHttpRequest sourceRequest = new RecordableServerHttpRequest(exchange.getRequest());
		HttpExchange.Started startedHttpExchange = HttpExchange.start(sourceRequest);
		exchange.getResponse().beforeCommit(() -> {
			RecordableServerHttpResponse sourceResponse = new RecordableServerHttpResponse(exchange.getResponse());
			HttpExchange finishedExchange = startedHttpExchange.finish(sourceResponse,
					principalAndSession::getPrincipal, principalAndSession::getSessionId, this.includes);
			this.repository.add(finishedExchange);
			return Mono.empty();
		});
	}

	/**
	 * A {@link Principal} and {@link WebSession}.
	 */
	private static class PrincipalAndSession {

		private final Principal principal;

		private final WebSession session;

		PrincipalAndSession(Object[] zipped) {
			this.principal = (zipped[0] != NONE) ? (Principal) zipped[0] : null;
			this.session = (zipped[1] != NONE) ? (WebSession) zipped[1] : null;
		}

		Principal getPrincipal() {
			return this.principal;
		}

		String getSessionId() {
			return (this.session != null && this.session.isStarted()) ? this.session.getId() : null;
		}

	}

}
