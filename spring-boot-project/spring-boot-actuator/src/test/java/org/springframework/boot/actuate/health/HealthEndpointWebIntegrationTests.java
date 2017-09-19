/*
 * Copyright 2012-2017 the original author or authors.
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
 * Integration tests for {@link HealthEndpoint} and {@link HealthWebEndpointExtension}
 * exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Andy Wilkinson
 */
@RunWith(WebEndpointRunners.class)
public class HealthEndpointWebIntegrationTests {

	private static WebTestClient client;

	private static ConfigurableApplicationContext context;

	@Test
	public void whenHealthIsUp200ResponseIsReturned() throws Exception {
		client.get().uri("/application/health").exchange().expectStatus().isOk()
				.expectBody().jsonPath("status").isEqualTo("UP")
				.jsonPath("details.alpha.status").isEqualTo("UP")
				.jsonPath("details.bravo.status").isEqualTo("UP");
	}

	@Test
	public void whenHealthIsDown503ResponseIsReturned() throws Exception {
		context.getBean("alphaHealthIndicator", TestHealthIndicator.class)
				.setHealth(Health.down().build());
		client.get().uri("/application/health").exchange().expectStatus()
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody().jsonPath("status")
				.isEqualTo("DOWN").jsonPath("details.alpha.status").isEqualTo("DOWN")
				.jsonPath("details.bravo.status").isEqualTo("UP");
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public HealthEndpoint healthEndpoint(
				Map<String, HealthIndicator> healthIndicators) {
			return new HealthEndpoint(
					new CompositeHealthIndicatorFactory().createHealthIndicator(
							new OrderedHealthAggregator(), healthIndicators));
		}

		@Bean
		public HealthWebEndpointExtension healthWebEndpointExtension(
				HealthEndpoint delegate) {
			return new HealthWebEndpointExtension(delegate, new HealthStatusHttpMapper());
		}

		@Bean
		public TestHealthIndicator alphaHealthIndicator() {
			return new TestHealthIndicator();
		}

		@Bean
		public TestHealthIndicator bravoHealthIndicator() {
			return new TestHealthIndicator();
		}

	}

	private static class TestHealthIndicator implements HealthIndicator {

		private Health health = Health.up().build();

		@Override
		public Health health() {
			Health result = this.health;
			this.health = Health.up().build();
			return result;
		}

		void setHealth(Health health) {
			this.health = health;
		}

	}

}
