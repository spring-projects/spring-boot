/*
 * Copyright 2012-2023 the original author or authors.
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Tests for {@link OpenTelemetryMetricsConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 */
@SpringJUnitConfig
@TestPropertySource(properties = { "management.otlp.metrics.export.resource-attributes.service.name=test",
		"management.otlp.metrics.export.step=1s" })
@Testcontainers(disabledWithoutDocker = true)
class OpenTelemetryMetricsConnectionDetailsFactoryIntegrationTests {

	private static final String OPENMETRICS_001 = "application/openmetrics-text; version=0.0.1; charset=utf-8";

	private static final String CONFIG_FILE_NAME = "collector-config.yml";

	@Container
	@ServiceConnection
	static final GenericContainer<?> container = new GenericContainer<>(DockerImageNames.opentelemetry())
		.withCommand("--config=/etc/" + CONFIG_FILE_NAME)
		.withCopyToContainer(MountableFile.forClasspathResource(CONFIG_FILE_NAME), "/etc/" + CONFIG_FILE_NAME)
		.withExposedPorts(4318, 9090);

	@Autowired
	private MeterRegistry meterRegistry;

	@Test
	void connectionCanBeMadeToOpenTelemetryCollectorContainer() {
		Counter.builder("test.counter").register(this.meterRegistry).increment(42);
		Gauge.builder("test.gauge", () -> 12).register(this.meterRegistry);
		Timer.builder("test.timer").register(this.meterRegistry).record(Duration.ofMillis(123));
		DistributionSummary.builder("test.distributionsummary").register(this.meterRegistry).record(24);

		Awaitility.await()
			.atMost(Duration.ofSeconds(5))
			.pollDelay(Duration.ofMillis(100))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> whenPrometheusScraped().then()
				.statusCode(200)
				.contentType(OPENMETRICS_001)
				.body(endsWith("# EOF\n")));

		whenPrometheusScraped().then()
			.body(containsString(
					"{job=\"test\",service_name=\"test\",telemetry_sdk_language=\"java\",telemetry_sdk_name=\"io.micrometer\""),

					matchesPattern("(?s)^.*test_counter\\{.+} 42\\.0\\n.*$"),
					matchesPattern("(?s)^.*test_gauge\\{.+} 12\\.0\\n.*$"),

					matchesPattern("(?s)^.*test_timer_count\\{.+} 1\\n.*$"),

					matchesPattern("(?s)^.*test_timer_sum\\{.+} 123\\.0\\n.*$"),
					matchesPattern("(?s)^.*test_timer_bucket\\{.+,le=\"\\+Inf\"} 1\\n.*$"),

					matchesPattern("(?s)^.*test_distributionsummary_count\\{.+} 1\\n.*$"),
					matchesPattern("(?s)^.*test_distributionsummary_sum\\{.+} 24\\.0\\n.*$"),
					matchesPattern("(?s)^.*test_distributionsummary_bucket\\{.+,le=\"\\+Inf\"} 1\\n.*$"));
	}

	private Response whenPrometheusScraped() {
		return RestAssured.given().port(container.getMappedPort(9090)).accept(OPENMETRICS_001).when().get("/metrics");
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
