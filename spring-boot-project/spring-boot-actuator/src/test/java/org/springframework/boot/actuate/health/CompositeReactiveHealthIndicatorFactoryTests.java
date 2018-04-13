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

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeReactiveHealthIndicatorFactory}.
 *
 * @author Stephane Nicoll
 */
public class CompositeReactiveHealthIndicatorFactoryTests {

	private static final Health UP = new Health.Builder().status(Status.UP).build();

	private static final Health DOWN = new Health.Builder().status(Status.DOWN).build();

	@Test
	public void noHealthIndicator() {
		ReactiveHealthIndicator healthIndicator = createHealthIndicator(
				Collections.singletonMap("test", () -> Mono.just(UP)), null);
		StepVerifier.create(healthIndicator.health()).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("test");
		}).verifyComplete();
	}

	@Test
	public void defaultHealthIndicatorNameFactory() {
		ReactiveHealthIndicator healthIndicator = new CompositeReactiveHealthIndicatorFactory()
				.createReactiveHealthIndicator(new OrderedHealthAggregator(), Collections
						.singletonMap("myHealthIndicator", () -> Mono.just(UP)), null);
		StepVerifier.create(healthIndicator.health()).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("my");
		}).verifyComplete();
	}

	@Test
	public void healthIndicatorIsAdapted() {
		ReactiveHealthIndicator healthIndicator = createHealthIndicator(
				Collections.singletonMap("test", () -> Mono.just(UP)),
				Collections.singletonMap("regular", () -> DOWN));
		StepVerifier.create(healthIndicator.health()).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails()).containsOnlyKeys("test", "regular");
		}).verifyComplete();
	}

	@Test
	public void reactiveHealthIndicatorTakesPrecedence() {
		ReactiveHealthIndicator reactiveHealthIndicator = mock(
				ReactiveHealthIndicator.class);
		given(reactiveHealthIndicator.health()).willReturn(Mono.just(UP));
		HealthIndicator regularHealthIndicator = mock(HealthIndicator.class);
		given(regularHealthIndicator.health()).willReturn(UP);
		ReactiveHealthIndicator healthIndicator = createHealthIndicator(
				Collections.singletonMap("test", reactiveHealthIndicator),
				Collections.singletonMap("test", regularHealthIndicator));
		StepVerifier.create(healthIndicator.health()).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("test");
		}).verifyComplete();
		verify(reactiveHealthIndicator, times(1)).health();
		verify(regularHealthIndicator, never()).health();
	}

	private ReactiveHealthIndicator createHealthIndicator(
			Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
			Map<String, HealthIndicator> healthIndicators) {
		return new CompositeReactiveHealthIndicatorFactory((n) -> n)
				.createReactiveHealthIndicator(new OrderedHealthAggregator(),
						reactiveHealthIndicators, healthIndicators);
	}

}
