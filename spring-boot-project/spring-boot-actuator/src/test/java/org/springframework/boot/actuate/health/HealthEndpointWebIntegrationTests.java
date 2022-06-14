/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ReflectionUtils;

/**
 * Integration tests for {@link HealthEndpoint} and {@link HealthEndpointWebExtension}
 * exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class HealthEndpointWebIntegrationTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	@WebEndpointTest
	void whenHealthIsUp200ResponseIsReturned(WebTestClient client) {
		client.get().uri("/actuator/health").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectBody().jsonPath("status").isEqualTo("UP").jsonPath("components.alpha.status").isEqualTo("UP")
				.jsonPath("components.bravo.status").isEqualTo("UP");
	}

	@WebEndpointTest
	void whenHealthIsUpAndAcceptsV3Request200ResponseIsReturned(WebTestClient client) {
		client.get().uri("/actuator/health").headers((headers) -> headers.set(HttpHeaders.ACCEPT, V3_JSON)).exchange()
				.expectStatus().isOk().expectBody().jsonPath("status").isEqualTo("UP")
				.jsonPath("components.alpha.status").isEqualTo("UP").jsonPath("components.bravo.status")
				.isEqualTo("UP");
	}

	@WebEndpointTest
	void whenHealthIsUpAndAcceptsAllRequest200ResponseIsReturned(WebTestClient client) {
		client.get().uri("/actuator/health").headers((headers) -> headers.set(HttpHeaders.ACCEPT, "*/*")).exchange()
				.expectStatus().isOk().expectBody().jsonPath("status").isEqualTo("UP")
				.jsonPath("components.alpha.status").isEqualTo("UP").jsonPath("components.bravo.status")
				.isEqualTo("UP");
	}

	@WebEndpointTest
	void whenHealthIsUpAndV2Request200ResponseIsReturnedInV2Format(WebTestClient client) {
		client.get().uri("/actuator/health").headers((headers) -> headers.set(HttpHeaders.ACCEPT, V2_JSON)).exchange()
				.expectStatus().isOk().expectBody().jsonPath("status").isEqualTo("UP").jsonPath("details.alpha.status")
				.isEqualTo("UP").jsonPath("details.bravo.status").isEqualTo("UP");
	}

	@WebEndpointTest
	void whenHealthIsDown503ResponseIsReturned(ApplicationContext context, WebTestClient client) {
		HealthIndicator healthIndicator = () -> Health.down().build();
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono.just(Health.down().build());
		withHealthContributor(context, "charlie", healthIndicator, reactiveHealthIndicator,
				() -> client.get().uri("/actuator/health").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
						.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status").isEqualTo("DOWN")
						.jsonPath("components.alpha.status").isEqualTo("UP").jsonPath("components.bravo.status")
						.isEqualTo("UP").jsonPath("components.charlie.status").isEqualTo("DOWN"));
	}

	@WebEndpointTest
	void whenComponentHealthIsDown503ResponseIsReturned(ApplicationContext context, WebTestClient client) {
		HealthIndicator healthIndicator = () -> Health.down().build();
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono.just(Health.down().build());
		withHealthContributor(context, "charlie", healthIndicator, reactiveHealthIndicator,
				() -> client.get().uri("/actuator/health/charlie").accept(MediaType.APPLICATION_JSON).exchange()
						.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status")
						.isEqualTo("DOWN"));
	}

	@WebEndpointTest
	void whenComponentInstanceHealthIsDown503ResponseIsReturned(ApplicationContext context, WebTestClient client) {
		HealthIndicator healthIndicator = () -> Health.down().build();
		CompositeHealthContributor composite = CompositeHealthContributor
				.fromMap(Collections.singletonMap("one", healthIndicator));
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono.just(Health.down().build());
		CompositeReactiveHealthContributor reactiveComposite = CompositeReactiveHealthContributor
				.fromMap(Collections.singletonMap("one", reactiveHealthIndicator));
		withHealthContributor(context, "charlie", composite, reactiveComposite,
				() -> client.get().uri("/actuator/health/charlie/one").accept(MediaType.APPLICATION_JSON).exchange()
						.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status")
						.isEqualTo("DOWN"));
	}

	private void withHealthContributor(ApplicationContext context, String name, HealthContributor healthContributor,
			ReactiveHealthContributor reactiveHealthContributor, ThrowingCallable callable) {
		HealthContributorRegistry healthContributorRegistry = getContributorRegistry(context,
				HealthContributorRegistry.class);
		healthContributorRegistry.registerContributor(name, healthContributor);
		ReactiveHealthContributorRegistry reactiveHealthContributorRegistry = getContributorRegistry(context,
				ReactiveHealthContributorRegistry.class);
		if (reactiveHealthContributorRegistry != null) {
			reactiveHealthContributorRegistry.registerContributor(name, reactiveHealthContributor);
		}
		try {
			callable.call();
		}
		catch (Throwable ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		finally {
			healthContributorRegistry.unregisterContributor(name);
			if (reactiveHealthContributorRegistry != null) {
				reactiveHealthContributorRegistry.unregisterContributor(name);
			}
		}
	}

	private <R extends ContributorRegistry<?>> R getContributorRegistry(ApplicationContext context,
			Class<R> registryType) {
		return context.getBeanProvider(registryType).getIfAvailable();
	}

	@WebEndpointTest
	void whenHealthIndicatorIsRemovedResponseIsAltered(WebTestClient client, ApplicationContext context) {
		String name = "bravo";
		HealthContributorRegistry healthContributorRegistry = getContributorRegistry(context,
				HealthContributorRegistry.class);
		HealthContributor bravo = healthContributorRegistry.unregisterContributor(name);
		ReactiveHealthContributorRegistry reactiveHealthContributorRegistry = getContributorRegistry(context,
				ReactiveHealthContributorRegistry.class);
		ReactiveHealthContributor reactiveBravo = (reactiveHealthContributorRegistry != null)
				? reactiveHealthContributorRegistry.unregisterContributor(name) : null;
		try {
			client.get().uri("/actuator/health").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
					.expectBody().jsonPath("status").isEqualTo("UP").jsonPath("components.alpha.status").isEqualTo("UP")
					.jsonPath("components.bravo.status").doesNotExist();
		}
		finally {
			healthContributorRegistry.registerContributor(name, bravo);
			if (reactiveHealthContributorRegistry != null && reactiveBravo != null) {
				reactiveHealthContributorRegistry.registerContributor(name, reactiveBravo);
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		HealthContributorRegistry healthContributorRegistry(Map<String, HealthContributor> healthContributorBeans) {
			return new DefaultHealthContributorRegistry(healthContributorBeans);
		}

		@Bean
		@ConditionalOnWebApplication(type = Type.REACTIVE)
		ReactiveHealthContributorRegistry reactiveHealthContributorRegistry(
				Map<String, HealthContributor> healthContributorBeans,
				Map<String, ReactiveHealthContributor> reactiveHealthContributorBeans) {
			Map<String, ReactiveHealthContributor> allIndicators = new LinkedHashMap<>(reactiveHealthContributorBeans);
			healthContributorBeans.forEach((name, contributor) -> allIndicators.computeIfAbsent(name,
					(key) -> ReactiveHealthContributor.adapt(contributor)));
			return new DefaultReactiveHealthContributorRegistry(allIndicators);
		}

		@Bean
		HealthEndpoint healthEndpoint(HealthContributorRegistry healthContributorRegistry,
				HealthEndpointGroups healthEndpointGroups) {
			return new HealthEndpoint(healthContributorRegistry, healthEndpointGroups, null);
		}

		@Bean
		@ConditionalOnWebApplication(type = Type.SERVLET)
		HealthEndpointWebExtension healthWebEndpointExtension(HealthContributorRegistry healthContributorRegistry,
				HealthEndpointGroups healthEndpointGroups) {
			return new HealthEndpointWebExtension(healthContributorRegistry, healthEndpointGroups, null);
		}

		@Bean
		@ConditionalOnWebApplication(type = Type.REACTIVE)
		ReactiveHealthEndpointWebExtension reactiveHealthWebEndpointExtension(
				ReactiveHealthContributorRegistry reactiveHealthContributorRegistry,
				HealthEndpointGroups healthEndpointGroups) {
			return new ReactiveHealthEndpointWebExtension(reactiveHealthContributorRegistry, healthEndpointGroups,
					null);
		}

		@Bean
		HealthEndpointGroups healthEndpointGroups() {
			TestHealthEndpointGroup primary = new TestHealthEndpointGroup();
			TestHealthEndpointGroup allTheAs = new TestHealthEndpointGroup((name) -> name.startsWith("a"));
			return HealthEndpointGroups.of(primary, Collections.singletonMap("alltheas", allTheAs));
		}

		@Bean
		HealthIndicator alphaHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		HealthIndicator bravoHealthIndicator() {
			return () -> Health.up().build();
		}

	}

}
