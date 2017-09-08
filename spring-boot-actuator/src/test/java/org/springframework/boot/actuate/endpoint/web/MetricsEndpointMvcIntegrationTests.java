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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link MetricsEndpoint} when exposed via Spring MVC
 *
 * @author Andy Wilkinson
 * @author Sergei Egorov
 */
@RunWith(WebEndpointsRunner.class)
public class MetricsEndpointMvcIntegrationTests {

	private static WebTestClient client;

	@Test
	public void home() {
		client.get().uri("/application/metrics").exchange().expectStatus().isOk()
				.expectBody().jsonPath("foo").isEqualTo(1);
	}

	@Test
	public void specificMetric() {
		client.get().uri("/application/metrics/foo").exchange().expectStatus().isOk()
				.expectBody().jsonPath("foo").isEqualTo(1);
	}

	@Test
	public void specificMetricWithDot() throws Exception {
		client.get().uri("/application/metrics/group2.a").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.length()").isEqualTo(1).jsonPath("['group2.a']")
				.isEqualTo("1");
	}

	@Test
	public void specificMetricWithNameThatCouldBeMistakenForAPathExtension() {
		client.get().uri("/application/metrics/bar.png").exchange().expectStatus().isOk()
				.expectBody().jsonPath("['bar.png']").isEqualTo(1);
	}

	@Test
	public void specificMetricThatDoesNotExist() throws Exception {
		client.get().uri("/application/metrics/bar").exchange().expectStatus()
				.isNotFound();
	}

	@Test
	public void regexAll() throws Exception {
		client.get().uri("/application/metrics?pattern=.*").exchange().expectStatus()
				.isOk().expectBody().jsonPath("$.length()").isEqualTo(6).jsonPath("foo")
				.isEqualTo(1).jsonPath("['bar.png']").isEqualTo(1)
				.jsonPath("['group1.a']").isEqualTo(1).jsonPath("['group1.b']")
				.isEqualTo(1).jsonPath("['group2.a']").isEqualTo(1).jsonPath("group2_a")
				.isEqualTo(1);
	}

	@Test
	public void regexGroupDot() throws Exception {
		client.get().uri("/application/metrics?pattern=group%5B0-9%5D%5C..*").exchange()
				.expectStatus().isOk().expectBody().jsonPath("$.length()").isEqualTo(3)
				.jsonPath("['group1.a']").isEqualTo(1).jsonPath("['group1.b']")
				.isEqualTo(1).jsonPath("['group2.a']").isEqualTo(1);
	}

	@Test
	public void regexGroup1() throws Exception {
		client.get().uri("/application/metrics?pattern=group1%5C..*").exchange()
				.expectStatus().isOk().expectBody().jsonPath("['group1.a']").isEqualTo(1)
				.jsonPath("['group1.b']").isEqualTo(1).jsonPath("$.length()")
				.isEqualTo(2);
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public MetricsEndpoint endpoint() {
			return new MetricsEndpoint(() -> Arrays.asList(new Metric<>("foo", 1),
					new Metric<>("bar.png", 1), new Metric<>("group1.a", 1),
					new Metric<>("group1.b", 1), new Metric<>("group2.a", 1),
					new Metric<>("group2_a", 1), new Metric<Integer>("baz", null)));
		}

	}

}
