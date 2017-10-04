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
 * Integration tests for {@link StatusEndpoint} and {@link StatusWebEndpointExtension}
 * exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Stephane Nicoll
 */
@RunWith(WebEndpointRunners.class)
public class StatusEndpointWebIntegrationTests {

	private static WebTestClient client;

	private static ConfigurableApplicationContext context;

	@Test
	public void whenStatusIsUp200ResponseIsReturned() throws Exception {
		client.get().uri("/application/status").exchange().expectStatus().isOk()
				.expectBody().json("{\"status\":\"UP\"}");
	}

	@Test
	public void whenStatusIsDown503ResponseIsReturned() throws Exception {
		context.getBean("alphaHealthIndicator", TestHealthIndicator.class)
				.setHealth(Health.down().build());
		client.get().uri("/application/status").exchange().expectStatus()
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE).expectBody()
				.json("{\"status\":\"DOWN\"}");
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public StatusEndpoint statusEndpoint(
				Map<String, HealthIndicator> healthIndicators) {
			return new StatusEndpoint(
					new CompositeHealthIndicatorFactory().createHealthIndicator(
							new OrderedHealthAggregator(), healthIndicators));
		}

		@Bean
		public StatusWebEndpointExtension statusWebEndpointExtension(
				StatusEndpoint delegate) {
			return new StatusWebEndpointExtension(delegate, new HealthStatusHttpMapper());
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
