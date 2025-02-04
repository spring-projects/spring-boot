/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Properties;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.expositionformats.OpenMetricsTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

/**
 * Tests for generating documentation describing the {@link PrometheusScrapeEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Johnny Lim
 */
class PrometheusScrapeEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void prometheus() {
		assertThat(this.mvc.get().uri("/actuator/prometheus")).hasStatusOk().apply(document("prometheus/all"));
	}

	@Test
	void prometheusOpenmetrics() {
		assertThat(this.mvc.get().uri("/actuator/prometheus").accept(OpenMetricsTextFormatWriter.CONTENT_TYPE))
			.satisfies((result) -> {
				assertThat(result).hasStatusOk()
					.headers()
					.hasValue("Content-Type", "application/openmetrics-text;version=1.0.0;charset=utf-8");
				assertThat(result).apply(document("prometheus/openmetrics"));
			});
	}

	@Test
	void filteredPrometheus() {
		assertThat(this.mvc.get()
			.uri("/actuator/prometheus")
			.param("includedNames", "jvm_memory_used_bytes,jvm_memory_committed_bytes"))
			.hasStatusOk()
			.apply(document("prometheus/names",
					queryParameters(parameterWithName("includedNames")
						.description("Restricts the samples to those that match the names. Optional.")
						.optional())));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		PrometheusScrapeEndpoint endpoint() {
			PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
			PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry((key) -> null, prometheusRegistry,
					Clock.SYSTEM);
			new JvmMemoryMetrics().bindTo(meterRegistry);
			return new PrometheusScrapeEndpoint(prometheusRegistry, new Properties());
		}

	}

}
