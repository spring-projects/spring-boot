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
import java.time.Duration;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.web.exchanges.HttpExchange.Session;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpExchangesWebFilter}.
 *
 * @author Andy Wilkinson
 */
class HttpExchangesWebFilterTests {

	private final InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();

	private final HttpExchangesWebFilter filter = new HttpExchangesWebFilter(this.repository,
			EnumSet.allOf(Include.class));

	@Test
	void filterRecordsExchange() {
		executeFilter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				(exchange) -> Mono.empty());
		assertThat(this.repository.findAll()).hasSize(1);
	}

	@Test
	void filterRecordsSessionIdWhenSessionIsUsed() {
		executeFilter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				(exchange) -> exchange.getSession()
					.doOnNext((session) -> session.getAttributes().put("a", "alpha"))
					.then());
		assertThat(this.repository.findAll()).hasSize(1);
		Session session = this.repository.findAll().get(0).getSession();
		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotNull();
	}

	@Test
	void filterDoesNotRecordIdOfUnusedSession() {
		executeFilter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				(exchange) -> exchange.getSession().then());
		assertThat(this.repository.findAll()).hasSize(1);
		Session session = this.repository.findAll().get(0).getSession();
		assertThat(session).isNull();
	}

	@Test
	void filterRecordsPrincipal() {
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		executeFilter(new ServerWebExchangeDecorator(
				MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com"))) {

			@SuppressWarnings("unchecked")
			@Override
			public <T extends Principal> Mono<T> getPrincipal() {
				return Mono.just((T) principal);
			}

		}, (exchange) -> exchange.getSession().doOnNext((session) -> session.getAttributes().put("a", "alpha")).then());
		assertThat(this.repository.findAll()).hasSize(1);
		org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal recordedPrincipal = this.repository
			.findAll()
			.get(0)
			.getPrincipal();
		assertThat(recordedPrincipal).isNotNull();
		assertThat(recordedPrincipal.getName()).isEqualTo("alice");
	}

	private void executeFilter(ServerWebExchange exchange, WebFilterChain chain) {
		StepVerifier
			.create(this.filter.filter(exchange, chain).then(Mono.defer(() -> exchange.getResponse().setComplete())))
			.expectComplete()
			.verify(Duration.ofSeconds(30));
	}

}
