/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CloudFoundryWebFluxEndpointHandlerMapping}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryWebFluxEndpointIntegrationTests {

	private static ReactiveTokenValidator tokenValidator = mock(
			ReactiveTokenValidator.class);

	private static ReactiveCloudFoundrySecurityService securityService = mock(
			ReactiveCloudFoundrySecurityService.class);

	@Test
	public void operationWithSecurityInterceptorForbidden() {
		given(tokenValidator.validate(any())).willReturn(Mono.empty());
		given(securityService.getAccessLevel(any(), eq("app-id")))
				.willReturn(Mono.just(AccessLevel.RESTRICTED));
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication/test")
						.accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange()
						.expectStatus().isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	public void operationWithSecurityInterceptorSuccess() {
		given(tokenValidator.validate(any())).willReturn(Mono.empty());
		given(securityService.getAccessLevel(any(), eq("app-id")))
				.willReturn(Mono.just(AccessLevel.FULL));
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication/test")
						.accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange()
						.expectStatus().isEqualTo(HttpStatus.OK));
	}

	@Test
	public void responseToOptionsRequestIncludesCorsHeaders() {
		load(TestEndpointConfiguration.class, (client) -> client.options()
				.uri("/cfApplication/test").accept(MediaType.APPLICATION_JSON)
				.header("Access-Control-Request-Method", "POST")
				.header("Origin", "http://example.com").exchange().expectStatus().isOk()
				.expectHeader()
				.valueEquals("Access-Control-Allow-Origin", "http://example.com")
				.expectHeader().valueEquals("Access-Control-Allow-Methods", "GET,POST"));
	}

	@Test
	public void linksToOtherEndpointsWithFullAccess() {
		given(tokenValidator.validate(any())).willReturn(Mono.empty());
		given(securityService.getAccessLevel(any(), eq("app-id")))
				.willReturn(Mono.just(AccessLevel.FULL));
		load(TestEndpointConfiguration.class, (client) -> client.get()
				.uri("/cfApplication").accept(MediaType.APPLICATION_JSON)
				.header("Authorization", "bearer " + mockAccessToken()).exchange()
				.expectStatus().isOk().expectBody().jsonPath("_links.length()")
				.isEqualTo(5).jsonPath("_links.self.href").isNotEmpty()
				.jsonPath("_links.self.templated").isEqualTo(false)
				.jsonPath("_links.info.href").isNotEmpty()
				.jsonPath("_links.info.templated").isEqualTo(false)
				.jsonPath("_links.env.href").isNotEmpty().jsonPath("_links.env.templated")
				.isEqualTo(false).jsonPath("_links.test.href").isNotEmpty()
				.jsonPath("_links.test.templated").isEqualTo(false)
				.jsonPath("_links.test-part.href").isNotEmpty()
				.jsonPath("_links.test-part.templated").isEqualTo(true));
	}

	@Test
	public void linksToOtherEndpointsForbidden() {
		CloudFoundryAuthorizationException exception = new CloudFoundryAuthorizationException(
				Reason.INVALID_TOKEN, "invalid-token");
		willThrow(exception).given(tokenValidator).validate(any());
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication")
						.accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange()
						.expectStatus().isUnauthorized());
	}

	@Test
	public void linksToOtherEndpointsWithRestrictedAccess() {
		given(tokenValidator.validate(any())).willReturn(Mono.empty());
		given(securityService.getAccessLevel(any(), eq("app-id")))
				.willReturn(Mono.just(AccessLevel.RESTRICTED));
		load(TestEndpointConfiguration.class,
				(client) -> client.get().uri("/cfApplication")
						.accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "bearer " + mockAccessToken()).exchange()
						.expectStatus().isOk().expectBody().jsonPath("_links.length()")
						.isEqualTo(2).jsonPath("_links.self.href").isNotEmpty()
						.jsonPath("_links.self.templated").isEqualTo(false)
						.jsonPath("_links.info.href").isNotEmpty()
						.jsonPath("_links.info.templated").isEqualTo(false)
						.jsonPath("_links.env").doesNotExist().jsonPath("_links.test")
						.doesNotExist().jsonPath("_links.test-part").doesNotExist());
	}

	private AnnotationConfigReactiveWebServerApplicationContext createApplicationContext(
			Class<?>... config) {
		AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
		context.register(config);
		return context;
	}

	private void load(Class<?> configuration, Consumer<WebTestClient> clientConsumer) {
		BiConsumer<ApplicationContext, WebTestClient> consumer = (context,
				client) -> clientConsumer.accept(client);
		AnnotationConfigReactiveWebServerApplicationContext context = createApplicationContext(
				configuration, CloudFoundryReactiveConfiguration.class);
		context.refresh();
		try {
			consumer.accept(context, WebTestClient.bindToServer()
					.baseUrl("http://localhost:" + getPort(context)).build());
		}
		finally {
			context.close();
		}
	}

	protected int getPort(AnnotationConfigReactiveWebServerApplicationContext context) {
		return context.getBean(CloudFoundryReactiveConfiguration.class).port;
	}

	private String mockAccessToken() {
		return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0b3B0YWwu"
				+ "Y29tIiwiZXhwIjoxNDI2NDIwODAwLCJhd2Vzb21lIjp0cnVlfQ."
				+ Base64Utils.encodeToString("signature".getBytes());
	}

	@Configuration
	@EnableWebFlux
	static class CloudFoundryReactiveConfiguration {

		private int port;

		@Bean
		public CloudFoundrySecurityInterceptor interceptor() {
			return new CloudFoundrySecurityInterceptor(tokenValidator, securityService,
					"app-id");
		}

		@Bean
		public EndpointMediaTypes EndpointMediaTypes() {
			return new EndpointMediaTypes(Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
		}

		@Bean
		public CloudFoundryWebFluxEndpointHandlerMapping cloudFoundryWebEndpointServletHandlerMapping(
				WebEndpointDiscoverer webEndpointDiscoverer,
				EndpointMediaTypes endpointMediaTypes,
				CloudFoundrySecurityInterceptor interceptor) {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.setAllowedOrigins(Arrays.asList("http://example.com"));
			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
			return new CloudFoundryWebFluxEndpointHandlerMapping(
					new EndpointMapping("/cfApplication"),
					webEndpointDiscoverer.getEndpoints(), endpointMediaTypes,
					corsConfiguration, interceptor,
					new EndpointLinksResolver(webEndpointDiscoverer.getEndpoints()));
		}

		@Bean
		public WebEndpointDiscoverer webEndpointDiscoverer(
				ApplicationContext applicationContext,
				EndpointMediaTypes endpointMediaTypes) {
			ParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
					DefaultConversionService.getSharedInstance());
			return new WebEndpointDiscoverer(applicationContext, parameterMapper,
					endpointMediaTypes, PathMapper.useEndpointId(),
					Collections.emptyList(), Collections.emptyList());
		}

		@Bean
		public EndpointDelegate endpointDelegate() {
			return mock(EndpointDelegate.class);
		}

		@Bean
		public NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		public HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		public ApplicationListener<ReactiveWebServerInitializedEvent> serverInitializedListener() {
			return (event) -> this.port = event.getWebServer().getPort();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		private final EndpointDelegate endpointDelegate;

		TestEndpoint(EndpointDelegate endpointDelegate) {
			this.endpointDelegate = endpointDelegate;
		}

		@ReadOperation
		public Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

		@ReadOperation
		public Map<String, Object> readPart(@Selector String part) {
			return Collections.singletonMap("part", part);
		}

		@WriteOperation
		public void write(String foo, String bar) {
			this.endpointDelegate.write(foo, bar);
		}

	}

	@Endpoint(id = "env")
	static class TestEnvEndpoint {

		@ReadOperation
		public Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

	}

	@Endpoint(id = "info")
	static class TestInfoEndpoint {

		@ReadOperation
		public Map<String, Object> readAll() {
			return Collections.singletonMap("All", true);
		}

	}

	@Configuration
	@Import(CloudFoundryReactiveConfiguration.class)
	protected static class TestEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new TestEndpoint(endpointDelegate);
		}

		@Bean
		public TestInfoEndpoint testInfoEnvEndpoint() {
			return new TestInfoEndpoint();
		}

		@Bean
		public TestEnvEndpoint testEnvEndpoint() {
			return new TestEnvEndpoint();
		}

	}

	public interface EndpointDelegate {

		void write();

		void write(String foo, String bar);

	}

}
