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

package org.springframework.boot.actuate.health;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
import org.springframework.context.ConfigurableApplicationContext;
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
@RunWith(WebEndpointRunners.class)
public class HealthEndpointWebIntegrationTests {

	private static WebTestClient client;

	private static ConfigurableApplicationContext context;

	@Test
	public void whenHealthIsUp200ResponseIsReturned() {
		client.get().uri("/actuator/health").exchange().expectStatus().isOk().expectBody()
				.jsonPath("status").isEqualTo("UP").jsonPath("details.alpha.status")
				.isEqualTo("UP").jsonPath("details.bravo.status").isEqualTo("UP");
	}

	@Test
	public void whenHealthIsDown503ResponseIsReturned() {
		HealthIndicatorRegistry registry = context.getBean(HealthIndicatorRegistry.class);
		registry.register("charlie", () -> Health.down().build());
		try {
			client.get().uri("/actuator/health").exchange().expectStatus()
					.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody()
					.jsonPath("status").isEqualTo("DOWN").jsonPath("details.alpha.status")
					.isEqualTo("UP").jsonPath("details.bravo.status").isEqualTo("UP")
					.jsonPath("details.charlie.status").isEqualTo("DOWN");
		}
		finally {
			registry.unregister("charlie");
		}
	}

	@Test
	public void whenHealthIndicatorIsRemovedResponseIsAltered() {
		HealthIndicatorRegistry registry = context.getBean(HealthIndicatorRegistry.class);
		HealthIndicator bravo = registry.unregister("bravo");
		try {
			client.get().uri("/actuator/health").exchange().expectStatus().isOk()
					.expectBody().jsonPath("status").isEqualTo("UP")
					.jsonPath("details.alpha.status").isEqualTo("UP")
					.jsonPath("details.bravo.status").doesNotExist();
		}
		finally {
			registry.register("bravo", bravo);
		}
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public HealthIndicatorRegistry healthIndicatorFactory(
				Map<String, HealthIndicator> healthIndicators) {
			return new HealthIndicatorRegistryFactory()
					.createHealthIndicatorRegistry(healthIndicators);
		}

		@Bean
		public HealthEndpoint healthEndpoint(HealthIndicatorRegistry registry) {
			return new HealthEndpoint(new CompositeHealthIndicator(
					new OrderedHealthAggregator(), registry));
		}

		@Bean
		public HealthEndpointWebExtension healthWebEndpointExtension(
				HealthEndpoint healthEndpoint) {
			return new HealthEndpointWebExtension(healthEndpoint,
					new HealthWebEndpointResponseMapper(new HealthStatusHttpMapper(),
							ShowDetails.ALWAYS,
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
