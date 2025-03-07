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

package org.springframework.boot.actuate.integration;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link IntegrationGraphEndpoint} exposed by Jersey, Spring MVC,
 * and WebFlux.
 *
 * @author Tim Ysewyn
 */
class IntegrationGraphEndpointWebIntegrationTests {

	@WebEndpointTest
	void graph(WebTestClient client) {
		client.get()
			.uri("/actuator/integrationgraph")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("contentDescriptor.providerVersion")
			.isNotEmpty()
			.jsonPath("contentDescriptor.providerFormatVersion")
			.isEqualTo(1.2f)
			.jsonPath("contentDescriptor.provider")
			.isEqualTo("spring-integration");
	}

	@WebEndpointTest
	void rebuild(WebTestClient client) {
		client.post()
			.uri("/actuator/integrationgraph")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isNoContent();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class TestConfiguration {

		@Bean
		IntegrationGraphEndpoint endpoint(IntegrationGraphServer integrationGraphServer) {
			return new IntegrationGraphEndpoint(integrationGraphServer);
		}

		@Bean
		IntegrationGraphServer integrationGraphServer() {
			return new IntegrationGraphServer();
		}

	}

}
