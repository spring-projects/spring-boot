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

package org.springframework.boot.test.autoconfigure.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer} with a
 * {@link RestClient} configured with a base URL.
 *
 * @author Scott Frederick
 */
@SpringBootTest
@AutoConfigureMockRestServiceServer
class AutoConfigureMockRestServiceServerWithRestClientIntegrationTests {

	@Autowired
	private RestClient restClient;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void mockServerExpectationsAreMatched() {
		this.server.expect(requestTo("/rest/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restClient.get().uri("/test").retrieve().toEntity(String.class);
		assertThat(entity.getBody()).isEqualTo("hello");
	}

	@EnableAutoConfiguration(exclude = CassandraAutoConfiguration.class)
	@Configuration(proxyBeanMethods = false)
	static class RootUriConfiguration {

		@Bean
		RestClient restClient(Builder restClientBuilder) {
			return restClientBuilder.baseUrl("/rest").build();
		}

	}

}
