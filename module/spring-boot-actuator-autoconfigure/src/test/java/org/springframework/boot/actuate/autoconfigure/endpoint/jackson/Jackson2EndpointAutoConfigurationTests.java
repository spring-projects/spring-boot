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

package org.springframework.boot.actuate.autoconfigure.endpoint.jackson;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Jackson2EndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3
 */
@SuppressWarnings("removal")
@Deprecated(since = "4.0.0", forRemoval = true)
class Jackson2EndpointAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Jackson2EndpointAutoConfiguration.class));

	@Test
	void endpointObjectMapperWhenNoProperty() {
		this.runner.run((context) -> assertThat(context)
			.hasSingleBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class));
	}

	@Test
	void endpointObjectMapperWhenPropertyTrue() {
		this.runner.run((context) -> assertThat(context)
			.hasSingleBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class));
	}

	@Test
	void endpointObjectMapperWhenPropertyFalse() {
		this.runner.withPropertyValues("management.endpoints.jackson.isolated-object-mapper=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class));
	}

	@Test
	void endpointObjectMapperWhenSpringWebIsAbsent() {
		this.runner.withClassLoader(new FilteredClassLoader(Jackson2ObjectMapperBuilder.class))
			.run((context) -> assertThat(context)
				.doesNotHaveBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class));
	}

	@Test
	void endpointObjectMapperDoesNotSerializeDatesAsTimestamps() {
		this.runner.run((context) -> {
			ObjectMapper objectMapper = context
				.getBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class)
				.get();
			Instant now = Instant.now();
			String json = objectMapper.writeValueAsString(Map.of("timestamp", now));
			assertThat(json).contains(DateTimeFormatter.ISO_INSTANT.format(now));
		});
	}

	@Test
	void endpointObjectMapperDoesNotSerializeDurationsAsTimestamps() {
		this.runner.run((context) -> {
			ObjectMapper objectMapper = context
				.getBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class)
				.get();
			Duration duration = Duration.ofSeconds(42);
			String json = objectMapper.writeValueAsString(Map.of("duration", duration));
			assertThat(json).contains(duration.toString());
		});
	}

	@Test
	void endpointObjectMapperDoesNotSerializeNullValues() {
		this.runner.run((context) -> {
			ObjectMapper objectMapper = context
				.getBean(org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper.class)
				.get();
			HashMap<String, String> map = new HashMap<>();
			map.put("key", null);
			String json = objectMapper.writeValueAsString(map);
			assertThat(json).isEqualTo("{}");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestEndpointMapperConfiguration {

		@Bean
		TestEndpointJsonMapper testEndpointJsonMapper() {
			return new TestEndpointJsonMapper();
		}

	}

	static class TestEndpointJsonMapper implements EndpointJsonMapper {

		@Override
		public JsonMapper get() {
			return new JsonMapper();
		}

	}

}
