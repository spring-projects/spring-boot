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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConcurrentlyHealthIndicatorStrategy}.
 *
 * @author Dmytro Nosan
 */
class ConcurrentlyHealthIndicatorStrategyTests {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	@AfterAll
	static void shutdownExecutor() {
		executor.shutdown();
	}

	@Test
	@Timeout(2)
	void testStrategy() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow-1", new TimeoutHealthIndicator(1500, Status.UP));
		indicators.put("slow-2", new TimeoutHealthIndicator(1500, Status.UP));
		indicators.put("error", new ErrorHealthIndicator());
		Map<String, Health> health = createStrategy().doHealth(indicators);
		assertThat(health).containsOnlyKeys("slow-1", "slow-2", "error");
		assertThat(health.get("slow-1")).isEqualTo(Health.up().build());
		assertThat(health.get("slow-2")).isEqualTo(Health.up().build());
		assertThat(health.get("error")).isEqualTo(Health.down(new UnsupportedOperationException()).build());
	}

	@Test
	void testTimeoutReachedDefaultFallback() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow", new TimeoutHealthIndicator(250, Status.UP));
		indicators.put("fast", new TimeoutHealthIndicator(10, Status.UP));
		Map<String, Health> health = createStrategy(200, null).doHealth(indicators);
		assertThat(health).containsOnlyKeys("slow", "fast");
		assertThat(health.get("slow")).isEqualTo(Health.unknown().build());
		assertThat(health.get("fast")).isEqualTo(Health.up().build());
	}

	@Test
	void testTimeoutReachedCustomTimeoutFallback() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow", new TimeoutHealthIndicator(250, Status.UP));
		indicators.put("fast", new TimeoutHealthIndicator(10, Status.UP));
		Map<String, Health> health = createStrategy(200, Health.down().build()).doHealth(indicators);
		assertThat(health).containsOnlyKeys("slow", "fast");
		assertThat(health.get("slow")).isEqualTo(Health.down().build());
		assertThat(health.get("fast")).isEqualTo(Health.up().build());
	}

	@Test
	void testInterrupted() throws InterruptedException {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow", new TimeoutHealthIndicator(750, Status.UP));
		indicators.put("fast", new TimeoutHealthIndicator(250, Status.UP));
		AtomicReference<Map<String, Health>> healthReference = new AtomicReference<>();
		Thread thread = new Thread(() -> healthReference.set(createStrategy().doHealth(indicators)));
		thread.start();
		thread.join(100);
		thread.interrupt();
		thread.join();
		Map<String, Health> health = healthReference.get();
		assertThat(health).containsOnlyKeys("slow", "fast");
		Health unknown = Health.unknown().withException(new InterruptedException()).build();
		assertThat(health.get("slow")).isEqualTo(unknown);
		assertThat(health.get("fast")).isEqualTo(unknown);
	}

	private HealthIndicatorStrategy createStrategy(long timeout, Health timeoutHealth) {
		return new ConcurrentlyHealthIndicatorStrategy(executor, timeout, timeoutHealth);
	}

	private HealthIndicatorStrategy createStrategy() {
		return new ConcurrentlyHealthIndicatorStrategy(executor);
	}

	private static final class ErrorHealthIndicator implements HealthIndicator {

		@Override
		public Health health() {
			throw new UnsupportedOperationException();
		}

	}

	private static final class TimeoutHealthIndicator implements HealthIndicator {

		private final long timeout;

		private final Status status;

		TimeoutHealthIndicator(long timeout, Status status) {
			this.timeout = timeout;
			this.status = status;
		}

		@Override
		public Health health() {
			ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
			try {
				return executorService
						.schedule(() -> Health.status(this.status).build(), this.timeout, TimeUnit.MILLISECONDS).get();
			}
			catch (Exception ex) {
				// never
				throw new RuntimeException(ex);
			}
			finally {
				executorService.shutdown();
			}
		}

	}

}
