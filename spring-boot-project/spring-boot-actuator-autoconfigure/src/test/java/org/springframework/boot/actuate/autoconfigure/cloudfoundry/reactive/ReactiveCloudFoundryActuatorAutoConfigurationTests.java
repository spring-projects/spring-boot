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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.ipc.netty.http.HttpResources;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
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
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveCloudFoundryActuatorAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ReactiveCloudFoundryActuatorAutoConfigurationTests {

	private AnnotationConfigReactiveWebApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
	}

	@After
	public void close() {
		HttpResources.reset();
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void cloudFoundryPlatformActive() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		EndpointMapping endpointMapping = (EndpointMapping) ReflectionTestUtils
				.getField(handlerMapping, "endpointMapping");
		assertThat(endpointMapping.getPath()).isEqualTo("/cloudfoundryapplication");
		CorsConfiguration corsConfiguration = (CorsConfiguration) ReflectionTestUtils
				.getField(handlerMapping, "corsConfiguration");
		assertThat(corsConfiguration.getAllowedOrigins()).contains("*");
		assertThat(corsConfiguration.getAllowedMethods()).containsAll(
				Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
		assertThat(corsConfiguration.getAllowedHeaders()).containsAll(
				Arrays.asList("Authorization", "X-Cf-App-Instance", "Content-Type"));
	}

	@Test
	public void cloudfoundryapplicationProducesActuatorMediaType() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		WebTestClient webTestClient = WebTestClient.bindToApplicationContext(this.context)
				.build();
		webTestClient.get().uri("/cloudfoundryapplication").header("Content-Type",
				ActuatorMediaType.V2_JSON + ";charset=UTF-8");
	}

	@Test
	public void cloudFoundryPlatformActiveSetsApplicationId() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		Object interceptor = ReflectionTestUtils.getField(handlerMapping,
				"securityInterceptor");
		String applicationId = (String) ReflectionTestUtils.getField(interceptor,
				"applicationId");
		assertThat(applicationId).isEqualTo("my-app-id");
	}

	@Test
	public void cloudFoundryPlatformActiveSetsCloudControllerUrl() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		Object interceptor = ReflectionTestUtils.getField(handlerMapping,
				"securityInterceptor");
		Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
				"cloudFoundrySecurityService");
		String cloudControllerUrl = (String) ReflectionTestUtils
				.getField(interceptorSecurityService, "cloudControllerUrl");
		assertThat(cloudControllerUrl).isEqualTo("http://my-cloud-controller.com");
	}

	@Test
	public void cloudFoundryPlatformActiveAndCloudControllerUrlNotPresent() {
		TestPropertyValues
				.of("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
				.applyTo(this.context);
		setupContext();
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = this.context.getBean(
				"cloudFoundryWebFluxEndpointHandlerMapping",
				CloudFoundryWebFluxEndpointHandlerMapping.class);
		Object securityInterceptor = ReflectionTestUtils.getField(handlerMapping,
				"securityInterceptor");
		Object interceptorSecurityService = ReflectionTestUtils
				.getField(securityInterceptor, "cloudFoundrySecurityService");
		assertThat(interceptorSecurityService).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cloudFoundryPathsIgnoredBySpringSecurity() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		WebFilterChainProxy chainProxy = this.context.getBean(WebFilterChainProxy.class);
		List<SecurityWebFilterChain> filters = (List<SecurityWebFilterChain>) ReflectionTestUtils
				.getField(chainProxy, "filters");
		Boolean cfRequestMatches = filters.get(0).matches(MockServerWebExchange.from(
				MockServerHttpRequest.get("/cloudfoundryapplication/my-path").build()))
				.block();
		Boolean otherRequestMatches = filters.get(0)
				.matches(MockServerWebExchange
						.from(MockServerHttpRequest.get("/some-other-path").build()))
				.block();
		assertThat(cfRequestMatches).isTrue();
		assertThat(otherRequestMatches).isFalse();
		otherRequestMatches = filters.get(1)
				.matches(MockServerWebExchange
						.from(MockServerHttpRequest.get("/some-other-path").build()))
				.block();
		assertThat(otherRequestMatches).isTrue();
	}

	@Test
	public void cloudFoundryPlatformInactive() {
		setupContext();
		this.context.refresh();
		assertThat(this.context.containsBean("cloudFoundryWebFluxEndpointHandlerMapping"))
				.isFalse();
	}

	@Test
	public void cloudFoundryManagementEndpointsDisabled() {
		setupContextWithCloudEnabled();
		TestPropertyValues
				.of("VCAP_APPLICATION=---", "management.cloudfoundry.enabled:false")
				.applyTo(this.context);
		this.context.refresh();
		assertThat(this.context.containsBean("cloudFoundryWebFluxEndpointHandlerMapping"))
				.isFalse();
	}

	@Test
	public void allEndpointsAvailableUnderCloudFoundryWithoutEnablingWebIncludes() {
		setupContextWithCloudEnabled();
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		Collection<ExposableWebEndpoint> endpoints = handlerMapping.getEndpoints();
		List<String> endpointIds = endpoints.stream().map(ExposableEndpoint::getId)
				.collect(Collectors.toList());
		assertThat(endpointIds).contains("test");
	}

	@Test
	public void endpointPathCustomizationIsNotApplied() {
		setupContextWithCloudEnabled();
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		Collection<ExposableWebEndpoint> endpoints = handlerMapping.getEndpoints();
		ExposableWebEndpoint endpoint = endpoints.stream()
				.filter((candidate) -> "test".equals(candidate.getId())).findFirst()
				.get();
		assertThat(endpoint.getOperations()).hasSize(1);
		WebOperation operation = endpoint.getOperations().iterator().next();
		assertThat(operation.getRequestPredicate().getPath()).isEqualTo("test");
	}

	@Test
	public void healthEndpointInvokerShouldBeCloudFoundryWebExtension() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		Collection<ExposableWebEndpoint> endpoints = getHandlerMapping().getEndpoints();
		ExposableWebEndpoint endpoint = endpoints.iterator().next();
		WebOperation webOperation = endpoint.getOperations().iterator().next();
		Object invoker = ReflectionTestUtils.getField(webOperation, "invoker");
		assertThat(ReflectionTestUtils.getField(invoker, "target"))
				.isInstanceOf(CloudFoundryReactiveHealthEndpointWebExtension.class);
	}

	@Test
	public void skipSslValidation() {
		setupContextWithCloudEnabled();
		TestPropertyValues.of("management.cloudfoundry.skip-ssl-validation:true")
				.applyTo(this.context);
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		Object interceptor = ReflectionTestUtils.getField(handlerMapping,
				"securityInterceptor");
		Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
				"cloudFoundrySecurityService");
		WebClient webClient = (WebClient) ReflectionTestUtils
				.getField(interceptorSecurityService, "webClient");
		webClient.get().uri("https://self-signed.badssl.com/").exchange().block();
	}

	@Test
	public void sslValidationNotSkippedByDefault() {
		setupContextWithCloudEnabled();
		this.context.refresh();
		CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping();
		Object interceptor = ReflectionTestUtils.getField(handlerMapping,
				"securityInterceptor");
		Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
				"cloudFoundrySecurityService");
		WebClient webClient = (WebClient) ReflectionTestUtils
				.getField(interceptorSecurityService, "webClient");
		this.thrown.expectCause(instanceOf(SSLException.class));
		webClient.get().uri("https://self-signed.badssl.com/").exchange().block();
	}

	private void setupContextWithCloudEnabled() {
		TestPropertyValues
				.of("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:http://my-cloud-controller.com")
				.applyTo(this.context);
		setupContext();
	}

	private void setupContext() {
		this.context.register(ReactiveSecurityAutoConfiguration.class,
				ReactiveUserDetailsServiceAutoConfiguration.class,
				WebFluxAutoConfiguration.class, JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				WebClientCustomizerConfig.class, WebClientAutoConfiguration.class,
				ManagementContextAutoConfiguration.class, EndpointAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				ReactiveCloudFoundryActuatorAutoConfiguration.class);
	}

	private CloudFoundryWebFluxEndpointHandlerMapping getHandlerMapping() {
		return this.context.getBean("cloudFoundryWebFluxEndpointHandlerMapping",
				CloudFoundryWebFluxEndpointHandlerMapping.class);
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
