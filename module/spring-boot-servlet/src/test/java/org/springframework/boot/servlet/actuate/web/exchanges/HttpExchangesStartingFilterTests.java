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
import java.security.Principal;
import java.util.EnumSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpExchangesStartingFilter}.
 *
 * @author Spring Boot Team
 */
class HttpExchangesStartingFilterTests {

	private final InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();

	private final HttpExchangesStartingFilter filter = new HttpExchangesStartingFilter(this.repository,
			EnumSet.allOf(Include.class));

	@Test
	void filterRecordsExchangeWhenChainCompletesNormally() throws ServletException, IOException {
		this.filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
		assertThat(this.repository.findAll()).hasSize(1);
	}

	@Test
	void filterSetsStartedAttributeOnRequest() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		this.filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain(new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				// attribute must be set before the chain runs
				assertThat(req.getAttribute(HttpExchangesStartingFilter.ATTRIBUTE_STARTED))
					.isInstanceOf(HttpExchange.Started.class);
			}
		}));
	}

	@Test
	void filterDoesNotRecordWhenFinishedAttributeIsSet() throws ServletException, IOException {
		// Simulate HttpExchangesFilter having already recorded (sets ATTRIBUTE_FINISHED)
		this.filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				new MockFilterChain(new HttpServlet() {
					@Override
					protected void service(HttpServletRequest req, HttpServletResponse resp)
							throws ServletException, IOException {
						req.setAttribute(HttpExchangesStartingFilter.ATTRIBUTE_FINISHED, Boolean.TRUE);
					}
				}));
		// Starting filter must NOT add a second record
		assertThat(this.repository.findAll()).isEmpty();
	}

	@Test
	void filterRecordsWhenChainTerminatesEarly() throws ServletException, IOException {
		// Simulate Spring Security short-circuiting: chain does not call the next filter,
		// ATTRIBUTE_FINISHED is never set, so the starting filter records the exchange.
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(401);
		this.filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain(new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.setStatus(401);
				// Intentionally does NOT call filterChain.doFilter() —
				// simulates security filter short-circuiting.
			}
		}));
		assertThat(this.repository.findAll()).hasSize(1);
		assertThat(this.repository.findAll().get(0).getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void statusIsAssumedToBe500WhenChainFails() {
		assertThatIOException().isThrownBy(() -> this.filter.doFilter(new MockHttpServletRequest(),
				new MockHttpServletResponse(), new MockFilterChain(new HttpServlet() {

					@Override
					protected void service(HttpServletRequest req, HttpServletResponse resp)
							throws ServletException, IOException {
						throw new IOException();
					}

				})))
			.satisfies((ex) -> {
				assertThat(this.repository.findAll()).hasSize(1);
				assertThat(this.repository.findAll().get(0).getResponse().getStatus()).isEqualTo(500);
			});
	}

	@Test
	void filterRecordsPrincipal() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		request.setUserPrincipal(principal);
		this.filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
		assertThat(this.repository.findAll()).hasSize(1);
		HttpExchange.Principal recordedPrincipal = this.repository.findAll().get(0).getPrincipal();
		assertThat(recordedPrincipal).isNotNull();
		assertThat(recordedPrincipal.getName()).isEqualTo("alice");
	}

	@Test
	void filterRejectsInvalidRequests() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setServerName("<script>alert(document.domain)</script>");
		this.filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
		assertThat(this.repository.findAll()).isEmpty();
	}

	@Test
	void filterOrderIsHighestPrecedencePlusOne() {
		assertThat(this.filter.getOrder()).isEqualTo(Integer.MIN_VALUE + 1);
	}

}
