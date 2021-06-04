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

package org.springframework.boot.actuate.trace.http.reactive;

import java.security.Principal;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.trace.http.HttpExchangeTracer;
import org.springframework.boot.actuate.trace.http.HttpTrace.Session;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.actuate.trace.http.Include;
import org.springframework.boot.actuate.web.trace.reactive.HttpTraceWebFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpTraceWebFilter}.
 *
 * @author Andy Wilkinson
 */
class HttpTraceWebFilterTests {

	private final InMemoryHttpTraceRepository repository = new InMemoryHttpTraceRepository();

	private final HttpExchangeTracer tracer = new HttpExchangeTracer(EnumSet.allOf(Include.class));

	private final HttpTraceWebFilter filter = new HttpTraceWebFilter(this.repository, this.tracer,
			EnumSet.allOf(Include.class));

	@Test
	void filterTracesExchange() {
		executeFilter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				(exchange) -> Mono.empty());
		assertThat(this.repository.findAll()).hasSize(1);
	}

	@Test
	void filterCapturesSessionIdWhenSessionIsUsed() {
		executeFilter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				(exchange) -> exchange.getSession().doOnNext((session) -> session.getAttributes().put("a", "alpha"))
						.then());
		assertThat(this.repository.findAll()).hasSize(1);
		Session session = this.repository.findAll().get(0).getSession();
		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotNull();
	}

	@Test
	void filterDoesNotCaptureIdOfUnusedSession() {
		executeFilter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				(exchange) -> exchange.getSession().then());
		assertThat(this.repository.findAll()).hasSize(1);
		Session session = this.repository.findAll().get(0).getSession();
		assertThat(session).isNull();
	}

	@Test
	void filterCapturesPrincipal() {
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		executeFilter(new ServerWebExchangeDecorator(
				MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com"))) {

			@Override
			public Mono<Principal> getPrincipal() {
				return Mono.just(principal);
			}

		}, (exchange) -> exchange.getSession().doOnNext((session) -> session.getAttributes().put("a", "alpha")).then());
		assertThat(this.repository.findAll()).hasSize(1);
		org.springframework.boot.actuate.trace.http.HttpTrace.Principal tracedPrincipal = this.repository.findAll()
				.get(0).getPrincipal();
		assertThat(tracedPrincipal).isNotNull();
		assertThat(tracedPrincipal.getName()).isEqualTo("alice");
	}

	private void executeFilter(ServerWebExchange exchange, WebFilterChain chain) {
		StepVerifier.create(
				this.filter.filter(exchange, chain).then(Mono.defer(() -> exchange.getResponse().setComplete())))
				.verifyComplete();
	}

}
