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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.junit.After;
import org.junit.Test;
import reactor.netty.http.HttpResources;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveCloudFoundryActuatorAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ReactiveCloudFoundryActuatorAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ReactiveSecurityAutoConfiguration.class,
					ReactiveUserDetailsServiceAutoConfiguration.class,
					WebFluxAutoConfiguration.class, JacksonAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class,
					WebClientCustomizerConfig.class, WebClientAutoConfiguration.class,
					ManagementContextAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					HealthIndicatorAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class,
					ReactiveCloudFoundryActuatorAutoConfiguration.class));

	@After
	public void close() {
		HttpResources.reset();
	}

	@Test
	public void cloudFoundryPlatformActive() {
		this.contextRunner
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					EndpointMapping endpointMapping = (EndpointMapping) ReflectionTestUtils
							.getField(handlerMapping, "endpointMapping");
					assertThat(endpointMapping.getPath())
							.isEqualTo("/cloudfoundryapplication");
					CorsConfiguration corsConfiguration = (CorsConfiguration) ReflectionTestUtils
							.getField(handlerMapping, "corsConfiguration");
					assertThat(corsConfiguration.getAllowedOrigins()).contains("*");
					assertThat(corsConfiguration.getAllowedMethods()).containsAll(
							Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
					assertThat(corsConfiguration.getAllowedHeaders())
							.containsAll(Arrays.asList("Authorization",
									"X-Cf-App-Instance", "Content-Type"));
				});
	}

	@Test
	public void cloudfoundryapplicationProducesActuatorMediaType() {
		this.contextRunner
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					WebTestClient webTestClient = WebTestClient
							.bindToApplicationContext(context).build();
					webTestClient.get().uri("/cloudfoundryapplication").header(
							"Content-Type", ActuatorMediaType.V2_JSON + ";charset=UTF-8");
				});
	}

	@Test
	public void cloudFoundryPlatformActiveSetsApplicationId() {
		this.contextRunner
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					Object interceptor = ReflectionTestUtils.getField(handlerMapping,
							"securityInterceptor");
					String applicationId = (String) ReflectionTestUtils
							.getField(interceptor, "applicationId");
					assertThat(applicationId).isEqualTo("my-app-id");
				});
	}

	@Test
	public void cloudFoundryPlatformActiveSetsCloudControllerUrl() {
		this.contextRunner
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					Object interceptor = ReflectionTestUtils.getField(handlerMapping,
							"securityInterceptor");
					Object interceptorSecurityService = ReflectionTestUtils
							.getField(interceptor, "cloudFoundrySecurityService");
					String cloudControllerUrl = (String) ReflectionTestUtils
							.getField(interceptorSecurityService, "cloudControllerUrl");
					assertThat(cloudControllerUrl)
							.isEqualTo("http://my-cloud-controller.com");
				});
	}

	@Test
	public void cloudFoundryPlatformActiveAndCloudControllerUrlNotPresent() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---",
				"vcap.application.application_id:my-app-id").run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = context
							.getBean("cloudFoundryWebFluxEndpointHandlerMapping",
									CloudFoundryWebFluxEndpointHandlerMapping.class);
					Object securityInterceptor = ReflectionTestUtils
							.getField(handlerMapping, "securityInterceptor");
					Object interceptorSecurityService = ReflectionTestUtils
							.getField(securityInterceptor, "cloudFoundrySecurityService");
					assertThat(interceptorSecurityService).isNull();
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cloudFoundryPathsIgnoredBySpringSecurity() {
		this.contextRunner
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					WebFilterChainProxy chainProxy = context
							.getBean(WebFilterChainProxy.class);
					List<SecurityWebFilterChain> filters = (List<SecurityWebFilterChain>) ReflectionTestUtils
							.getField(chainProxy, "filters");
					Boolean cfRequestMatches = filters.get(0)
							.matches(MockServerWebExchange.from(MockServerHttpRequest
									.get("/cloudfoundryapplication/my-path").build()))
							.block(Duration.ofSeconds(30));
					Boolean otherRequestMatches = filters.get(0)
							.matches(MockServerWebExchange.from(MockServerHttpRequest
									.get("/some-other-path").build()))
							.block(Duration.ofSeconds(30));
					assertThat(cfRequestMatches).isTrue();
					assertThat(otherRequestMatches).isFalse();
					otherRequestMatches = filters.get(1)
							.matches(MockServerWebExchange.from(MockServerHttpRequest
									.get("/some-other-path").build()))
							.block(Duration.ofSeconds(30));
					assertThat(otherRequestMatches).isTrue();
				});

	}

	@Test
	public void cloudFoundryPlatformInactive() {
		this.contextRunner.run((context) -> assertThat(
				context.containsBean("cloudFoundryWebFluxEndpointHandlerMapping"))
						.isFalse());
	}

	@Test
	public void cloudFoundryManagementEndpointsDisabled() {
		this.contextRunner
				.withPropertyValues("VCAP_APPLICATION=---",
						"management.cloudfoundry.enabled:false")
				.run((context) -> assertThat(
						context.containsBean("cloudFoundryWebFluxEndpointHandlerMapping"))
								.isFalse());
	}

	@Test
	public void allEndpointsAvailableUnderCloudFoundryWithoutEnablingWebIncludes() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					Collection<ExposableWebEndpoint> endpoints = handlerMapping
							.getEndpoints();
					List<EndpointId> endpointIds = endpoints.stream()
							.map(ExposableEndpoint::getEndpointId)
							.collect(Collectors.toList());
					assertThat(endpointIds).contains(EndpointId.of("test"));
				});
	}

	@Test
	public void endpointPathCustomizationIsNotApplied() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					Collection<ExposableWebEndpoint> endpoints = handlerMapping
							.getEndpoints();
					ExposableWebEndpoint endpoint = endpoints.stream()
							.filter((candidate) -> EndpointId.of("test")
									.equals(candidate.getEndpointId()))
							.findFirst().get();
					assertThat(endpoint.getOperations()).hasSize(1);
					WebOperation operation = endpoint.getOperations().iterator().next();
					assertThat(operation.getRequestPredicate().getPath())
							.isEqualTo("test");
				});
	}

	@Test
	public void healthEndpointInvokerShouldBeCloudFoundryWebExtension() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(HealthEndpointAutoConfiguration.class))
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					Collection<ExposableWebEndpoint> endpoints = getHandlerMapping(
							context).getEndpoints();
					ExposableWebEndpoint endpoint = endpoints.iterator().next();
					assertThat(endpoint.getOperations()).hasSize(3);
					WebOperation webOperation = findOperationWithRequestPath(endpoint,
							"health");
					Object invoker = ReflectionTestUtils.getField(webOperation,
							"invoker");
					assertThat(ReflectionTestUtils.getField(invoker, "target"))
							.isInstanceOf(
									CloudFoundryReactiveHealthEndpointWebExtension.class);
				});
	}

	@Test
	public void skipSslValidation() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(HealthEndpointAutoConfiguration.class))
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com",
						"management.cloudfoundry.skip-ssl-validation:true")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					Object interceptor = ReflectionTestUtils.getField(handlerMapping,
							"securityInterceptor");
					Object interceptorSecurityService = ReflectionTestUtils
							.getField(interceptor, "cloudFoundrySecurityService");
					WebClient webClient = (WebClient) ReflectionTestUtils
							.getField(interceptorSecurityService, "webClient");
					webClient.get().uri("https://self-signed.badssl.com/").exchange()
							.block(Duration.ofSeconds(30));
				});
	}

	@Test
	public void sslValidationNotSkippedByDefault() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(HealthEndpointAutoConfiguration.class))
				.withPropertyValues("VCAP_APPLICATION:---",
						"vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.run((context) -> {
					CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(
							context);
					Object interceptor = ReflectionTestUtils.getField(handlerMapping,
							"securityInterceptor");
					Object interceptorSecurityService = ReflectionTestUtils
							.getField(interceptor, "cloudFoundrySecurityService");
					WebClient webClient = (WebClient) ReflectionTestUtils
							.getField(interceptorSecurityService, "webClient");
					assertThatExceptionOfType(RuntimeException.class)
							.isThrownBy(() -> webClient.get()
									.uri("https://self-signed.badssl.com/").exchange()
									.block(Duration.ofSeconds(30)))
							.withCauseInstanceOf(SSLException.class);
				});
	}

	private CloudFoundryWebFluxEndpointHandlerMapping getHandlerMapping(
			ApplicationContext context) {
		return context.getBean("cloudFoundryWebFluxEndpointHandlerMapping",
				CloudFoundryWebFluxEndpointHandlerMapping.class);
	}

	private WebOperation findOperationWithRequestPath(ExposableWebEndpoint endpoint,
			String requestPath) {
		for (WebOperation operation : endpoint.getOperations()) {
			if (operation.getRequestPredicate().getPath().equals(requestPath)) {
				return operation;
			}
		}
		throw new IllegalStateException("No operation found with request path "
				+ requestPath + " from " + endpoint.getOperations());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		public String hello() {
			return "hello world";
		}

	}

	@Configuration
	static class WebClientCustomizerConfig {

		@Bean
		public WebClientCustomizer webClientCustomizer() {
			return mock(WebClientCustomizer.class);
		}

	}

}
