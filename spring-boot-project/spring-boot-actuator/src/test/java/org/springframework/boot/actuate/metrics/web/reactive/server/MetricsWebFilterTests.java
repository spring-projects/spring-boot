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

package org.springframework.boot.actuate.metrics.web.reactive.server;

import java.io.EOFException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsWebFilter}
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class MetricsWebFilterTests {

	private static final String REQUEST_METRICS_NAME = "http.server.requests";

	private static final String REQUEST_METRICS_NAME_PERCENTILE = REQUEST_METRICS_NAME + ".percentile";

	private final FaultyWebFluxTagsProvider tagsProvider = new FaultyWebFluxTagsProvider();

	private SimpleMeterRegistry registry;

	private MetricsWebFilter webFilter;

	@BeforeEach
	void setup() {
		MockClock clock = new MockClock();
		this.registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		this.webFilter = new MetricsWebFilter(this.registry, this.tagsProvider, REQUEST_METRICS_NAME,
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
					exchange.getResponse().setRawStatusCode(500);
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
			exchange.getResponse().setRawStatusCode(500);
			return exchange.getResponse().setComplete();
		}).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
		assertMetricsContainsTag("exception", anonymous.getClass().getName());
	}

	@Test
	void filterAddsTagsToRegistryForHandledExceptions() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.webFilter.filter(exchange, (serverWebExchange) -> {
			exchange.getAttributes().put(ErrorAttributes.ERROR_ATTRIBUTE, new IllegalStateException("test error"));
			return exchange.getResponse().setComplete();
		}).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
		assertMetricsContainsTag("exception", "IllegalStateException");
	}

	@Test
	void filterAddsTagsToRegistryForExceptionsAndCommittedResponse() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.webFilter.filter(exchange, (serverWebExchange) -> {
			exchange.getResponse().setRawStatusCode(500);
			return exchange.getResponse().setComplete().then(Mono.error(new IllegalStateException("test error")));
		}).onErrorResume((t) -> Mono.empty()).block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
	}

	@Test
	void trailingSlashShouldNotRecordDuplicateMetrics() {
		MockServerWebExchange exchange1 = createExchange("/projects/spring-boot", "/projects/{project}");
		MockServerWebExchange exchange2 = createExchange("/projects/spring-boot", "/projects/{project}/");
		this.webFilter.filter(exchange1, (serverWebExchange) -> exchange1.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		this.webFilter.filter(exchange2, (serverWebExchange) -> exchange2.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertThat(this.registry.get(REQUEST_METRICS_NAME).tag("uri", "/projects/{project}").timer().count())
				.isEqualTo(2);
		assertThat(this.registry.get(REQUEST_METRICS_NAME).tag("status", "200").timer().count()).isEqualTo(2);
	}

	@Test
	void cancelledConnectionsShouldProduceMetrics() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		Mono<Void> processing = this.webFilter.filter(exchange,
				(serverWebExchange) -> exchange.getResponse().setComplete());
		StepVerifier.create(processing).thenCancel().verify(Duration.ofSeconds(5));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
		assertMetricsContainsTag("outcome", "UNKNOWN");
	}

	@Test
	void disconnectedExceptionShouldProduceMetrics() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		Mono<Void> processing = this.webFilter
				.filter(exchange, (serverWebExchange) -> Mono.error(new EOFException("Disconnected")))
				.onErrorResume((t) -> {
					exchange.getResponse().setRawStatusCode(500);
					return exchange.getResponse().setComplete();
				});
		StepVerifier.create(processing).expectComplete().verify(Duration.ofSeconds(5));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "500");
		assertMetricsContainsTag("outcome", "UNKNOWN");
	}

	@Test
	void filterAddsStandardTags() {
		MockServerWebExchange exchange = createTimedHandlerMethodExchange("timed");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
	}

	@Test
	void filterAddsExtraTags() {
		MockServerWebExchange exchange = createTimedHandlerMethodExchange("timedExtraTags");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
		assertMetricsContainsTag("tag1", "value1");
		assertMetricsContainsTag("tag2", "value2");
	}

	@Test
	void filterAddsExtraTagsAndException() {
		MockServerWebExchange exchange = createTimedHandlerMethodExchange("timedExtraTags");
		this.webFilter.filter(exchange, (serverWebExchange) -> Mono.error(new IllegalStateException("test error")))
				.onErrorResume((ex) -> {
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
		MockServerWebExchange exchange = createTimedHandlerMethodExchange("timedPercentiles");
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
		assertMetricsContainsTag("uri", "/projects/{project}");
		assertMetricsContainsTag("status", "200");
		assertThat(this.registry.get(REQUEST_METRICS_NAME_PERCENTILE).tag("phi", "0.95").gauge().value()).isNotZero();
		assertThat(this.registry.get(REQUEST_METRICS_NAME_PERCENTILE).tag("phi", "0.5").gauge().value()).isNotZero();
	}

	@Test
	void whenMetricsRecordingFailsThenExchangeFilteringSucceeds() {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		this.tagsProvider.failOnce();
		this.webFilter.filter(exchange, (serverWebExchange) -> exchange.getResponse().setComplete())
				.block(Duration.ofSeconds(30));
	}

	private MockServerWebExchange createTimedHandlerMethodExchange(String methodName) {
		MockServerWebExchange exchange = createExchange("/projects/spring-boot", "/projects/{project}");
		exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
				new HandlerMethod(this, ReflectionUtils.findMethod(Handlers.class, methodName)));
		return exchange;
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

	static class Handlers {

		@Timed
		Mono<String> timed() {
			return Mono.just("test");
		}

		@Timed(extraTags = { "tag1", "value1", "tag2", "value2" })
		Mono<String> timedExtraTags() {
			return Mono.just("test");
		}

		@Timed(percentiles = { 0.5, 0.95 })
		Mono<String> timedPercentiles() {
			return Mono.just("test");
		}

	}

	class FaultyWebFluxTagsProvider extends DefaultWebFluxTagsProvider {

		private final AtomicBoolean fail = new AtomicBoolean(false);

		FaultyWebFluxTagsProvider() {
			super(true);
		}

		@Override
		public Iterable<Tag> httpRequestTags(ServerWebExchange exchange, Throwable exception) {
			if (this.fail.compareAndSet(true, false)) {
				throw new RuntimeException();
			}
			return super.httpRequestTags(exchange, exception);
		}

		void failOnce() {
			this.fail.set(true);
		}

	}

}
