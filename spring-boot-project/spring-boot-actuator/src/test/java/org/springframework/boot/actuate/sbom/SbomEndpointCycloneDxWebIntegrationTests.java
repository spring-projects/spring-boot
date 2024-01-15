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

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link SbomEndpoint} exposed by Jersey, Spring MVC, and WebFlux
 * in CycloneDX format.
 *
 * @author Moritz Halbritter
 */
class SbomEndpointCycloneDxWebIntegrationTests {

	@WebEndpointTest
	void shouldReturnSbomContent(WebTestClient client) {
		client.get()
			.uri("/actuator/sbom/application")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.parseMediaType("application/vnd.cyclonedx+json"))
			.expectBody()
			.jsonPath("$.bomFormat")
			.isEqualTo("CycloneDX");
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
