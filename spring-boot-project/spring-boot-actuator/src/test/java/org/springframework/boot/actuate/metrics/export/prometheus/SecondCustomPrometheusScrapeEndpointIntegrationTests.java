/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.metrics.export.prometheus;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.metrics.expositionformats.OpenMetricsTextFormatWriter;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for exposing a {@link PrometheusScrapeEndpoint} and
 * {@link PrometheusSimpleclientScrapeEndpoint} with different IDs.
 *
 * @author Jon Schneider
 * @author Johnny Lim
 */
class SecondCustomPrometheusScrapeEndpointIntegrationTests {

	@WebEndpointTest
	void scrapeHasContentTypeText004ByDefault(WebTestClient client) {
		String expectedContentType = PrometheusTextFormatWriter.CONTENT_TYPE;
		client.get()
			.uri("/actuator/prometheus")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType(expectedContentType))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.contains("counter3_total"));
		client.get()
			.uri("/actuator/prometheussc")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType(expectedContentType))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.contains("counter3_total"));
	}

	@WebEndpointTest
	void scrapeHasContentTypeText004ByDefaultWhenClientAcceptsWildcardWithParameter(WebTestClient client) {
		String expectedContentType = PrometheusTextFormatWriter.CONTENT_TYPE;
		String accept = "*/*;q=0.8";
		client.get()
			.uri("/actuator/prometheus")
			.accept(MediaType.parseMediaType(accept))
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType(expectedContentType))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.contains("counter3_total"));
		client.get()
			.uri("/actuator/prometheussc")
			.accept(MediaType.parseMediaType(accept))
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType(expectedContentType))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.contains("counter3_total"));
	}

	@WebEndpointTest
	void scrapeCanProduceOpenMetrics100(WebTestClient client) {
		MediaType openMetrics = MediaType.parseMediaType(OpenMetricsTextFormatWriter.CONTENT_TYPE);
		client.get()
			.uri("/actuator/prometheus")
			.accept(openMetrics)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(openMetrics)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.contains("counter3_total"));
		client.get()
			.uri("/actuator/prometheussc")
			.accept(openMetrics)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(openMetrics)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.contains("counter3_total"));
	}

	@WebEndpointTest
	void scrapePrefersToProduceOpenMetrics100(WebTestClient client) {
		MediaType openMetrics = MediaType.parseMediaType(OpenMetricsTextFormatWriter.CONTENT_TYPE);
		MediaType textPlain = MediaType.parseMediaType(PrometheusTextFormatWriter.CONTENT_TYPE);
		client.get()
			.uri("/actuator/prometheus")
			.accept(openMetrics, textPlain)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(openMetrics);
		client.get()
			.uri("/actuator/prometheussc")
			.accept(openMetrics, textPlain)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(openMetrics);
	}

	@WebEndpointTest
	void scrapeWithIncludedNames(WebTestClient client) {
		client.get()
			.uri("/actuator/prometheus?includedNames=counter1,counter2")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType(PrometheusTextFormatWriter.CONTENT_TYPE))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.doesNotContain("counter3_total"));
		client.get()
			.uri("/actuator/prometheussc?includedNames=counter1_total,counter2_total")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType(PrometheusTextFormatWriter.CONTENT_TYPE))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("counter1_total")
				.contains("counter2_total")
				.doesNotContain("counter3_total"));
	}

	@SuppressWarnings({ "deprecation", "removal" })
	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		PrometheusScrapeEndpoint prometheusScrapeEndpoint(PrometheusRegistry prometheusRegistry) {
			return new PrometheusScrapeEndpoint(prometheusRegistry);
		}

		@Bean
		CustomPrometheusScrapeEndpoint customPrometheusScrapeEndpoint(CollectorRegistry collectorRegistry) {
			return new CustomPrometheusScrapeEndpoint(collectorRegistry);
		}

		@Bean
		PrometheusRegistry prometheusRegistry() {
			return new PrometheusRegistry();
		}

		@Bean
		CollectorRegistry collectorRegistry() {
			return new CollectorRegistry(true);
		}

		@Bean
		PrometheusMeterRegistry registry(PrometheusRegistry prometheusRegistry) {
			return new PrometheusMeterRegistry((k) -> null, prometheusRegistry, Clock.SYSTEM);
		}

		@Bean
		io.micrometer.prometheus.PrometheusMeterRegistry oldRegistry(CollectorRegistry collectorRegistry) {
			return new io.micrometer.prometheus.PrometheusMeterRegistry((k) -> null, collectorRegistry, Clock.SYSTEM);
		}

		@Bean
		CompositeMeterRegistry compositeMeterRegistry(PrometheusMeterRegistry prometheusMeterRegistry,
				io.micrometer.prometheus.PrometheusMeterRegistry prometheusSCMeterRegistry) {
			CompositeMeterRegistry composite = new CompositeMeterRegistry();
			composite.add(prometheusMeterRegistry).add(prometheusSCMeterRegistry);
			Counter.builder("counter1").register(composite);
			Counter.builder("counter2").register(composite);
			Counter.builder("counter3").register(composite);
			return composite;
		}

		@WebEndpoint(id = "prometheussc")
		static class CustomPrometheusScrapeEndpoint extends PrometheusSimpleclientScrapeEndpoint {

			CustomPrometheusScrapeEndpoint(CollectorRegistry collectorRegistry) {
				super(collectorRegistry);
			}

		}

	}

}
