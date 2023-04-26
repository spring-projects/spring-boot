/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.reactive;

import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.reactive.WebFluxEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.metrics.web.TestController;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.filter.reactive.ServerHttpObservationFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxObservationAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Dmytro Nosan
 * @author Madhura Bhave
 * @author Jonatan Ivanov
 */
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("removal")
class WebFluxObservationAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.with(MetricsRun.simple())
		.withConfiguration(
				AutoConfigurations.of(ObservationAutoConfiguration.class, WebFluxObservationAutoConfiguration.class));

	@Test
	void shouldProvideWebFluxObservationFilter() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ServerHttpObservationFilter.class));
	}

	@Test
	void shouldProvideWebFluxObservationFilterOrdered() {
		this.contextRunner.withBean(FirstWebFilter.class).withBean(ThirdWebFilter.class).run((context) -> {
			List<WebFilter> webFilters = context.getBeanProvider(WebFilter.class).orderedStream().toList();
			assertThat(webFilters.get(0)).isInstanceOf(FirstWebFilter.class);
			assertThat(webFilters.get(1)).isInstanceOf(ServerHttpObservationFilter.class);
			assertThat(webFilters.get(2)).isInstanceOf(ThirdWebFilter.class);
		});
	}

	@Test
	void shouldUseCustomConventionWhenAvailable() {
		this.contextRunner.withUserConfiguration(CustomConventionConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ServerHttpObservationFilter.class);
			assertThat(context).getBean(ServerHttpObservationFilter.class)
				.extracting("observationConvention")
				.isInstanceOf(CustomConvention.class);
		});
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, ObservationAutoConfiguration.class,
					WebFluxAutoConfiguration.class))
			.withPropertyValues("management.metrics.web.server.max-uri-tags=2")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context);
				assertThat(registry.get("http.server.requests").meters()).hasSizeLessThanOrEqualTo(2);
				assertThat(output).contains("Reached the maximum number of URI tags for 'http.server.requests'");
			});
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDeniedWhenUsingCustomObservationName(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, ObservationAutoConfiguration.class,
					WebFluxAutoConfiguration.class))
			.withPropertyValues("management.metrics.web.server.max-uri-tags=2",
					"management.observations.http.server.requests.name=my.http.server.requests")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context);
				assertThat(registry.get("my.http.server.requests").meters()).hasSizeLessThanOrEqualTo(2);
				assertThat(output).contains("Reached the maximum number of URI tags for 'my.http.server.requests'");
			});
	}

	@Test
	void whenAnActuatorEndpointIsCalledObservationsShouldBeRecorded() {
		this.contextRunner.withUserConfiguration(TestController.class, TestObservationRegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(InfoEndpointAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, WebFluxEndpointManagementContextConfiguration.class,
					MetricsAutoConfiguration.class, ObservationAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=info")
			.run((context) -> {
				TestObservationRegistry observationRegistry = getInitializedTestObservationRegistry(context, "/test0",
						"/actuator/info");
				TestObservationRegistryAssert.assertThat(observationRegistry)
					.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 2)
					.hasAnObservationWithAKeyValue("http.url", "/test0")
					.hasAnObservationWithAKeyValue("http.url", "/actuator/info");
			});
	}

	@Test
	void whenActuatorObservationsEnabledObservationsShouldBeRecorded() {
		this.contextRunner.withUserConfiguration(TestController.class, TestObservationRegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(InfoEndpointAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, WebFluxEndpointManagementContextConfiguration.class,
					MetricsAutoConfiguration.class, ObservationAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=info",
					"management.observations.http.server.actuator.enabled=true")
			.run((context) -> {
				TestObservationRegistry observationRegistry = getInitializedTestObservationRegistry(context, "/test0",
						"/actuator/info");
				TestObservationRegistryAssert.assertThat(observationRegistry)
					.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 2)
					.hasAnObservationWithAKeyValue("http.url", "/test0")
					.hasAnObservationWithAKeyValue("http.url", "/actuator/info");
			});
	}

	@Test
	void whenActuatorObservationsDisabledObservationsShouldNotBeRecorded() {
		this.contextRunner.withUserConfiguration(TestController.class, TestObservationRegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(InfoEndpointAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, WebFluxEndpointManagementContextConfiguration.class,
					MetricsAutoConfiguration.class, ObservationAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=info",
					"management.observations.http.server.actuator.enabled=false")
			.run((context) -> {
				assertThat(context).hasBean("actuatorWebEndpointObservationPredicate");
				TestObservationRegistry observationRegistry = getInitializedTestObservationRegistry(context, "/test0",
						"/actuator/info");
				TestObservationRegistryAssert.assertThat(observationRegistry)
					.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 1)
					.hasAnObservationWithAKeyValue("http.url", "/test0");
			});
	}

	@Test
	void whenActuatorObservationsDisabledObservationsShouldNotBeRecordedUsingCustomEndpointBasePath() {
		this.contextRunner.withUserConfiguration(TestController.class, TestObservationRegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(InfoEndpointAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, WebFluxEndpointManagementContextConfiguration.class,
					MetricsAutoConfiguration.class, ObservationAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=info",
					"management.observations.http.server.actuator.enabled=false",
					"management.endpoints.web.base-path=/management")
			.run((context) -> {
				assertThat(context).hasBean("actuatorWebEndpointObservationPredicate");
				TestObservationRegistry observationRegistry = getInitializedTestObservationRegistry(context, "/test0",
						"/management/info");
				TestObservationRegistryAssert.assertThat(observationRegistry)
					.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 1)
					.hasAnObservationWithAKeyValue("http.url", "/test0");
			});
	}

	/**
	 * Due to limitations in {@code WebTestClient}, these tests need to start a real
	 * webserver and utilize a real http client with a real http request.
	 */
	@Test
	void whenActuatorObservationsDisabledObservationsShouldNotBeRecordedUsingCustomWebfluxBasePath() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
			.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
					ReactiveWebServerFactoryAutoConfiguration.class, InfoEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					WebFluxEndpointManagementContextConfiguration.class, MetricsAutoConfiguration.class,
					ObservationAutoConfiguration.class, WebFluxObservationAutoConfiguration.class))
			.withUserConfiguration(TestController.class, TestObservationRegistryConfiguration.class)
			.withPropertyValues("server.port=0", "management.endpoints.web.exposure.include=info",
					"management.observations.http.server.actuator.enabled=false", "spring.webflux.base-path=/test-path")
			.run((context) -> {
				assertThat(context).hasBean("actuatorWebEndpointObservationPredicate");
				WebTestClient client = createWebTestClientForLocalPort(context);
				TestObservationRegistry observationRegistry = getInitializedTestObservationRegistry(context, client,
						"/test-path/test0", "/test-path/actuator/info");
				TestObservationRegistryAssert.assertThat(observationRegistry)
					.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 1)
					.hasAnObservationWithAKeyValue("http.url", "/test-path/test0");
			});
	}

	/**
	 * Due to limitations in {@code WebTestClient}, these tests need to start a real
	 * webserver and utilize a real http client with a real http request.
	 */
	@Test
	void whenActuatorObservationsDisabledObservationsShouldNotBeRecordedUsingCustomWebfluxBasePathAndCustomEndpointBasePath() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
			.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
					ReactiveWebServerFactoryAutoConfiguration.class, InfoEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					WebFluxEndpointManagementContextConfiguration.class, MetricsAutoConfiguration.class,
					ObservationAutoConfiguration.class, WebFluxObservationAutoConfiguration.class))
			.withUserConfiguration(TestController.class, TestObservationRegistryConfiguration.class)
			.withPropertyValues("server.port=0", "management.endpoints.web.exposure.include=info",
					"management.observations.http.server.actuator.enabled=false", "spring.webflux.base-path=/test-path",
					"management.endpoints.web.base-path=/management")
			.run((context) -> {
				assertThat(context).hasBean("actuatorWebEndpointObservationPredicate");
				WebTestClient client = createWebTestClientForLocalPort(context);
				TestObservationRegistry observationRegistry = getInitializedTestObservationRegistry(context, client,
						"/test-path/test0", "/test-path/management/info");
				TestObservationRegistryAssert.assertThat(observationRegistry)
					.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 1)
					.hasAnObservationWithAKeyValue("http.url", "/test-path/test0");
			});
	}

	@Test
	void shouldNotDenyNorLogIfMaxUrisIsNotReached(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, ObservationAutoConfiguration.class,
					WebFluxAutoConfiguration.class))
			.withPropertyValues("management.metrics.web.server.max-uri-tags=5")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context);
				assertThat(registry.get("http.server.requests").meters()).hasSize(3);
				assertThat(output).doesNotContain("Reached the maximum number of URI tags for 'http.server.requests'");
			});
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableReactiveWebApplicationContext context)
			throws Exception {
		return getInitializedMeterRegistry(context, "/test0", "/test1", "/test2");
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableReactiveWebApplicationContext context, String... urls) {
		assertThat(context).hasSingleBean(ServerHttpObservationFilter.class);
		WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
		for (String url : urls) {
			client.get().uri(url).exchange().expectStatus().isOk();
		}
		return context.getBean(MeterRegistry.class);
	}

	private TestObservationRegistry getInitializedTestObservationRegistry(
			AssertableReactiveWebApplicationContext context, String... urls) {
		WebTestClient client = WebTestClient.bindToApplicationContext(context).configureClient().build();
		return getInitializedTestObservationRegistry(context, client, urls);
	}

	private TestObservationRegistry getInitializedTestObservationRegistry(
			AssertableReactiveWebApplicationContext context, WebTestClient client, String... urls) {
		assertThat(context).hasSingleBean(ServerHttpObservationFilter.class);
		for (String url : urls) {
			client.get().uri(url).exchange().expectStatus().isOk();
		}
		return context.getBean(TestObservationRegistry.class);
	}

	private WebTestClient createWebTestClientForLocalPort(AssertableReactiveWebApplicationContext context) {
		int port = ((AnnotationConfigReactiveWebServerApplicationContext) context.getSourceApplicationContext())
			.getWebServer()
			.getPort();
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Configuration(proxyBeanMethods = false)
	static class TestObservationRegistryConfiguration {

		@Bean
		ObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConventionConfiguration {

		@Bean
		CustomConvention customConvention() {
			return new CustomConvention();
		}

	}

	static class CustomConvention extends DefaultServerRequestObservationConvention {

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class FirstWebFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			return chain.filter(exchange);
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE + 2)
	static class ThirdWebFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			return chain.filter(exchange);
		}

	}

}
