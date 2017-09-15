/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.JvmMemoryMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Web integration tests for {@link MetricsEndpoint}.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 */
@RunWith(WebEndpointRunners.class)
public class MetricsEndpointWebIntegrationTests {

	private static WebTestClient client;

	private final ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	@Test
	public void listNames() throws IOException {
		String responseBody = MetricsEndpointWebIntegrationTests.client.get()
				.uri("/application/metrics").exchange().expectStatus().isOk()
				.expectBody(String.class).returnResult().getResponseBody();
		Map<String, List<String>> names = this.mapper.readValue(responseBody, Map.class);
		assertThat(names.get("names")).contains("jvm.memory.used");
	}

	@Test
	public void selectByName() throws IOException {
		MetricsEndpointWebIntegrationTests.client.get()
				.uri("/application/metrics/jvm.memory.used").exchange().expectStatus()
				.isOk().expectBody()
				.jsonPath("['jvmMemoryUsed.area.nonheap.id.Compressed_Class_Space']")
				.exists().jsonPath("['jvmMemoryUsed.area.heap.id.PS_Old_Gen']");
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public MeterRegistry registry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		public MetricsEndpoint metricsEndpoint(MeterRegistry meterRegistry) {
			return new MetricsEndpoint(meterRegistry);
		}

		@Bean
		public JvmMemoryMetrics jvmMemoryMetrics(MeterRegistry meterRegistry) {
			JvmMemoryMetrics memoryMetrics = new JvmMemoryMetrics();
			memoryMetrics.bindTo(meterRegistry);
			return memoryMetrics;
		}

	}

}
