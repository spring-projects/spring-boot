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

package org.springframework.boot.actuate.sbom;

import net.minidev.json.JSONArray;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SbomEndpoint} exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Moritz Halbritter
 */
class SbomEndpointWebIntegrationTests {

	@WebEndpointTest
	void shouldReturnSboms(WebTestClient client) {
		client.get()
			.uri("/actuator/sbom")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"))
			.expectBody()
			.jsonPath("$.ids")
			.value((value) -> assertThat(value).isEqualTo(new JSONArray().appendElement("application")));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		SbomProperties sbomProperties() {
			SbomProperties properties = new SbomProperties();
			properties.getApplication().setLocation("classpath:sbom/cyclonedx.json");
			return properties;
		}

		@Bean
		SbomEndpoint sbomEndpoint(SbomProperties properties, ResourceLoader resourceLoader) {
			return new SbomEndpoint(properties, resourceLoader);
		}

		@Bean
		SbomEndpointWebExtension sbomEndpointWebExtension(SbomEndpoint sbomEndpoint, SbomProperties properties) {
			return new SbomEndpointWebExtension(sbomEndpoint, properties);
		}

	}

}
