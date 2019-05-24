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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.time.Duration;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsWebFilter}
 *
 * @author Brian Clozel
 */
class MetricsWebFilterTests {

	private static final String REQUEST_METRICS_NAME = "http.server.requests";

	private SimpleMeterRegistry registry;

	private MetricsWebFilter webFilter;

	@BeforeEach
	public void setup() {
		MockClock clock = new MockClock();
		this.registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		this.webFilter = new MetricsWebFilter(this.registry, new DefaultWebFluxTagsProvider(), REQUEST_METRICS_NAME,
				AutoTimer.ENABLED);
	}

	@Test
	void filterAddsTagsToRegistry() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
	}

	@Test
	void filterAddsTagsToRegistryForExceptions() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.webFilter.filter(exchange, (serverWebExchange) -> Mono.error(new IllegalStateException("test error")))
				.onErrorResume((t) -> {
					exchange.getResponse().setStatusCodeValue(500);
					return exchange.getResponse().setComplete();
				}).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
		assertMetricsContainsTag("exception", "IllegalStateException");
	}

	@Test
	void filterAddsNonEmptyTagsToRegistryForAnonymousExceptions() {
		final Exception anonymous = new Exception("test error") {
		};

		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.webFilter.filter(exchange, (serverWebExchange) -> Mono.error(anonymous)).onErrorResume((t) -> {
			exchange.getResponse().setStatusCodeValue(500);
			return exchange.getResponse().setComplete();
		}).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
		assertMetricsContainsTag("exception", anonymous.getClass().getName());
	}

	@Test
	void filterAddsTagsToRegistryForExceptionsAndCommittedResponse() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.webFilter.filter(exchange, (serverWebExchange) -> {
			exchange.getResponse().setStatusCodeValue(500);
			return exchange.getResponse().setComplete().then(Mono.error(new IllegalStateException("test error")));
		}).onErrorResume((t) -> Mono.empty()).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
	}

	private MockServerWebExchange createExchange(String path, String pathPattern) {
		PathPatternParser parser = new PathPatternParser();
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
		exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, parser.parse(pathPattern));
		return exchange;
	}

	private void assertMetricsContainsTag(String tagKey, String tagValue) {
		assertThat(this.registry.get(REQUEST_METRICS_NAME).tag(tagKey, tagValue).timer().count()).isEqualTo(1);
	}

}
