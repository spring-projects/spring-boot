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

package org.springframework.boot.actuate.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
public class HealthEndpointTests {

	@Test
	public void upAndUpIsAggregatedToUp() throws Exception {
		Map<String, HealthIndicator> healthIndicators = new HashMap<>();
		healthIndicators.put("up", () -> new Health.Builder().status(Status.UP).build());
		healthIndicators.put("upAgain",
				() -> new Health.Builder().status(Status.UP).build());
		HealthEndpoint endpoint = new HealthEndpoint(new OrderedHealthAggregator(),
				healthIndicators);
		assertThat(endpoint.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void upAndDownIsAggregatedToDown() throws Exception {
		Map<String, HealthIndicator> healthIndicators = new HashMap<>();
		healthIndicators.put("up", () -> new Health.Builder().status(Status.UP).build());
		healthIndicators.put("down",
				() -> new Health.Builder().status(Status.DOWN).build());
		HealthEndpoint endpoint = new HealthEndpoint(new OrderedHealthAggregator(),
				healthIndicators);
		assertThat(endpoint.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void unknownStatusMapsToUnknown() throws Exception {
		Map<String, HealthIndicator> healthIndicators = new HashMap<>();
		healthIndicators.put("status", () -> new Health.Builder().status("FINE").build());
		HealthEndpoint endpoint = new HealthEndpoint(new OrderedHealthAggregator(),
				healthIndicators);
		assertThat(endpoint.health().getStatus()).isEqualTo(Status.UNKNOWN);
	}

}
