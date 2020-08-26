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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.time.Duration;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MetricsWebFilter} for handlers with {@link Timed} annotation.
 *
 * @author Andrey Shlykov
 */
class MetricsWebFilterTimedAnnotationTests {

	private static final String REQUEST_METRICS_NAME = "http.server.requests";

	private static final String REQUEST_METRICS_NAME_PERCENTILE = REQUEST_METRICS_NAME + ".percentile";

	private SimpleMeterRegistry registry;

	private MetricsWebFilter webFilter;

	@BeforeEach
	void setup() {
		MockClock clock = new MockClock();
		this.registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		this.webFilter = new MetricsWebFilter(this.registry, new DefaultWebFluxTagsProvider(true), REQUEST_METRICS_NAME,
				AutoTimer.ENABLED);
	}

	@Test
	void filterAddsStandardTags() {
		MockServerWebExchange exchange = createExchange("timedHandler");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
	}

	@Test
	void filterAddsExtraTags() {
		MockServerWebExchange exchange = createExchange("timedExtraTagsHandler");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
		assertMetricsContainsTag("tag1", "value1");
		assertMetricsContainsTag("tag2", "value2");
	}

	@Test
	void filterAddsExtraTagsAndException() {
		MockServerWebExchange exchange = createExchange("timedExtraTagsHandler");
		this.webFilter.filter(exchange, (serverWebExchange) -> Mono.error(new IllegalStateException("test error")))
				.onErrorResume((t) -> {
					exchange.getResponse().setRawStatusCode(500);
					return exchange.getResponse().setComplete();
				}).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
		assertMetricsContainsTag("exception", "IllegalStateException");
		assertMetricsContainsTag("tag1", "value1");
		assertMetricsContainsTag("tag2", "value2");
	}

	@Test
	void filterAddsPercentileMeters() {
		MockServerWebExchange exchange = createExchange("timedPercentilesHandler");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
		assertThat(this.registry.get(REQUEST_METRICS_NAME_PERCENTILE).tag("phi", "0.95").gauge().value()).isNotZero();
		assertThat(this.registry.get(REQUEST_METRICS_NAME_PERCENTILE).tag("phi", "0.5").gauge().value()).isNotZero();
	}

	private MockServerWebExchange createExchange(String handlerName) {
		PathPatternParser parser = new PathPatternParser();
		HandlerMethod handlerMethod = new HandlerMethod(this, ReflectionUtils.findMethod(this.getClass(), handlerName));
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/projects/spring-boot").build());
		exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				parser.parse("/projects/{project}"));
		exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, handlerMethod);
		return exchange;
	}

	private void assertMetricsContainsTag(String tagKey, String tagValue) {
		assertThat(this.registry.get(REQUEST_METRICS_NAME).tag(tagKey, tagValue).timer().count()).isEqualTo(1);
	}

	@Timed
	Mono<String> timedHandler() {
		return Mono.just("test");
	}

	@Timed(extraTags = { "tag1", "value1", "tag2", "value2" })
	Mono<String> timedExtraTagsHandler() {
		return Mono.just("test");
	}

	@Timed(percentiles = { 0.5, 0.95 })
	Mono<String> timedPercentilesHandler() {
		return Mono.just("test");
	}

}
