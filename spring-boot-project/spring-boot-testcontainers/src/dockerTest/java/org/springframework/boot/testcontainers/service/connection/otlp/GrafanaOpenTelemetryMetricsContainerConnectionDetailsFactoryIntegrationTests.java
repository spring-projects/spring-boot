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

package org.springframework.boot.testcontainers.service.connection.otlp;

import java.time.Duration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrafanaOpenTelemetryMetricsContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@TestPropertySource(properties = { "management.otlp.metrics.export.resource-attributes.service.name=test",
		"management.otlp.metrics.export.step=1s" })
@Testcontainers(disabledWithoutDocker = true)
class GrafanaOpenTelemetryMetricsContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final LgtmStackContainer container = TestImage.container(LgtmStackContainer.class);

	@Autowired
	private MeterRegistry meterRegistry;

	@Test
	void connectionCanBeMadeToOpenTelemetryCollectorContainer() {
		Counter.builder("test.counter").register(this.meterRegistry).increment(42);
		Gauge.builder("test.gauge", () -> 12).register(this.meterRegistry);
		Timer.builder("test.timer").register(this.meterRegistry).record(Duration.ofMillis(123));
		DistributionSummary.builder("test.distributionsummary").register(this.meterRegistry).record(24);

		Awaitility.given()
			.pollInterval(Duration.ofSeconds(2))
			.atMost(Duration.ofSeconds(10))
			.ignoreExceptions()
			.untilAsserted(() -> {
				Response response = RestAssured.given()
					.queryParam("query", "{job=\"test\"}")
					.get("%s/api/v1/query".formatted(container.getPromehteusHttpUrl()))
					.prettyPeek()
					.thenReturn();
				assertThat(response.getStatusCode()).isEqualTo(200);
				assertThat(response.body()
					.jsonPath()
					.getList("data.result.find { it.metric.__name__ == 'test_counter_total' }.value")).contains("42");
				assertThat(response.body()
					.jsonPath()
					.getList("data.result.find { it.metric.__name__ == 'test_gauge' }.value")).contains("12");
				assertThat(response.body()
					.jsonPath()
					.getList("data.result.find { it.metric.__name__ == 'test_timer_milliseconds_count' }.value"))
					.contains("1");
				assertThat(response.body()
					.jsonPath()
					.getList("data.result.find { it.metric.__name__ == 'test_timer_milliseconds_sum' }.value"))
					.contains("123");
				assertThat(response.body()
					.jsonPath()
					.getList(
							"data.result.find { it.metric.__name__ == 'test_timer_milliseconds_bucket' & it.metric.le == '+Inf' }.value"))
					.contains("1");
				assertThat(response.body()
					.jsonPath()
					.getList("data.result.find { it.metric.__name__ == 'test_distributionsummary_count' }.value"))
					.contains("1");
				assertThat(response.body()
					.jsonPath()
					.getList("data.result.find { it.metric.__name__ == 'test_distributionsummary_sum' }.value"))
					.contains("24");
				assertThat(response.body()
					.jsonPath()
					.getList(
							"data.result.find { it.metric.__name__ == 'test_distributionsummary_bucket' & it.metric.le == '+Inf' }.value"))
					.contains("1");
			});
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(OtlpMetricsExportAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

}
