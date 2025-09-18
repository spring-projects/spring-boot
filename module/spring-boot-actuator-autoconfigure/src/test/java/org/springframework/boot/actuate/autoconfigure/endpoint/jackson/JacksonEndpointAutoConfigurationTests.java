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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class JacksonEndpointAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonEndpointAutoConfiguration.class));

	@Test
	void endpointJsonMapperWhenNoProperty() {
		this.runner.run((context) -> assertThat(context).hasSingleBean(EndpointJsonMapper.class));
	}

	@Test
	void endpointJsonMapperWhenPropertyTrue() {
		this.runner.withPropertyValues("management.endpoints.jackson.isolated-object-mapper=true")
			.run((context) -> assertThat(context).hasSingleBean(EndpointJsonMapper.class));
	}

	@Test
	void endpointJsonMapperWhenPropertyFalse() {
		this.runner.withPropertyValues("management.endpoints.jackson.isolated-object-mapper=false")
			.run((context) -> assertThat(context).doesNotHaveBean(EndpointJsonMapper.class));
	}

	@Test
	void endpointJsonMapperDoesNotSerializeDatesAsTimestamps() {
		this.runner.run((context) -> {
			JsonMapper jsonMapper = context.getBean(EndpointJsonMapper.class).get();
			Instant now = Instant.now();
			String json = jsonMapper.writeValueAsString(Map.of("timestamp", now));
			assertThat(json).contains(DateTimeFormatter.ISO_INSTANT.format(now));
		});
	}

	@Test
	void endpointJsonMapperDoesNotSerializeDurationsAsTimestamps() {
		this.runner.run((context) -> {
			JsonMapper jsonMapper = context.getBean(EndpointJsonMapper.class).get();
			Duration duration = Duration.ofSeconds(42);
			String json = jsonMapper.writeValueAsString(Map.of("duration", duration));
			assertThat(json).contains(duration.toString());
		});
	}

	@Test
	void endpointJsonMapperDoesNotSerializeNullValues() {
		this.runner.run((context) -> {
			JsonMapper jsonMapper = context.getBean(EndpointJsonMapper.class).get();
			HashMap<String, String> map = new HashMap<>();
			map.put("key", null);
			String json = jsonMapper.writeValueAsString(map);
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
