/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link HealthEndpoint} and {@link HealthEndpointWebExtension}
 * exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Andy Wilkinson
 */
class HealthEndpointWebIntegrationTests {

	@WebEndpointTest
	void whenHealthIsUp200ResponseIsReturned(WebTestClient client) {
		client.get().uri("/actuator/health").exchange().expectStatus().isOk().expectBody().jsonPath("status")
				.isEqualTo("UP").jsonPath("details.alpha.status").isEqualTo("UP").jsonPath("details.bravo.status")
				.isEqualTo("UP");
	}

	@WebEndpointTest
	void whenHealthIsDown503ResponseIsReturned(ApplicationContext context, WebTestClient client) throws Exception {
		withHealthIndicator(context, "charlie", () -> Health.down().build(), () -> Mono.just(Health.down().build()),
				() -> {
					client.get().uri("/actuator/health").exchange().expectStatus()
							.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status").isEqualTo("DOWN")
							.jsonPath("details.alpha.status").isEqualTo("UP").jsonPath("details.bravo.status")
							.isEqualTo("UP").jsonPath("details.charlie.status").isEqualTo("DOWN");
					return null;
				});
	}

	@WebEndpointTest
	void whenComponentHealthIsDown503ResponseIsReturned(ApplicationContext context, WebTestClient client)
			throws Exception {
		withHealthIndicator(context, "charlie", () -> Health.down().build(), () -> Mono.just(Health.down().build()),
				() -> {
					client.get().uri("/actuator/health/charlie").exchange().expectStatus()
							.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status")
							.isEqualTo("DOWN");
					return null;
				});
	}

	@WebEndpointTest
	void whenComponentInstanceHealthIsDown503ResponseIsReturned(ApplicationContext context, WebTestClient client)
			throws Exception {
		CompositeHealthIndicator composite = new CompositeHealthIndicator(new OrderedHealthAggregator(),
				Collections.singletonMap("one", () -> Health.down().build()));
		CompositeReactiveHealthIndicator reactiveComposite = new CompositeReactiveHealthIndicator(
				new OrderedHealthAggregator(), new DefaultReactiveHealthIndicatorRegistry(
						Collections.singletonMap("one", () -> Mono.just(Health.down().build()))));
		withHealthIndicator(context, "charlie", composite, reactiveComposite, () -> {
			client.get().uri("/actuator/health/charlie/one").exchange().expectStatus()
					.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status").isEqualTo("DOWN");
			return null;
		});
	}

	private void withHealthIndicator(ApplicationContext context, String name, HealthIndicator healthIndicator,
			ReactiveHealthIndicator reactiveHealthIndicator, Callable<Void> action) throws Exception {
		Consumer<String> unregister;
		Consumer<String> reactiveUnregister;
		try {
			ReactiveHealthIndicatorRegistry registry = context.getBean(ReactiveHealthIndicatorRegistry.class);
			registry.register(name, reactiveHealthIndicator);
			reactiveUnregister = registry::unregister;
		}
		catch (NoSuchBeanDefinitionException ex) {
			reactiveUnregister = (indicatorName) -> {
			};
			// Continue
		}
		HealthIndicatorRegistry registry = context.getBean(HealthIndicatorRegistry.class);
		registry.register(name, healthIndicator);
		unregister = reactiveUnregister.andThen(registry::unregister);
		try {
			action.call();
		}
		finally {
			unregister.accept("charlie");
		}
	}

	@WebEndpointTest
	void whenHealthIndicatorIsRemovedResponseIsAltered(WebTestClient client, ApplicationContext context) {
		Consumer<String> reactiveRegister = null;
		try {
			ReactiveHealthIndicatorRegistry registry = context.getBean(ReactiveHealthIndicatorRegistry.class);
			ReactiveHealthIndicator unregistered = registry.unregister("bravo");
			reactiveRegister = (name) -> registry.register(name, unregistered);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue
		}
		HealthIndicatorRegistry registry = context.getBean(HealthIndicatorRegistry.class);
		HealthIndicator bravo = registry.unregister("bravo");
		try {
			client.get().uri("/actuator/health").exchange().expectStatus().isOk().expectBody().jsonPath("status")
					.isEqualTo("UP").jsonPath("details.alpha.status").isEqualTo("UP").jsonPath("details.bravo.status")
					.doesNotExist();
		}
		finally {
			registry.register("bravo", bravo);
			if (reactiveRegister != null) {
				reactiveRegister.accept("bravo");
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	public static class TestConfiguration {

		@Bean
		public HealthIndicatorRegistry healthIndicatorFactory(Map<String, HealthIndicator> healthIndicators) {
			return new HealthIndicatorRegistryFactory().createHealthIndicatorRegistry(healthIndicators);
		}

		@Bean
		@ConditionalOnWebApplication(type = Type.REACTIVE)
		public ReactiveHealthIndicatorRegistry reactiveHealthIndicatorRegistry(
				Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
				Map<String, HealthIndicator> healthIndicators) {
			return new ReactiveHealthIndicatorRegistryFactory()
					.createReactiveHealthIndicatorRegistry(reactiveHealthIndicators, healthIndicators);
		}

		@Bean
		public HealthEndpoint healthEndpoint(HealthIndicatorRegistry registry) {
			return new HealthEndpoint(new CompositeHealthIndicator(new OrderedHealthAggregator(), registry));
		}

		@Bean
		@ConditionalOnWebApplication(type = Type.SERVLET)
		public HealthEndpointWebExtension healthWebEndpointExtension(HealthEndpoint healthEndpoint) {
			return new HealthEndpointWebExtension(healthEndpoint, new HealthWebEndpointResponseMapper(
					new HealthStatusHttpMapper(), ShowDetails.ALWAYS, new HashSet<>(Arrays.asList("ACTUATOR"))));
		}

		@Bean
		@ConditionalOnWebApplication(type = Type.REACTIVE)
		public ReactiveHealthEndpointWebExtension reactiveHealthWebEndpointExtension(
				ReactiveHealthIndicatorRegistry registry, HealthEndpoint healthEndpoint) {
			return new ReactiveHealthEndpointWebExtension(
					new CompositeReactiveHealthIndicator(new OrderedHealthAggregator(), registry),
					new HealthWebEndpointResponseMapper(new HealthStatusHttpMapper(), ShowDetails.ALWAYS,
							new HashSet<>(Arrays.asList("ACTUATOR"))));
		}

		@Bean
		public HealthIndicator alphaHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		public HealthIndicator bravoHealthIndicator() {
			return () -> Health.up().build();
		}

	}

}
