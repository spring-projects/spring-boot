/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for web endpoints exposed using Spring MVC on CloudFoundry.
 *
 * @author Madhura Bhave
 */
class CloudFoundryMvcWebEndpointIntegrationTests {

	private static TokenValidator tokenValidator = mock(TokenValidator.class);

	private static CloudFoundrySecurityService securityService = mock(CloudFoundrySecurityService.class);

	@Test
	void operationWithSecurityInterceptorForbidden() {
		given(securityService.getAccessLevel(any(), eq("app-id"))).willReturn(AccessLevel.RESTRICTED);
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication/test").accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange().expectStatus()
						.isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void operationWithSecurityInterceptorSuccess() {
		given(securityService.getAccessLevel(any(), eq("app-id"))).willReturn(AccessLevel.FULL);
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication/test").accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange().expectStatus()
						.isEqualTo(HttpStatus.OK));
	}

	@Test
	void responseToOptionsRequestIncludesCorsHeaders() {
		load(TestEndpointConfiguration.class,
				(client) -> client.options().uri("/cfApplication/test").accept(MediaType.APPLICATION_JSON)
						.header("Access-Control-Request-Method", "POST").header("Origin", "https://example.com")
						.exchange().expectStatus().isOk().expectHeader()
						.valueEquals("Access-Control-Allow-Origin", "https://example.com").expectHeader()
						.valueEquals("Access-Control-Allow-Methods", "GET,POST"));
	}

	@Test
	void linksToOtherEndpointsWithFullAccess() {
		given(securityService.getAccessLevel(any(), eq("app-id"))).willReturn(AccessLevel.FULL);
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication").accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange().expectStatus().isOk()
						.expectBody().jsonPath("_links.length()").isEqualTo(5).jsonPath("_links.self.href").isNotEmpty()
						.jsonPath("_links.self.templated").isEqualTo(false).jsonPath("_links.info.href").isNotEmpty()
						.jsonPath("_links.info.templated").isEqualTo(false).jsonPath("_links.env.href").isNotEmpty()
						.jsonPath("_links.env.templated").isEqualTo(false).jsonPath("_links.test.href").isNotEmpty()
						.jsonPath("_links.test.templated").isEqualTo(false).jsonPath("_links.test-part.href")
						.isNotEmpty().jsonPath("_links.test-part.templated").isEqualTo(true));
	}

	@Test
	void linksToOtherEndpointsForbidden() {
		CloudFoundryAuthorizationException exception = new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN,
				"invalid-token");
		willThrow(exception).given(tokenValidator).validate(any());
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication").accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange().expectStatus()
						.isUnauthorized());
	}

	@Test
	void linksToOtherEndpointsWithRestrictedAccess() {
		given(securityService.getAccessLevel(any(), eq("app-id"))).willReturn(AccessLevel.RESTRICTED);
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication").accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange().expectStatus().isOk()
						.expectBody().jsonPath("_links.length()").isEqualTo(2).jsonPath("_links.self.href").isNotEmpty()
						.jsonPath("_links.self.templated").isEqualTo(false).jsonPath("_links.info.href").isNotEmpty()
						.jsonPath("_links.info.templated").isEqualTo(false).jsonPath("_links.env").doesNotExist()
						.jsonPath("_links.test").doesNotExist().jsonPath("_links.test-part").doesNotExist());
	}

	private AnnotationConfigServletWebServerApplicationContext createApplicationContext(Class<?>... config) {
		return new AnnotationConfigServletWebServerApplicationContext(config);
	}

	private int getPort(AnnotationConfigServletWebServerApplicationContext context) {
		return context.getWebServer().getPort();
	}

	private void load(Class<?> configuration, Consumer<WebTestClient> clientConsumer) {
		BiConsumer<ApplicationContext, WebTestClient> consumer = (context, client) -> clientConsumer.accept(client);
		try (AnnotationConfigServletWebServerApplicationContext context = createApplicationContext(configuration,
				CloudFoundryMvcConfiguration.class)) {
			consumer.accept(context, WebTestClient.bindToServer().baseUrl("http://localhost:" + getPort(context))
					.responseTimeout(Duration.ofMinutes(5)).build());
		}
	}

	private String mockAccessToken() {
		return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ."
				+ Base64Utils.encodeToString("signature".getBytes());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class CloudFoundryMvcConfiguration {

		@Bean
		CloudFoundrySecurityInterceptor interceptor() {
			return new CloudFoundrySecurityInterceptor(tokenValidator, securityService, "app-id");
		}

		@Bean
		EndpointMediaTypes EndpointMediaTypes() {
			return new EndpointMediaTypes(Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
		}

		@Bean
		CloudFoundryWebEndpointServletHandlerMapping cloudFoundryWebEndpointServletHandlerMapping(
				WebEndpointDiscoverer webEndpointDiscoverer, EndpointMediaTypes endpointMediaTypes,
				CloudFoundrySecurityInterceptor interceptor) {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.setAllowedOrigins(Arrays.asList("https://example.com"));
			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
			Collection<ExposableWebEndpoint> webEndpoints = webEndpointDiscoverer.getEndpoints();
			List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>(webEndpoints);
			return new CloudFoundryWebEndpointServletHandlerMapping(new EndpointMapping("/cfApplication"), webEndpoints,
					endpointMediaTypes, corsConfiguration, interceptor, allEndpoints);
		}

		@Bean
		WebEndpointDiscoverer webEndpointDiscoverer(ApplicationContext applicationContext,
				EndpointMediaTypes endpointMediaTypes) {
			ParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
					DefaultConversionService.getSharedInstance());
			return new WebEndpointDiscoverer(applicationContext, parameterMapper, endpointMediaTypes, null,
					Collections.emptyList(), Collections.emptyList());
		}

		@Bean
		EndpointDelegate endpointDelegate() {
			return mock(EndpointDelegate.class);
		}

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		private final EndpointDelegate endpointDelegate;

		TestEndpoint(EndpointDelegate endpointDelegate) {
			this.endpointDelegate = endpointDelegate;
		}

		@ReadOperation
		Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

		@ReadOperation
		Map<String, Object> readPart(@Selector String part) {
			return Collections.singletonMap("part", part);
		}

		@WriteOperation
		void write(String foo, String bar) {
			this.endpointDelegate.write(foo, bar);
		}

	}

	@Endpoint(id = "env")
	static class TestEnvEndpoint {

		@ReadOperation
		Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

	}

	@Endpoint(id = "info")
	static class TestInfoEndpoint {

		@ReadOperation
		Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(CloudFoundryMvcConfiguration.class)
	static class TestEndpointConfiguration {

		@Bean
		TestEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new TestEndpoint(endpointDelegate);
		}

		@Bean
		TestInfoEndpoint testInfoEnvEndpoint() {
			return new TestInfoEndpoint();
		}

		@Bean
		TestEnvEndpoint testEnvEndpoint() {
			return new TestEnvEndpoint();
		}

	}

	interface EndpointDelegate {

		void write();

		void write(String foo, String bar);

	}

}
