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

package org.springframework.boot.actuate.context.properties;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Integration tests for {@link ConfigurationPropertiesReportEndpoint} exposed by Jersey,
 * Spring MVC, and WebFlux.
 *
 * @author Chris Bono
 */
class ConfigurationPropertiesReportEndpointWebIntegrationTests {

	private WebTestClient client;

	@BeforeEach
	void prepareEnvironment(ConfigurableApplicationContext context, WebTestClient client) {
		TestPropertyValues.of("com.foo.name=fooz", "com.bar.name=barz").applyTo(context);
		this.client = client;
	}

	@WebEndpointTest
	void noFilters() {
		this.client.get()
			.uri("/actuator/configprops")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$..beans[*]")
			.value(hasSize(greaterThanOrEqualTo(2)))
			.jsonPath("$..beans['fooDotCom']")
			.exists()
			.jsonPath("$..beans['barDotCom']")
			.exists();
	}

	@WebEndpointTest
	void filterByExactPrefix() {
		this.client.get()
			.uri("/actuator/configprops/com.foo")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$..beans[*]")
			.value(hasSize(1))
			.jsonPath("$..beans['fooDotCom']")
			.exists();
	}

	@WebEndpointTest
	void filterByGeneralPrefix() {
		this.client.get()
			.uri("/actuator/configprops/com.")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$..beans[*]")
			.value(hasSize(2))
			.jsonPath("$..beans['fooDotCom']")
			.exists()
			.jsonPath("$..beans['barDotCom']")
			.exists();
	}

	@WebEndpointTest
	void filterByNonExistentPrefix() {
		this.client.get().uri("/actuator/configprops/com.zoo").exchange().expectStatus().isNotFound();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class TestConfiguration {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), null);
		}

		@Bean
		ConfigurationPropertiesReportEndpointWebExtension endpointWebExtension(
				ConfigurationPropertiesReportEndpoint endpoint) {
			return new ConfigurationPropertiesReportEndpointWebExtension(endpoint, Show.ALWAYS, Collections.emptySet());
		}

		@Bean
		@ConfigurationProperties(prefix = "com.foo")
		Foo fooDotCom() {
			return new Foo();
		}

		@Bean
		@ConfigurationProperties(prefix = "com.bar")
		Bar barDotCom() {
			return new Bar();
		}

	}

	public static class Foo {

		private String name = "5150";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class Bar {

		private String name = "6160";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
