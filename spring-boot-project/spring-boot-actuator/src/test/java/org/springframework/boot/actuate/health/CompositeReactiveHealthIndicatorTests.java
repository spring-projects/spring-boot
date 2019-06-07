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

import java.time.Duration;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositeReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
public class CompositeReactiveHealthIndicatorTests {

	private static final Health UNKNOWN_HEALTH = Health.unknown().withDetail("detail", "value").build();

	private static final Health HEALTHY = Health.up().build();

	private OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();

	private CompositeReactiveHealthIndicator indicator = new CompositeReactiveHealthIndicator(this.healthAggregator);

	@Test
	public void singleIndicator() {
		this.indicator.addHealthIndicator("test", () -> Mono.just(HEALTHY));
		StepVerifier.create(this.indicator.health()).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("test");
			assertThat(h.getDetails().get("test")).isEqualTo(HEALTHY);
		}).verifyComplete();
	}

	@Test
	public void longHealth() {
		for (int i = 0; i < 50; i++) {
			this.indicator.addHealthIndicator("test" + i, new TimeoutHealth(10000, Status.UP));
		}
		StepVerifier.withVirtualTime(this.indicator::health).expectSubscription().thenAwait(Duration.ofMillis(10000))
				.consumeNextWith((h) -> {
					assertThat(h.getStatus()).isEqualTo(Status.UP);
					assertThat(h.getDetails()).hasSize(50);
				}).verifyComplete();

	}

	@Test
	public void timeoutReachedUsesFallback() {
		this.indicator.addHealthIndicator("slow", new TimeoutHealth(10000, Status.UP))
				.addHealthIndicator("fast", new TimeoutHealth(10, Status.UP)).timeoutStrategy(100, UNKNOWN_HEALTH);
		StepVerifier.create(this.indicator.health()).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("slow", "fast");
			assertThat(h.getDetails().get("slow")).isEqualTo(UNKNOWN_HEALTH);
			assertThat(h.getDetails().get("fast")).isEqualTo(HEALTHY);
		}).verifyComplete();
	}

	@Test
	public void timeoutNotReached() {
		this.indicator.addHealthIndicator("slow", new TimeoutHealth(10000, Status.UP))
				.addHealthIndicator("fast", new TimeoutHealth(10, Status.UP)).timeoutStrategy(20000, null);
		StepVerifier.withVirtualTime(this.indicator::health).expectSubscription().thenAwait(Duration.ofMillis(10000))
				.consumeNextWith((h) -> {
					assertThat(h.getStatus()).isEqualTo(Status.UP);
					assertThat(h.getDetails()).containsOnlyKeys("slow", "fast");
					assertThat(h.getDetails().get("slow")).isEqualTo(HEALTHY);
					assertThat(h.getDetails().get("fast")).isEqualTo(HEALTHY);
				}).verifyComplete();
	}

	static class TimeoutHealth implements ReactiveHealthIndicator {

		private final long timeout;

		private final Status status;

		TimeoutHealth(long timeout, Status status) {
			this.timeout = timeout;
			this.status = status;
		}

		@Override
		public Mono<Health> health() {
			return Mono.delay(Duration.ofMillis(this.timeout)).map((l) -> Health.status(this.status).build());
		}

	}

}
