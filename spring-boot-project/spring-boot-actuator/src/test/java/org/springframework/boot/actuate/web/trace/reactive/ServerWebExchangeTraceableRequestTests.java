/*
 * Copyright 2012-2020 the original author or authors.
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServerWebExchangeTraceableRequest}.
 *
 * @author Dmytro Nosan
 */
class ServerWebExchangeTraceableRequestTests {

	private ServerWebExchange exchange;

	private ServerHttpRequest request;

	@BeforeEach
	void setUp() {
		this.exchange = mock(ServerWebExchange.class);
		this.request = mock(ServerHttpRequest.class);
		given(this.exchange.getRequest()).willReturn(this.request);
	}

	@Test
	void getMethod() {
		String method = "POST";
		given(this.request.getMethodValue()).willReturn(method);
		ServerWebExchangeTraceableRequest traceableRequest = new ServerWebExchangeTraceableRequest(this.exchange);
		assertThat(traceableRequest.getMethod()).isSameAs(method);
	}

	@Test
	void getUri() {
		URI uri = URI.create("http://localhost:8080/");
		given(this.request.getURI()).willReturn(uri);
		ServerWebExchangeTraceableRequest traceableRequest = new ServerWebExchangeTraceableRequest(this.exchange);
		assertThat(traceableRequest.getUri()).isSameAs(uri);
	}

	@Test
	void getHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("name", "value");
		given(this.request.getHeaders()).willReturn(httpHeaders);
		ServerWebExchangeTraceableRequest traceableRequest = new ServerWebExchangeTraceableRequest(this.exchange);
		assertThat(traceableRequest.getHeaders()).containsOnly(entry("name", Collections.singletonList("value")));
	}

	@Test
	void getUnresolvedRemoteAddress() {
		InetSocketAddress socketAddress = InetSocketAddress.createUnresolved("unresolved.example.com", 8080);
		given(this.request.getRemoteAddress()).willReturn(socketAddress);
		ServerWebExchangeTraceableRequest traceableRequest = new ServerWebExchangeTraceableRequest(this.exchange);
		assertThat(traceableRequest.getRemoteAddress()).isNull();
	}

	@Test
	void getRemoteAddress() {
		InetSocketAddress socketAddress = new InetSocketAddress(0);
		given(this.request.getRemoteAddress()).willReturn(socketAddress);
		ServerWebExchangeTraceableRequest traceableRequest = new ServerWebExchangeTraceableRequest(this.exchange);
		assertThat(traceableRequest.getRemoteAddress()).isEqualTo(socketAddress.getAddress().toString());
	}

}
