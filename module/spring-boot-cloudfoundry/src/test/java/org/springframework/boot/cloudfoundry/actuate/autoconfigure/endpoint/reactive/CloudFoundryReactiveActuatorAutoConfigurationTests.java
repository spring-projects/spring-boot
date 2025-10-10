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

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.reactive;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.netty.http.HttpResources;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.servlet.CloudFoundryInfoEndpointWebExtension;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CloudFoundryReactiveActuatorAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Moritz Halbritter
 */
class CloudFoundryReactiveActuatorAutoConfigurationTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveSecurityAutoConfiguration.class,
				WebFluxAutoConfiguration.class, JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				WebClientCustomizerConfig.class, WebClientAutoConfiguration.class,
				ManagementContextAutoConfiguration.class, EndpointAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				InfoContributorAutoConfiguration.class, InfoEndpointAutoConfiguration.class,
				ProjectInfoAutoConfiguration.class, CloudFoundryReactiveActuatorAutoConfiguration.class))
		.withUserConfiguration(UserDetailsServiceConfiguration.class);

	private static final String BASE_PATH = "/cloudfoundryapplication";

	@AfterEach
	void close() {
		HttpResources.reset();
	}

	@Test
	void cloudFoundryPlatformActive() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(context);
				assertThat(handlerMapping).extracting("endpointMapping.path").isEqualTo("/cloudfoundryapplication");
				assertThat(handlerMapping)
					.extracting("corsConfiguration", InstanceOfAssertFactories.type(CorsConfiguration.class))
					.satisfies((corsConfiguration) -> {
						assertThat(corsConfiguration.getAllowedOrigins()).contains("*");
						assertThat(corsConfiguration.getAllowedMethods())
							.containsAll(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
						assertThat(corsConfiguration.getAllowedHeaders())
							.containsAll(Arrays.asList("Authorization", "X-Cf-App-Instance", "Content-Type"));
					});
			});
	}

	@Test
	void cloudfoundryapplicationProducesActuatorMediaType() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				WebTestClient webTestClient = WebTestClient.bindToApplicationContext(context).build();
				webTestClient.get().uri("/cloudfoundryapplication").header("Content-Type", V2_JSON + ";charset=UTF-8");
			});
	}

	@Test
	void cloudFoundryPlatformActiveSetsApplicationId() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> assertThat(getHandlerMapping(context)).extracting("securityInterceptor.applicationId")
				.isEqualTo("my-app-id"));
	}

	@Test
	void cloudFoundryPlatformActiveSetsCloudControllerUrl() {
		this.contextRunner
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> assertThat(getHandlerMapping(context))
				.extracting("securityInterceptor.cloudFoundrySecurityService.cloudControllerUrl")
				.isEqualTo("https://my-cloud-controller.com"));
	}

	@Test
	void cloudFoundryPlatformActiveAndCloudControllerUrlNotPresent() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
			.run((context) -> assertThat(context.getBean("cloudFoundryWebFluxEndpointHandlerMapping",
					CloudFoundryWebFluxEndpointHandlerMapping.class))
				.extracting("securityInterceptor.cloudFoundrySecurityService")
				.isNull());
	}

	@Test
	@SuppressWarnings("unchecked")
	void cloudFoundryPathsIgnoredBySpringSecurity() {
		this.contextRunner.withBean(TestEndpoint.class, TestEndpoint::new)
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				assertThat(context.getBean(WebFilterChainProxy.class))
					.extracting("filters", InstanceOfAssertFactories.list(SecurityWebFilterChain.class))
					.satisfies((filters) -> {
						Boolean cfBaseRequestMatches = getMatches(filters, BASE_PATH);
						Boolean cfBaseWithTrailingSlashRequestMatches = getMatches(filters, BASE_PATH + "/");
						Boolean cfRequestMatches = getMatches(filters, BASE_PATH + "/test");
						Boolean cfRequestWithAdditionalPathMatches = getMatches(filters, BASE_PATH + "/test/a");
						Boolean otherCfRequestMatches = getMatches(filters, BASE_PATH + "/other-path");
						Boolean otherRequestMatches = getMatches(filters, "/some-other-path");
						assertThat(cfBaseRequestMatches).isTrue();
						assertThat(cfBaseWithTrailingSlashRequestMatches).isTrue();
						assertThat(cfRequestMatches).isTrue();
						assertThat(cfRequestWithAdditionalPathMatches).isTrue();
						assertThat(otherCfRequestMatches).isFalse();
						assertThat(otherRequestMatches).isFalse();
						otherRequestMatches = filters.get(1)
							.matches(MockServerWebExchange.from(MockServerHttpRequest.get("/some-other-path").build()))
							.block(Duration.ofSeconds(30));
						assertThat(otherRequestMatches).isTrue();
					});
			});
	}

	private static @Nullable Boolean getMatches(List<? extends SecurityWebFilterChain> filters, String urlTemplate) {
		return filters.get(0)
			.matches(MockServerWebExchange.from(MockServerHttpRequest.get(urlTemplate).build()))
			.block(Duration.ofSeconds(30));
	}

	@Test
	void cloudFoundryPlatformInactive() {
		this.contextRunner
			.run((context) -> assertThat(context.containsBean("cloudFoundryWebFluxEndpointHandlerMapping")).isFalse());
	}

	@Test
	void cloudFoundryManagementEndpointsDisabled() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION=---", "management.cloudfoundry.enabled:false")
			.run((context) -> assertThat(context.containsBean("cloudFoundryWebFluxEndpointHandlerMapping")).isFalse());
	}

	@Test
	void allEndpointsAvailableUnderCloudFoundryWithoutEnablingWebIncludes() {
		this.contextRunner.withBean(TestEndpoint.class, TestEndpoint::new)
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(context);
				Collection<ExposableWebEndpoint> endpoints = handlerMapping.getEndpoints();
				List<EndpointId> endpointIds = endpoints.stream().map(ExposableWebEndpoint::getEndpointId).toList();
				assertThat(endpointIds).contains(EndpointId.of("test"));
			});
	}

	@Test
	void endpointPathCustomizationIsNotApplied() {
		this.contextRunner.withBean(TestEndpoint.class, TestEndpoint::new)
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				CloudFoundryWebFluxEndpointHandlerMapping handlerMapping = getHandlerMapping(context);
				Collection<ExposableWebEndpoint> endpoints = handlerMapping.getEndpoints();
				ExposableWebEndpoint endpoint = endpoints.stream()
					.filter((candidate) -> EndpointId.of("test").equals(candidate.getEndpointId()))
					.findFirst()
					.get();
				assertThat(endpoint.getOperations()).hasSize(1);
				WebOperation operation = endpoint.getOperations().iterator().next();
				assertThat(operation.getRequestPredicate().getPath()).isEqualTo("test");
			});
	}

	@Test
	void healthEndpointInvokerShouldBeCloudFoundryWebExtension() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class))
			.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
					"vcap.application.cf_api:https://my-cloud-controller.com")
			.run((context) -> {
				Collection<ExposableWebEndpoint> endpoints = getHandlerMapping(context).getEndpoints();
				ExposableWebEndpoint endpoint = endpoints.iterator().next();
				assertThat(endpoint.getOperations()).hasSize(2);
				WebOperation webOperation = findOperationWithRequestPath(endpoint, "health");
				assertThat(webOperation).extracting("invoker")
					.extracting("target")
					.isInstanceOf(CloudFoundryReactiveHealthEndpointWebExtension.class);
			});
	}

	@Test
	@WithResource(name = "git.properties", content = """
			#Generated by Git-Commit-Id-Plugin
			#Thu May 23 09:26:42 BST 2013
			git.commit.id.abbrev=e02a4f3
			git.commit.user.email=dsyer@vmware.com
			git.commit.message.full=Update Spring
			git.commit.id=e02a4f3b6f452cdbf6dd311f1362679eb4c31ced
			git.commit.message.short=Update Spring
			git.commit.user.name=Dave Syer
			git.build.user.name=Dave Syer
			git.build.user.email=dsyer@vmware.com
			git.branch=develop
			git.commit.time=2013-04-24T08\\:42\\:13+0100
			git.build.time=2013-05-23T09\\:26\\:42+0100
			""")
	@SuppressWarnings("unchecked")
	void gitFullDetailsAlwaysPresent() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---").run((context) -> {
			CloudFoundryInfoEndpointWebExtension extension = context
				.getBean(CloudFoundryInfoEndpointWebExtension.class);
			Map<String, Object> git = (Map<String, Object>) extension.info().get("git");
			assertThat(git).isNotNull();
			Map<String, Object> commit = (Map<String, Object>) git.get("commit");
			assertThat(commit).hasSize(4);
		});
	}

	@Test
	@WithPackageResources("test.jks")
	void skipSslValidation() throws IOException {
		JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails("JKS", null, "classpath:test.jks", "secret");
		SslBundle sslBundle = SslBundle.of(new JksSslStoreBundle(keyStoreDetails, keyStoreDetails));
		try (MockWebServer server = new MockWebServer()) {
			server.useHttps(sslBundle.createSslContext().getSocketFactory(), false);
			server.enqueue(new MockResponse().setResponseCode(204));
			server.start();
			this.contextRunner.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class))
				.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:https://my-cloud-controller.com",
						"management.cloudfoundry.skip-ssl-validation:true")
				.run((context) -> assertThat(getHandlerMapping(context))
					.extracting("securityInterceptor.cloudFoundrySecurityService.webClient",
							InstanceOfAssertFactories.type(WebClient.class))
					.satisfies((webClient) -> {
						ResponseEntity<Void> response = webClient.get()
							.uri(server.url("/").uri())
							.retrieve()
							.toBodilessEntity()
							.block(Duration.ofSeconds(30));
						assertThat(response).isNotNull();
						assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));
					}));
		}
	}

	@Test
	@WithPackageResources("test.jks")
	void sslValidationNotSkippedByDefault() throws IOException {
		JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails("JKS", null, "classpath:test.jks", "secret");
		SslBundle sslBundle = SslBundle.of(new JksSslStoreBundle(keyStoreDetails, keyStoreDetails));
		try (MockWebServer server = new MockWebServer()) {
			server.useHttps(sslBundle.createSslContext().getSocketFactory(), false);
			server.enqueue(new MockResponse().setResponseCode(204));
			server.start();
			this.contextRunner.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class))
				.withPropertyValues("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
						"vcap.application.cf_api:https://my-cloud-controller.com")
				.run((context) -> assertThat(getHandlerMapping(context))
					.extracting("securityInterceptor.cloudFoundrySecurityService.webClient",
							InstanceOfAssertFactories.type(WebClient.class))
					.satisfies((webClient) -> assertThatExceptionOfType(RuntimeException.class)
						.isThrownBy(() -> webClient.get()
							.uri(server.url("/").uri())
							.retrieve()
							.toBodilessEntity()
							.block(Duration.ofSeconds(30)))
						.withCauseInstanceOf(SSLException.class)));
		}
	}

	private CloudFoundryWebFluxEndpointHandlerMapping getHandlerMapping(ApplicationContext context) {
		return context.getBean("cloudFoundryWebFluxEndpointHandlerMapping",
				CloudFoundryWebFluxEndpointHandlerMapping.class);
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

	}

	@Configuration(proxyBeanMethods = false)
	static class WebClientCustomizerConfig {

		@Bean
		WebClientCustomizer webClientCustomizer() {
			return mock(WebClientCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDetailsServiceConfiguration {

		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return new MapReactiveUserDetailsService(
					User.withUsername("alice").password("secret").roles("admin").build());
		}

	}

}
