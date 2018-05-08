/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.web.reactive.server;

import io.micrometer.core.instrument.Tag;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxTags}.
 *
 * @author Brian Clozel
 */
public class WebFluxTagsTests {

	private MockServerWebExchange exchange;

	private PathPatternParser parser = new PathPatternParser();

	@Before
	public void setup() {
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
	}

	@Test
	public void uriTagValueIsBestMatchingPatternWhenAvailable() {
		this.exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				this.parser.parse("/spring"));
		this.exchange.getResponse().setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("/spring");
	}

	@Test
	public void uriTagValueIsRedirectionWhenResponseStatusIs3xx() {
		this.exchange.getResponse().setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("REDIRECTION");
	}

	@Test
	public void uriTagValueIsNotFoundWhenResponseStatusIs404() {
		this.exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("NOT_FOUND");
	}

	@Test
	public void uriTagToleratesCustomResponseStatus() {
		this.exchange.getResponse().setStatusCodeValue(601);
		Tag tag = WebFluxTags.uri(this.exchange);
		assertThat(tag.getValue()).isEqualTo("root");
	}

	@Test
	public void uriTagIsUnknownWhenRequestIsNull() {
		Tag tag = WebFluxTags.uri(null);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

}
