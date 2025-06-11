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

package org.springframework.boot.webflux.autoconfigure.actuate.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.jackson.EndpointObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the WebFlux actuator endpoints.
 *
 * @author Andy Wilkinson
 */
class WebFluxEndpointIntegrationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, CodecsAutoConfiguration.class,
				WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class, EndpointAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class,
				BeansEndpointAutoConfiguration.class))
		.withUserConfiguration(EndpointsConfiguration.class);

	@Test
	void linksAreProvidedToAllEndpointTypes() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include:*").run((context) -> {
			WebTestClient client = createWebTestClient(context);
			client.get()
				.uri("/actuator")
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("_links.beans")
				.isNotEmpty()
				.jsonPath("_links.restcontroller")
				.isNotEmpty()
				.jsonPath("_links.controller")
				.isNotEmpty();
		});
	}

	@Test
	void linksPageIsNotAvailableWhenDisabled() {
		this.contextRunner.withPropertyValues("management.endpoints.web.discovery.enabled=false").run((context) -> {
			WebTestClient client = createWebTestClient(context);
			client.get().uri("/actuator").exchange().expectStatus().isNotFound();
		});
	}

	@Test
	void endpointObjectMapperCanBeApplied() {
		this.contextRunner.withUserConfiguration(EndpointObjectMapperConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include:*")
			.run((context) -> {
				WebTestClient client = createWebTestClient(context);
				client.get()
					.uri("/actuator/beans")
					.exchange()
					.expectStatus()
					.isOk()
					.expectBody()
					.consumeWith((result) -> {
						String json = new String(result.getResponseBody(), StandardCharsets.UTF_8);
						assertThat(json).contains("\"scope\":\"notelgnis\"");
					});
			});
	}

	private WebTestClient createWebTestClient(ApplicationContext context) {
		return WebTestClient.bindToApplicationContext(context)
			.configureClient()
			.baseUrl("https://spring.example.org")
			.build();
	}

	@org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint(id = "controller")
	@SuppressWarnings("removal")
	static class TestControllerEndpoint {

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "restcontroller")
	@SuppressWarnings("removal")
	static class TestRestControllerEndpoint {

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointsConfiguration {

		@Bean
		TestControllerEndpoint testControllerEndpoint() {
			return new TestControllerEndpoint();
		}

		@Bean
		TestRestControllerEndpoint testRestControllerEndpoint() {
			return new TestRestControllerEndpoint();
		}

	}

	@Configuration
	@SuppressWarnings({ "deprecation", "removal" })
	static class EndpointObjectMapperConfiguration {

		@Bean
		EndpointObjectMapper endpointObjectMapper() {
			SimpleModule module = new SimpleModule();
			module.addSerializer(String.class, new ReverseStringSerializer());
			ObjectMapper objectMapper = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
				.modules(module)
				.build();
			return () -> objectMapper;
		}

		static class ReverseStringSerializer extends StdScalarSerializer<Object> {

			ReverseStringSerializer() {
				super(String.class, false);
			}

			@Override
			public boolean isEmpty(SerializerProvider prov, Object value) {
				return ((String) value).isEmpty();
			}

			@Override
			public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
				serialize(value, gen);
			}

			@Override
			public final void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
					TypeSerializer typeSer) throws IOException {
				serialize(value, gen);
			}

			private void serialize(Object value, JsonGenerator gen) throws IOException {
				StringBuilder builder = new StringBuilder((String) value);
				gen.writeString(builder.reverse().toString());
			}

		}

	}

}
