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

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration;
import org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.BeanIds;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CompositeFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link CloudFoundryActuatorAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryActuatorAutoConfigurationTests {

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	private static final String BASE_PATH = "/cloudfoundryapplication";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, WebMvcAutoConfiguration.class,
				JacksonAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				RestTemplateAutoConfiguration.class, ManagementContextAutoConfiguration.class,
				ServletManagementContextAutoConfiguration.class, EndpointAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, CloudFoundryActuatorAutoConfiguration.class));

	@Test
	void cloudFoundryPlatformActive() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				EndpointMapping endpointMapping = (EndpointMapping) ReflectionTestUtils.getField(handlerMapping,
						"endpointMapping");
				assertThat(endpointMapping.getPath()).isEqualTo("/cloudfoundryapplication");
				CorsConfiguration corsConfiguration = (CorsConfiguration) ReflectionTestUtils.getField(handlerMapping,
						"corsConfiguration");
				assertThat(corsConfiguration.getAllowedOrigins()).contains("*");
				assertThat(corsConfiguration.getAllowedMethods())
					.containsAll(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
				assertThat(corsConfiguration.getAllowedHeaders())
					.containsAll(Arrays.asList("Authorization", "X-Cf-App-Instance", "Content-Type"));
			});
	}

	@Test
	void cloudfoundryapplicationProducesActuatorMediaType() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				MockMvcTester mvc = MockMvcTester.from(context);
				assertThat(mvc.get().uri("/cloudfoundryapplication")).hasHeader("Content-Type", V3_JSON);
			});
	}

	@Test
	void cloudFoundryPlatformActiveSetsApplicationId() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				Object interceptor = ReflectionTestUtils.getField(handlerMapping, "securityInterceptor");
				String applicationId = (String) ReflectionTestUtils.getField(interceptor, "applicationId");
				assertThat(applicationId).isEqualTo("my-app-id");
			});
	}

	@Test
	void cloudFoundryPlatformActiveSetsCloudControllerUrl() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				Object interceptor = ReflectionTestUtils.getField(handlerMapping, "securityInterceptor");
				Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
						"cloudFoundrySecurityService");
				String cloudControllerUrl = (String) ReflectionTestUtils.getField(interceptorSecurityService,
						"cloudControllerUrl");
				assertThat(cloudControllerUrl).isEqualTo("https://my-cloud-controller.com");
			});
	}

	@Test
	void skipSslValidation() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com",
					"management.cloudfoundry.skip-ssl-validation:true")
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				Object interceptor = ReflectionTestUtils.getField(handlerMapping, "securityInterceptor");
				Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
						"cloudFoundrySecurityService");
				RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(interceptorSecurityService,
						"restTemplate");
				assertThat(restTemplate.getRequestFactory()).isInstanceOf(SkipSslVerificationHttpRequestFactory.class);
			});
	}

	@Test
	void cloudFoundryPlatformActiveAndCloudControllerUrlNotPresent() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				Object securityInterceptor = ReflectionTestUtils.getField(handlerMapping, "securityInterceptor");
				Object interceptorSecurityService = ReflectionTestUtils.getField(securityInterceptor,
						"cloudFoundrySecurityService");
				assertThat(interceptorSecurityService).isNull();
			});
	}

	@Test
	void cloudFoundryPathsPermittedBySpringSecurity() {
		this.contextRunner.withBean(TestEndpoint.class, TestEndpoint::new)
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
			.run((context) -> {
				SecurityFilterChain chain = getSecurityFilterChain(context);
				MockHttpServletRequest request = new MockHttpServletRequest();
				testCloudFoundrySecurity(request, BASE_PATH, chain);
				testCloudFoundrySecurity(request, BASE_PATH + "/", chain);
				testCloudFoundrySecurity(request, BASE_PATH + "/test", chain);
				testCloudFoundrySecurity(request, BASE_PATH + "/test/a", chain);
				request.setServletPath(BASE_PATH + "/other-path");
				request.setRequestURI(BASE_PATH + "/other-path");
				assertThat(chain.matches(request)).isFalse();
				request.setServletPath("/some-other-path");
				request.setRequestURI("/some-other-path");
				assertThat(chain.matches(request)).isFalse();
			});
	}

	@Test
	void cloudFoundryPathsPermittedWithCsrfBySpringSecurity() {
		this.contextRunner.withBean(TestEndpoint.class, TestEndpoint::new)
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
			.run((context) -> {
				MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
				mvc.perform(post(BASE_PATH + "/test?name=test").contentType(MediaType.APPLICATION_JSON)
					.with(csrf().useInvalidToken())).andExpect(status().isServiceUnavailable());
				// If CSRF fails we'll get a 403, if it works we get service unavailable
				// because of "Cloud controller URL is not available"
			});
	}

	private SecurityFilterChain getSecurityFilterChain(AssertableWebApplicationContext context) {
		Filter springSecurityFilterChain = context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		FilterChainProxy filterChainProxy = getFilterChainProxy(springSecurityFilterChain);
		SecurityFilterChain securityFilterChain = filterChainProxy.getFilterChains().get(0);
		return securityFilterChain;
	}

	private FilterChainProxy getFilterChainProxy(Filter filter) {
		if (filter instanceof FilterChainProxy filterChainProxy) {
			return filterChainProxy;
		}
		if (filter instanceof CompositeFilter) {
			List<?> filters = (List<?>) ReflectionTestUtils.getField(filter, "filters");
			return (FilterChainProxy) filters.stream()
				.filter(FilterChainProxy.class::isInstance)
				.findFirst()
				.orElseThrow();
		}
		throw new IllegalStateException("No FilterChainProxy found");
	}

	private static void testCloudFoundrySecurity(MockHttpServletRequest request, String requestUri,
			SecurityFilterChain chain) {
		request.setRequestURI(requestUri);
		assertThat(chain.matches(request)).isTrue();
	}

	@Test
	void cloudFoundryPlatformInactive() {
		this.contextRunner.withPropertyValues()
			.run((context) -> assertThat(context.containsBean("cloudFoundryWebEndpointServletHandlerMapping"))
				.isFalse());
	}

	@Test
	void cloudFoundryManagementEndpointsDisabled() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION=---", "management.cloudfoundry.enabled:false")
			.run((context) -> assertThat(context.containsBean("cloudFoundryEndpointHandlerMapping")).isFalse());
	}

	@Test
	void allEndpointsAvailableUnderCloudFoundryWithoutExposeAllOnWeb() {
		this.contextRunner.withBean(TestEndpoint.class, TestEndpoint::new)
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				Collection<ExposableWebEndpoint> endpoints = handlerMapping.getEndpoints();
				assertThat(endpoints.stream()
					.filter((candidate) -> EndpointId.of("test").equals(candidate.getEndpointId()))
					.findFirst()).isNotEmpty();
			});
	}

	@Test
	void endpointPathCustomizationIsNotApplied() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com",
					"management.endpoints.web.path-mapping.test=custom")
			.withBean(TestEndpoint.class, TestEndpoint::new)
			.run((context) -> {
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping = getHandlerMapping(context);
				Collection<ExposableWebEndpoint> endpoints = handlerMapping.getEndpoints();
				ExposableWebEndpoint endpoint = endpoints.stream()
					.filter((candidate) -> EndpointId.of("test").equals(candidate.getEndpointId()))
					.findFirst()
					.get();
				Collection<WebOperation> operations = endpoint.getOperations();
				assertThat(operations).hasSize(2);
				assertThat(operations.iterator().next().getRequestPredicate().getPath()).isEqualTo("test");
			});
	}

	@Test
	void healthEndpointInvokerShouldBeCloudFoundryWebExtension() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
					HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class))
			.run((context) -> {
				Collection<ExposableWebEndpoint> endpoints = context
					.getBean("cloudFoundryWebEndpointServletHandlerMapping",
							CloudFoundryWebEndpointServletHandlerMapping.class)
					.getEndpoints();
				ExposableWebEndpoint endpoint = endpoints.iterator().next();
				assertThat(endpoint.getOperations()).hasSize(2);
				WebOperation webOperation = findOperationWithRequestPath(endpoint, "health");
				assertThat(webOperation).extracting("invoker.target")
					.isInstanceOf(CloudFoundryHealthEndpointWebExtension.class);
			});
	}

	private CloudFoundryWebEndpointServletHandlerMapping getHandlerMapping(ApplicationContext context) {
		return context.getBean("cloudFoundryWebEndpointServletHandlerMapping",
				CloudFoundryWebEndpointServletHandlerMapping.class);
	}

	private WebOperation findOperationWithRequestPath(ExposableWebEndpoint endpoint, String requestPath) {
		for (WebOperation operation : endpoint.getOperations()) {
			WebOperationRequestPredicate predicate = operation.getRequestPredicate();
			if (predicate.getPath().equals(requestPath) && predicate.getProduces().contains(V3_JSON)) {
				return operation;
			}
		}
		throw new IllegalStateException(
				"No operation found with request path " + requestPath + " from " + endpoint.getOperations());
	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		String hello() {
			return "hello world";
		}

		@WriteOperation
		void update(String name) {
		}

	}

}
