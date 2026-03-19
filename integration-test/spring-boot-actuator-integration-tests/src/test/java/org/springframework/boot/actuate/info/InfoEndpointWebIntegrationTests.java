/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.info;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfoEndpoint} exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = "info.app.name=MyService")
class InfoEndpointWebIntegrationTests {

	private static final String ENDPOINT_PATH = "/actuator/info";

	@WebEndpointTest
	void shouldReturnInfoFromAllContributors(WebTestClient client) {
		var response = getEndpointResponse(client);

		verifySuccessStatus(response);
		verifyContributorContent(response, TestContributor.FIRST);
		verifyContributorContent(response, TestContributor.SECOND);
	}

	private ResponseSpec getEndpointResponse(WebTestClient client) {
		return client.get()
				.uri(ENDPOINT_PATH)
				.accept(MediaType.APPLICATION_JSON)
				.exchange();
	}

	private void verifySuccessStatus(ResponseSpec response) {
		response.expectStatus().isOk();
	}

	private void verifyContributorContent(ResponseSpec response, TestContributor contributor) {
		contributor.getExpectedData().forEach((key, value) -> {
			var jsonPath = contributor.buildJsonPath(key);
			response.expectBody()
					.jsonPath(jsonPath)
					.value(actual -> assertThat(actual).isEqualTo(value));
		});
	}

	enum TestContributor {

		FIRST("firstContributor", Map.of(
				"firstKey1", "firstValue1",
				"firstKey2", "firstValue2"
		)),

		SECOND("secondContributor", Map.of(
				"secondKey1", "secondValue1",
				"secondKey2", "secondValue2"
		));

		private final String name;
		private final Map<String, String> data;

		TestContributor(String name, Map<String, String> data) {
			this.name = name;
			this.data = data;
		}

		String getName() {
			return this.name;
		}

		Map<String, String> getExpectedData() {
			return this.data;
		}

		String buildJsonPath(String key) {
			return "%s.%s".formatted(this.name, key);
		}

		InfoContributor createContributor() {
			return builder -> builder.withDetail(this.name, this.data);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		InfoEndpoint infoEndpoint(ObjectProvider<InfoContributor> infoContributors) {
			return new InfoEndpoint(infoContributors.orderedStream().toList());
		}

		@Bean
		InfoContributor firstContributor() {
			return TestContributor.FIRST.createContributor();
		}

		@Bean
		InfoContributor secondContributor() {
			return TestContributor.SECOND.createContributor();
		}

	}

}
