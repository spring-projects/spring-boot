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

import java.util.Properties;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.expositionformats.OpenMetricsTextFormatWriter;
import io.prometheus.metrics.expositionformats.PrometheusProtobufWriter;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrometheusScrapeEndpoint}.
 *
 * @author Jon Schneider
 * @author Johnny Lim
 */
class PrometheusScrapeEndpointIntegrationTests {

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
	}

	@WebEndpointTest
	void scrapeCanProducePrometheusProtobuf(WebTestClient client) {
		MediaType prometheusProtobuf = MediaType.parseMediaType(PrometheusProtobufWriter.CONTENT_TYPE);
		client.get()
			.uri("/actuator/prometheus")
			.accept(prometheusProtobuf)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(prometheusProtobuf)
			.expectBody(byte[].class)
			.value((body) -> assertThat(body).isNotEmpty());
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		PrometheusScrapeEndpoint prometheusScrapeEndpoint(PrometheusRegistry prometheusRegistry) {
			return new PrometheusScrapeEndpoint(prometheusRegistry, new Properties());
		}

		@Bean
		PrometheusRegistry prometheusRegistry() {
			return new PrometheusRegistry();
		}

		@Bean
		MeterRegistry registry(PrometheusRegistry prometheusRegistry) {
			PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry((k) -> null, prometheusRegistry,
					Clock.SYSTEM);
			Counter.builder("counter1").register(meterRegistry);
			Counter.builder("counter2").register(meterRegistry);
			Counter.builder("counter3").register(meterRegistry);
			return meterRegistry;
		}

	}

}
