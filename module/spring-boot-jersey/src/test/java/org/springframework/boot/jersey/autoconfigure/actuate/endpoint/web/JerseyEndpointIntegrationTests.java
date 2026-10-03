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

package org.springframework.boot.jersey.autoconfigure.actuate.endpoint.web;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.glassfish.jersey.server.ResourceConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration;
import org.springframework.boot.jersey.autoconfigure.JerseyJacksonAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Jersey actuator endpoints.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Kristoffer Larsen Hopland
 */
class JerseyEndpointIntegrationTests {

	@Test
	void linksAreProvidedToAllEndpointTypes() {
		testJerseyEndpoints(new Class<?>[] { EndpointsConfiguration.class, ResourceConfigConfiguration.class });
	}

	@Test
	void linksPageIsNotAvailableWhenDisabled() {
		getContextRunner(new Class<?>[] { EndpointsConfiguration.class, ResourceConfigConfiguration.class })
			.withPropertyValues("management.endpoints.web.discovery.enabled:false")
			.run((context) -> {
				WebServer webServer = context
					.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
					.getWebServer();
				assertThat(webServer).isNotNull();
				int port = webServer.getPort();
				WebTestClient client = WebTestClient.bindToServer()
					.baseUrl("http://localhost:" + port)
					.responseTimeout(Duration.ofMinutes(5))
					.build();
				client.get().uri("/actuator").exchange().expectStatus().isNotFound();
			});
	}

	@Test
	void actuatorEndpointsWhenUserProvidedResourceConfigBeanNotAvailable() {
		testJerseyEndpoints(new Class<?>[] { EndpointsConfiguration.class });
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void endpointJsonMapperCanBeApplied(boolean separateManagementPort) {
		getContextRunner(
				new Class<?>[] { EndpointsConfiguration.class, ResourceConfigConfiguration.class,
						EndpointJsonMapperConfiguration.class },
				TomcatServletManagementContextAutoConfiguration.class, ServletManagementContextAutoConfiguration.class)
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("management.server.port=" + (separateManagementPort ? "0" : ""))
			.run((context) -> {
				WebServer webServer = context
					.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
					.getWebServer();
				assertThat(webServer).isNotNull();
				Integer port = separateManagementPort
						? context.getEnvironment().getProperty("local.management.port", Integer.class)
						: webServer.getPort();
				assertThat(port).isNotNull();
				WebTestClient.bindToServer()
					.baseUrl("http://localhost:" + port)
					.build()
					.get()
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

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void actuatorOnlyApplicationUsesJsonMapper(boolean separateManagementPort, boolean isolatedMapper) {
		getContextRunner(new Class<?>[] { MapperEndpointConfiguration.class }, JacksonEndpointAutoConfiguration.class,
				TomcatServletManagementContextAutoConfiguration.class, ServletManagementContextAutoConfiguration.class)
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("management.server.port=" + (separateManagementPort ? "0" : ""),
					"management.endpoints.jackson.isolated-json-mapper=" + isolatedMapper,
					"spring.jackson.default-property-inclusion=non-null")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(JerseyAutoConfiguration.class);
				assertThat(context).hasBean("jacksonResourceConfigCustomizer");
				WebServer webServer = context
					.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
					.getWebServer();
				assertThat(webServer).isNotNull();
				Integer port = separateManagementPort
						? context.getEnvironment().getProperty("local.management.port", Integer.class)
						: webServer.getPort();
				assertThat(port).isNotNull();
				String json = "{\"first_name\":\"Jersey\"}";
				WebTestClient.bindToServer()
					.baseUrl("http://localhost:" + port)
					.build()
					.get()
					.uri("/actuator/mapper")
					.exchange()
					.expectStatus()
					.isOk()
					.expectBody()
					.json(json, JsonCompareMode.STRICT);
			});
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	@SuppressWarnings("removal")
	void actuatorOnlyApplicationUsesJackson2ByDefault(boolean separateManagementPort) {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(getAutoconfigurations(
					org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class,
					TomcatServletManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class)))
			.withUserConfiguration(MapperEndpointConfiguration.class)
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("management.endpoints.web.exposure.include=*", "server.port=0",
					"management.server.port=" + (separateManagementPort ? "0" : ""),
					"spring.jackson2.default-property-inclusion=non-null")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(JerseyAutoConfiguration.class);
				assertThat(context).hasBean("jackson2ResourceConfigCustomizer");
				WebServer webServer = context
					.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
					.getWebServer();
				assertThat(webServer).isNotNull();
				Integer port = separateManagementPort
						? context.getEnvironment().getProperty("local.management.port", Integer.class)
						: webServer.getPort();
				assertThat(port).isNotNull();
				WebTestClient.bindToServer()
					.baseUrl("http://localhost:" + port)
					.build()
					.get()
					.uri("/actuator/mapper")
					.exchange()
					.expectStatus()
					.isOk()
					.expectBody()
					.json("{\"FirstName\":\"Jersey\"}", JsonCompareMode.STRICT);
			});
	}

