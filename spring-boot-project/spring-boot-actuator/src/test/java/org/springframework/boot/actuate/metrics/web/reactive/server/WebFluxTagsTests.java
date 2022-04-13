/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.io.EOFException;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebFluxTags}.
 *
 * @author Brian Clozel
 * @author Michael McFadyen
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
class WebFluxTagsTests {

	private MockServerWebExchange exchange;

	private final PathPatternParser parser = new PathPatternParser();

	@BeforeEach
	void setup() {
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
	}

	@Test
	void uriTagValueIsBestMatchingPatternWhenAvailable() {
		this.exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				this.parser.parse("/spring/"));
		this.exchange.getResponse().setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("/spring/");
	}

	@Test
	void uriTagValueIsRootWhenBestMatchingPatternIsEmpty() {
		this.exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, this.parser.parse(""));
		this.exchange.getResponse().setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("root");
	}

	@Test
	void uriTagValueWithBestMatchingPatternAndIgnoreTrailingSlashRemoveTrailingSlash() {
		this.exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				this.parser.parse("/spring/"));
		Tag tag = WebFluxTags.uri(this.exchange, true);
		assertThat(tag.getValue()).isEqualTo("/spring");
	}

	@Test
	void uriTagValueWithBestMatchingPatternAndIgnoreTrailingSlashKeepSingleSlash() {
		this.exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, this.parser.parse("/"));
		Tag tag = WebFluxTags.uri(this.exchange, true);
		assertThat(tag.getValue()).isEqualTo("/");
	}

	@Test
	void uriTagValueIsRedirectionWhenResponseStatusIs3xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("REDIRECTION");
	}

	@Test
	void uriTagValueIsNotFoundWhenResponseStatusIs404() {
		this.exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("NOT_FOUND");
	}

	@Test
	void uriTagToleratesCustomResponseStatus() {
		this.exchange.getResponse().setRawStatusCode(601);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("root");
	}

	@Test
	void uriTagValueIsRootWhenRequestHasNoPatternOrPathInfo() {
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("root");
	}

	@Test
	void uriTagValueIsRootWhenRequestHasNoPatternAndSlashPathInfo() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Tag tag = WebFluxTags.uri(exchange);
		assertThat(tag.getValue()).isEqualTo("root");
	}

	@Test
	void uriTagValueIsUnknownWhenRequestHasNoPatternAndNonRootPathInfo() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/example").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Tag tag = WebFluxTags.uri(exchange);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

	@Test
	void methodTagToleratesNonStandardHttpMethods() {
		ServerWebExchange exchange = mock(ServerWebExchange.class);
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		given(exchange.getRequest()).willReturn(request);
		given(request.getMethod()).willReturn(HttpMethod.valueOf("CUSTOM"));
		Tag tag = WebFluxTags.method(exchange);
		assertThat(tag.getValue()).isEqualTo("CUSTOM");
	}

	@Test
	void outcomeTagIsSuccessWhenResponseStatusIsNull() {
		this.exchange.getResponse().setStatusCode(null);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("SUCCESS");
	}

	@Test
	void outcomeTagIsSuccessWhenResponseStatusIsAvailableFromUnderlyingServer() {
		ServerWebExchange exchange = mock(ServerWebExchange.class);
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getStatusCode().value()).willReturn(null);
		given(exchange.getRequest()).willReturn(request);
		given(exchange.getResponse()).willReturn(response);
		Tag tag = WebFluxTags.outcome(exchange, null);
		assertThat(tag.getValue()).isEqualTo("SUCCESS");
	}

	@Test
	void outcomeTagIsInformationalWhenResponseIs1xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.CONTINUE);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("INFORMATIONAL");
	}

	@Test
	void outcomeTagIsSuccessWhenResponseIs2xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.OK);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("SUCCESS");
	}

	@Test
	void outcomeTagIsRedirectionWhenResponseIs3xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("REDIRECTION");
	}

	@Test
	void outcomeTagIsClientErrorWhenResponseIs4xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("CLIENT_ERROR");
	}

	@Test
	void outcomeTagIsServerErrorWhenResponseIs5xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("SERVER_ERROR");
	}

	@Test
	void outcomeTagIsClientErrorWhenResponseIsNonStandardInClientSeries() {
		this.exchange.getResponse().setRawStatusCode(490);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("CLIENT_ERROR");
	}

	@Test
	void outcomeTagIsUnknownWhenResponseStatusIsInUnknownSeries() {
		this.exchange.getResponse().setRawStatusCode(701);
		Tag tag = WebFluxTags.outcome(this.exchange, null);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

	@Test
	void outcomeTagIsUnknownWhenExceptionIsDisconnectedClient() {
		Tag tag = WebFluxTags.outcome(this.exchange, new EOFException("broken pipe"));
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

	@Test
	void outcomeTagIsUnknownWhenExceptionIsCancelledExchange() {
		Tag tag = WebFluxTags.outcome(this.exchange, new CancelledServerWebExchangeException());
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

}
