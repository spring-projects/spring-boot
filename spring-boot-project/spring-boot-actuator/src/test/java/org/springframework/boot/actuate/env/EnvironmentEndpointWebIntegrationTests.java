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

package org.springframework.boot.actuate.env;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

class EnvironmentEndpointWebIntegrationTests {

	private ConfigurableApplicationContext context;

	private WebTestClient client;

	@BeforeEach
	void prepareEnvironment(ConfigurableApplicationContext context, WebTestClient client) {
		TestPropertyValues.of("foo:bar", "fool:baz").applyTo(context);
		this.client = client;
		this.context = context;
	}

	@WebEndpointTest
	void home() {
		this.client.get()
			.uri("/actuator/env")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("propertySources[?(@.name=='systemProperties')]")
			.exists();
	}

	@WebEndpointTest
	void sub() {
		this.client.get()
			.uri("/actuator/env/foo")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("property.source")
			.isEqualTo("test")
			.jsonPath("property.value")
			.isEqualTo("bar");
	}

	@WebEndpointTest
	void regex() {
		Map<String, Object> map = new HashMap<>();
		map.put("food", null);
		this.context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("null-value", map));
		this.client.get()
			.uri("/actuator/env?pattern=foo.*")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath(forProperty("test", "foo"))
			.isEqualTo("bar")
			.jsonPath(forProperty("test", "fool"))
			.isEqualTo("baz");
	}

	@WebEndpointTest
	void nestedPathWhenPlaceholderCannotBeResolvedShouldReturnUnresolvedProperty() {
		Map<String, Object> map = new HashMap<>();
		map.put("my.foo", "${my.bar}");
		this.context.getEnvironment()
			.getPropertySources()
			.addFirst(new MapPropertySource("unresolved-placeholder", map));
		this.client.get()
			.uri("/actuator/env/my.foo")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("property.value")
			.isEqualTo("${my.bar}")
			.jsonPath(forPropertyEntry("unresolved-placeholder"))
			.isEqualTo("${my.bar}");
	}

	@WebEndpointTest
	void nestedPathForUnknownKeyShouldReturn404() {
		this.client.get().uri("/actuator/env/this.does.not.exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void nestedPathMatchedByRegexWhenPlaceholderCannotBeResolvedShouldReturnUnresolvedProperty() {
		Map<String, Object> map = new HashMap<>();
		map.put("my.foo", "${my.bar}");
		this.context.getEnvironment()
			.getPropertySources()
			.addFirst(new MapPropertySource("unresolved-placeholder", map));
		this.client.get()
			.uri("/actuator/env?pattern=my.*")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("propertySources[?(@.name=='unresolved-placeholder')].properties.['my.foo'].value")
			.isEqualTo("${my.bar}");
	}

	private String forProperty(String source, String name) {
		return "propertySources[?(@.name=='" + source + "')].properties.['" + name + "'].value";
	}

	private String forPropertyEntry(String source) {
		return "propertySources[?(@.name=='" + source + "')].property.value";
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		EnvironmentEndpoint endpoint(Environment environment) {
			return new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS);
		}

		@Bean
		EnvironmentEndpointWebExtension environmentEndpointWebExtension(EnvironmentEndpoint endpoint) {
			return new EnvironmentEndpointWebExtension(endpoint, Show.ALWAYS, Collections.emptySet());
		}

	}

}