	protected void testJerseyEndpoints(Class<?>[] userConfigurations) {
		getContextRunner(userConfigurations).run((context) -> {
			WebServer webServer = context
				.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
				.getWebServer();
			assertThat(webServer).isNotNull();
			int port = webServer.getPort();
			WebTestClient client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.responseTimeout(Duration.ofMinutes(5))
				.build();
			client.get()
				.uri("/actuator")
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("_links.beans")
				.isNotEmpty()
				.jsonPath("_links.restcontroller")
				.doesNotExist()
				.jsonPath("_links.controller")
				.doesNotExist();
		});
	}

	WebApplicationContextRunner getContextRunner(Class<?>[] userConfigurations,
			Class<?>... additionalAutoConfigurations) {
		return new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(getAutoconfigurations(additionalAutoConfigurations)))
			.withUserConfiguration(userConfigurations)
			.withPropertyValues("management.endpoints.web.exposure.include:*", "server.port:0",
					"spring.jersey.preferred-json-mapper=jackson");
	}

	private Class<?>[] getAutoconfigurations(Class<?>... additional) {
		List<Class<?>> autoconfigurations = new ArrayList<>(Arrays.asList(JacksonAutoConfiguration.class,
				JerseyAutoConfiguration.class, JerseyJacksonAutoConfiguration.class, EndpointAutoConfiguration.class,
				TomcatServletWebServerAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				ManagementContextAutoConfiguration.class, BeansEndpointAutoConfiguration.class));
		autoconfigurations.addAll(Arrays.asList(additional));
		return autoconfigurations.toArray(new Class<?>[0]);
	}

	@Configuration(proxyBeanMethods = false)
	static class MapperEndpointConfiguration {

		@Bean
		MapperEndpoint mapperEndpoint() {
			return new MapperEndpoint();
		}

	}

	@org.springframework.boot.actuate.endpoint.annotation.Endpoint(id = "mapper")
	static class MapperEndpoint {

		@org.springframework.boot.actuate.endpoint.annotation.ReadOperation
		MapperResponse response() {
			return new MapperResponse("Jersey", null);
		}

	}

	@tools.jackson.databind.annotation.JsonNaming(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
	@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	record MapperResponse(String firstName, @Nullable String body) implements OperationResponseBody {

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

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfig testResourceConfig() {
			return new ResourceConfig();
		}

	}

	@Configuration
	static class EndpointJsonMapperConfiguration {

		@Bean
		EndpointJsonMapper endpointJsonMapper() {
			SimpleModule module = new SimpleModule();
			module.addSerializer(String.class, new ReverseStringSerializer());
			JsonMapper jsonMapper = JsonMapper.builder().addModule(module).build();
			return () -> jsonMapper;
		}

		static class ReverseStringSerializer extends StdScalarSerializer<Object> {

			ReverseStringSerializer() {
				super(String.class, false);
			}

			@Override
			public boolean isEmpty(SerializationContext provider, Object value) {
				return ((String) value).isEmpty();
			}

			@Override
			public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
				serialize(value, gen);
			}

			@Override
			public final void serializeWithType(Object value, JsonGenerator gen, SerializationContext provider,
					TypeSerializer typeSer) {
				serialize(value, gen);
			}

			private void serialize(Object value, JsonGenerator gen) {
				StringBuilder builder = new StringBuilder((String) value);
				gen.writeString(builder.reverse().toString());
			}

		}

	}

}
