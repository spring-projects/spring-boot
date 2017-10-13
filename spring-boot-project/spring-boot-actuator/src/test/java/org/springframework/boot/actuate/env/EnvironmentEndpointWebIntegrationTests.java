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

package org.springframework.boot.actuate.env;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(WebEndpointRunners.class)
public class EnvironmentEndpointWebIntegrationTests {

	private static WebTestClient client;

	private static ConfigurableApplicationContext context;

	@Before
	public void prepareEnvironment() {
		TestPropertyValues.of("foo:bar", "fool:baz").applyTo(context);
	}

	@Test
	public void home() throws Exception {
		client.get().uri("/application/env").exchange().expectStatus().isOk().expectBody()
				.jsonPath("propertySources[?(@.name=='systemProperties')]").exists();
	}

	@Test
	public void sub() throws Exception {
		client.get().uri("/application/env/foo").exchange().expectStatus().isOk()
				.expectBody().jsonPath("property.source").isEqualTo("test")
				.jsonPath("property.value").isEqualTo("bar");
	}

	@Test
	public void regex() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("food", null);
		EnvironmentEndpointWebIntegrationTests.context.getEnvironment()
				.getPropertySources().addFirst(new MapPropertySource("null-value", map));
		client.get().uri("/application/env?pattern=foo.*").exchange().expectStatus()
				.isOk().expectBody().jsonPath(forProperty("test", "foo")).isEqualTo("bar")
				.jsonPath(forProperty("test", "fool")).isEqualTo("baz");
	}

	@Test
	public void nestedPathWhenPlaceholderCannotBeResolvedShouldReturnUnresolvedProperty()
			throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("my.foo", "${my.bar}");
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("unresolved-placeholder", map));
		client.get().uri("/application/env/my.foo").exchange().expectStatus().isOk()
				.expectBody().jsonPath("property.value").isEqualTo("${my.bar}")
				.jsonPath(forPropertyEntry("unresolved-placeholder"))
				.isEqualTo("${my.bar}");
	}

	@Test
	public void nestedPathWithSensitivePlaceholderShouldSanitize() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("my.foo", "${my.password}");
		map.put("my.password", "hello");
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("placeholder", map));
		client.get().uri("/application/env/my.foo").exchange().expectStatus().isOk()
				.expectBody().jsonPath("property.value").isEqualTo("******")
				.jsonPath(forPropertyEntry("placeholder")).isEqualTo("******");
	}

	@Test
	public void nestedPathForUnknownKeyShouldReturn404AndBody() throws Exception {
		client.get().uri("/application/env/this.does.not.exist").exchange().expectStatus()
				.isNotFound().expectBody().jsonPath("property").doesNotExist()
				.jsonPath("propertySources[?(@.name=='test')]").exists()
				.jsonPath("propertySources[?(@.name=='systemProperties')]").exists()
				.jsonPath("propertySources[?(@.name=='systemEnvironment')]").exists();
	}

	@Test
	public void nestedPathMatchedByRegexWhenPlaceholderCannotBeResolvedShouldReturnUnresolvedProperty()
			throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("my.foo", "${my.bar}");
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("unresolved-placeholder", map));
		client.get().uri("/application/env?pattern=my.*").exchange().expectStatus().isOk()
				.expectBody()
				.jsonPath(
						"propertySources[?(@.name=='unresolved-placeholder')].properties.['my.foo'].value")
				.isEqualTo("${my.bar}");
	}

	@Test
	public void nestedPathMatchedByRegexWithSensitivePlaceholderShouldSanitize()
			throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("my.foo", "${my.password}");
		map.put("my.password", "hello");
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("placeholder", map));
		client.get().uri("/application/env?pattern=my.*").exchange().expectStatus().isOk()
				.expectBody().jsonPath(forProperty("placeholder", "my.foo"))
				.isEqualTo("******");
	}

	private String forProperty(String source, String name) {
		return "propertySources[?(@.name=='" + source + "')].properties.['" + name
				+ "'].value";
	}

	private String forPropertyEntry(String source) {
		return "propertySources[?(@.name=='" + source + "')].property.value";
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public EnvironmentEndpoint endpoint(Environment environment) {
			return new EnvironmentEndpoint(environment);
		}

		@Bean
		public EnvironmentEndpointWebExtension environmentEndpointWebExtension(
				EnvironmentEndpoint endpoint) {
			return new EnvironmentEndpointWebExtension(endpoint);
		}

	}

}
