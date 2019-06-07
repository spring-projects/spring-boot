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

package org.springframework.boot.actuate.trace.http.reactive;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.EnumSet;

import javax.servlet.ServletException;

import org.junit.Test;
import reactor.core.publisher.Mono;

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
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpTraceWebFilter}.
 *
 * @author Andy Wilkinson
 */
public class HttpTraceWebFilterTests {

	private final InMemoryHttpTraceRepository repository = new InMemoryHttpTraceRepository();

	private final HttpExchangeTracer tracer = new HttpExchangeTracer(EnumSet.allOf(Include.class));

	private final HttpTraceWebFilter filter = new HttpTraceWebFilter(this.repository, this.tracer,
			EnumSet.allOf(Include.class));

	@Test
	public void filterTracesExchange() throws ServletException, IOException {
		this.filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				new WebFilterChain() {

					@Override
					public Mono<Void> filter(ServerWebExchange exchange) {
						return Mono.empty();
					}

				}).block(Duration.ofSeconds(30));
		assertThat(this.repository.findAll()).hasSize(1);
	}

	@Test
	public void filterCapturesSessionIdWhenSessionIsUsed() throws ServletException, IOException {
		this.filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				new WebFilterChain() {

					@Override
					public Mono<Void> filter(ServerWebExchange exchange) {
						exchange.getSession().block(Duration.ofSeconds(30)).getAttributes().put("a", "alpha");
						return Mono.empty();
					}

				}).block(Duration.ofSeconds(30));
		assertThat(this.repository.findAll()).hasSize(1);
		Session session = this.repository.findAll().get(0).getSession();
		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotNull();
	}

	@Test
	public void filterDoesNotCaptureIdOfUnusedSession() throws ServletException, IOException {
		this.filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
				new WebFilterChain() {

					@Override
					public Mono<Void> filter(ServerWebExchange exchange) {
						exchange.getSession().block(Duration.ofSeconds(30));
						return Mono.empty();
					}

				}).block(Duration.ofSeconds(30));
		assertThat(this.repository.findAll()).hasSize(1);
		Session session = this.repository.findAll().get(0).getSession();
		assertThat(session).isNull();
	}

	@Test
	public void filterCapturesPrincipal() throws ServletException, IOException {
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		this.filter.filter(new ServerWebExchangeDecorator(
				MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com"))) {

			@Override
			public Mono<Principal> getPrincipal() {
				return Mono.just(principal);
			}

		}, new WebFilterChain() {

			@Override
			public Mono<Void> filter(ServerWebExchange exchange) {
				exchange.getSession().block(Duration.ofSeconds(30)).getAttributes().put("a", "alpha");
				return Mono.empty();
			}

		}).block(Duration.ofSeconds(30));
		assertThat(this.repository.findAll()).hasSize(1);
		org.springframework.boot.actuate.trace.http.HttpTrace.Principal tracedPrincipal = this.repository.findAll()
				.get(0).getPrincipal();
		assertThat(tracedPrincipal).isNotNull();
		assertThat(tracedPrincipal.getName()).isEqualTo("alice");
	}

	@Test
	public void statusIsAssumedToBe500WhenChainFails() throws ServletException, IOException {
		try {
			this.filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("https://api.example.com")),
					new WebFilterChain() {

						@Override
						public Mono<Void> filter(ServerWebExchange exchange) {
							return Mono.error(new RuntimeException());
						}

					}).block(Duration.ofSeconds(30));
			fail();
		}
		catch (Exception ex) {
			assertThat(this.repository.findAll()).hasSize(1);
			assertThat(this.repository.findAll().get(0).getResponse().getStatus()).isEqualTo(500);
		}
	}

}
