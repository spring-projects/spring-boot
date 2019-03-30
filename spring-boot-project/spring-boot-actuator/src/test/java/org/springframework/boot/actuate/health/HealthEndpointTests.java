/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class HealthEndpointTests {

	private static final HealthIndicator one = () -> new Health.Builder()
			.status(Status.UP).withDetail("first", "1").build();

	private static final HealthIndicator two = () -> new Health.Builder()
			.status(Status.UP).withDetail("second", "2").build();

	@Test
	public void statusAndFullDetailsAreExposed() {
		Map<String, HealthIndicator> healthIndicators = new HashMap<>();
		healthIndicators.put("up", one);
		healthIndicators.put("upAgain", two);
		HealthEndpoint endpoint = new HealthEndpoint(
				createHealthIndicator(healthIndicators));
		Health health = endpoint.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys("up", "upAgain");
		Health upHealth = (Health) health.getDetails().get("up");
		assertThat(upHealth.getDetails()).containsOnly(entry("first", "1"));
		Health upAgainHealth = (Health) health.getDetails().get("upAgain");
		assertThat(upAgainHealth.getDetails()).containsOnly(entry("second", "2"));
	}

	@Test
	public void statusForComponentIsExposed() {
		HealthEndpoint endpoint = new HealthEndpoint(
				createHealthIndicator(Collections.singletonMap("test", one)));
		Health health = endpoint.healthForComponent("test");
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnly(entry("first", "1"));
	}

	@Test
	public void statusForUnknownComponentReturnNull() {
		HealthEndpoint endpoint = new HealthEndpoint(
				createHealthIndicator(Collections.emptyMap()));
		Health health = endpoint.healthForComponent("does-not-exist");
		assertThat(health).isNull();
	}

	@Test
	public void statusForComponentInstanceIsExposed() {
		CompositeHealthIndicator compositeIndicator = new CompositeHealthIndicator(
				new OrderedHealthAggregator(),
				Collections.singletonMap("sub", () -> Health.down().build()));
		HealthEndpoint endpoint = new HealthEndpoint(createHealthIndicator(
				Collections.singletonMap("test", compositeIndicator)));
		Health health = endpoint.healthForComponentInstance("test", "sub");
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).isEmpty();
	}

	@Test
	public void statusForUnknownComponentInstanceReturnNull() {
		CompositeHealthIndicator compositeIndicator = new CompositeHealthIndicator(
				new OrderedHealthAggregator(),
				Collections.singletonMap("sub", () -> Health.down().build()));
		HealthEndpoint endpoint = new HealthEndpoint(createHealthIndicator(
				Collections.singletonMap("test", compositeIndicator)));
		Health health = endpoint.healthForComponentInstance("test", "does-not-exist");
		assertThat(health).isNull();
	}

	@Test
	public void statusForComponentInstanceThatIsNotACompositeReturnNull() {
		HealthEndpoint endpoint = new HealthEndpoint(createHealthIndicator(
				Collections.singletonMap("test", () -> Health.up().build())));
		Health health = endpoint.healthForComponentInstance("test", "does-not-exist");
		assertThat(health).isNull();
	}

	private HealthIndicator createHealthIndicator(
			Map<String, HealthIndicator> healthIndicators) {
		return new CompositeHealthIndicator(new OrderedHealthAggregator(),
				healthIndicators);
	}

}
