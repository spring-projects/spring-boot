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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsWebClientFilterFunction}
 *
 * @author Brian Clozel
 * @author Scott Frederick
 */
class MetricsWebClientFilterFunctionTests {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private MeterRegistry registry;

	private MetricsWebClientFilterFunction filterFunction;

	private final FaultyTagsProvider tagsProvider = new FaultyTagsProvider();

	private ClientResponse response;

	private ExchangeFunction exchange;

	@BeforeEach
	void setup() {
		this.registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
		this.filterFunction = new MetricsWebClientFilterFunction(this.registry, this.tagsProvider,
				"http.client.requests", AutoTimer.ENABLED);
		this.response = mock(ClientResponse.class);
		this.exchange = (r) -> Mono.just(this.response);
	}

	@Test
	void filterShouldRecordTimer() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		given(this.response.rawStatusCode()).willReturn(HttpStatus.OK.value());
		this.filterFunction.filter(request, this.exchange).block(Duration.ofSeconds(5));
		assertThat(this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "200").timer().count()).isEqualTo(1);
	}

	@Test
	void filterWhenUriTemplatePresentShouldRecordTimer() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE, "/projects/{project}").build();
		given(this.response.rawStatusCode()).willReturn(HttpStatus.OK.value());
		this.filterFunction.filter(request, this.exchange).block(Duration.ofSeconds(5));
		assertThat(this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/{project}", "status", "200").timer().count()).isEqualTo(1);
	}

	@Test
	void filterWhenIoExceptionThrownShouldRecordTimer() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		ExchangeFunction errorExchange = (r) -> Mono.error(new IOException());
		this.filterFunction.filter(request, errorExchange).onErrorResume(IOException.class, (t) -> Mono.empty())
				.block(Duration.ofSeconds(5));
		assertThat(this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "IO_ERROR").timer().count())
						.isEqualTo(1);
	}

	@Test
	void filterWhenExceptionThrownShouldRecordTimer() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		ExchangeFunction exchange = (r) -> Mono.error(new IllegalArgumentException());
		this.filterFunction.filter(request, exchange).onErrorResume(IllegalArgumentException.class, (t) -> Mono.empty())
				.block(Duration.ofSeconds(5));
		assertThat(this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "CLIENT_ERROR").timer().count())
						.isEqualTo(1);
	}

	@Test
	void filterWhenCancelThrownShouldRecordTimer() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		given(this.response.rawStatusCode()).willReturn(HttpStatus.OK.value());
		Mono<ClientResponse> filter = this.filterFunction.filter(request, this.exchange);
		StepVerifier.create(filter).thenCancel().verify(Duration.ofSeconds(5));
		assertThat(this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "CLIENT_ERROR").timer().count())
						.isEqualTo(1);
		assertThatThrownBy(() -> this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "200").timer())
						.isInstanceOf(MeterNotFoundException.class);
	}

	@Test
	void filterWhenCancelAfterResponseThrownShouldNotRecordTimer() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		given(this.response.rawStatusCode()).willReturn(HttpStatus.OK.value());
		Mono<ClientResponse> filter = this.filterFunction.filter(request, this.exchange);
		StepVerifier.create(filter).expectNextCount(1).thenCancel().verify(Duration.ofSeconds(5));
		assertThat(this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "200").timer().count()).isEqualTo(1);
		assertThatThrownBy(() -> this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "CLIENT_ERROR").timer())
						.isInstanceOf(MeterNotFoundException.class);
	}

	@Test
	void filterWhenExceptionAndRetryShouldNotAccumulateRecordTime() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		ExchangeFunction exchange = (r) -> Mono.error(new IllegalArgumentException())
				.delaySubscription(Duration.ofMillis(1000)).cast(ClientResponse.class);
		this.filterFunction.filter(request, exchange).retry(1)
				.onErrorResume(IllegalArgumentException.class, (t) -> Mono.empty()).block(Duration.ofSeconds(5));
		Timer timer = this.registry.get("http.client.requests")
				.tags("method", "GET", "uri", "/projects/spring-boot", "status", "CLIENT_ERROR").timer();
		assertThat(timer.count()).isEqualTo(2);
		assertThat(timer.max(TimeUnit.MILLISECONDS)).isLessThan(2000);
	}

	@Test
	void whenMetricsRecordingFailsThenFilteringSucceeds() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.com/projects/spring-boot")).build();
		given(this.response.rawStatusCode()).willReturn(HttpStatus.OK.value());
		this.tagsProvider.failOnce();
		this.filterFunction.filter(request, this.exchange).block(Duration.ofSeconds(5));
	}

	static class FaultyTagsProvider extends DefaultWebClientExchangeTagsProvider {

		private final AtomicBoolean fail = new AtomicBoolean(false);

		@Override
		public Iterable<Tag> tags(ClientRequest request, ClientResponse response, Throwable throwable) {
			if (this.fail.compareAndSet(true, false)) {
				throw new RuntimeException();
			}
			return super.tags(request, response, throwable);
		}

		void failOnce() {
			this.fail.set(true);
		}

	}

}
