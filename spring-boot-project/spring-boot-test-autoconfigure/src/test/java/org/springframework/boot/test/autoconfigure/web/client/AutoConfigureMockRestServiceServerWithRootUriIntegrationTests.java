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

package org.springframework.boot.test.autoconfigure.web.client;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer} with a
 * {@link RestTemplate} configured with a root URI.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureMockRestServiceServer
class AutoConfigureMockRestServiceServerWithRootUriIntegrationTests {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	MeterRegistry meterRegistry;

	@Test
	void whenRestTemplateAppliesARootUriThenMockServerExpecationsAreStillMatched() {
		this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/test", String.class);
		assertThat(entity.getBody()).isEqualTo("hello");
		assertThat(this.meterRegistry.find("http.client.requests").tag("uri", "/rest/test").timer()).isNotNull();
	}

	@EnableAutoConfiguration(exclude = CassandraAutoConfiguration.class)
	@Configuration(proxyBeanMethods = false)
	static class RootUriConfiguration {

		@Bean
		RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder.rootUri("/rest").build();
		}

	}

}
