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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CompositeHealthIndicator}
 *
 * @author Tyler J. Frederick
 * @author Phillip Webb
 * @author Christian Dupuis
 */
class CompositeHealthIndicatorTests {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	private HealthAggregator healthAggregator;

	@Mock
	private HealthIndicator one;

	@Mock
	private HealthIndicator two;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health()).willReturn(new Health.Builder().unknown().withDetail("1", "1").build());
		given(this.two.health()).willReturn(new Health.Builder().unknown().withDetail("2", "2").build());

		this.healthAggregator = new OrderedHealthAggregator();
	}

	@AfterAll
	static void shutdownExecutor() {
		executor.shutdown();
	}

	@Test
	void createWithIndicators() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(this.healthAggregator, indicators);
		Health result = composite.health();
		assertThat(result.getDetails()).hasSize(2);
		assertThat(result.getDetails()).containsEntry("one",
				new Health.Builder().unknown().withDetail("1", "1").build());
		assertThat(result.getDetails()).containsEntry("two",
				new Health.Builder().unknown().withDetail("2", "2").build());
	}

	@Test
	void testSerialization() throws Exception {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("db1", this.one);
		indicators.put("db2", this.two);
		CompositeHealthIndicator innerComposite = new CompositeHealthIndicator(this.healthAggregator, indicators);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(this.healthAggregator,
				Collections.singletonMap("db", innerComposite));
		Health result = composite.health();
		ObjectMapper mapper = new ObjectMapper();
		assertThat(mapper.writeValueAsString(result))
				.isEqualTo("{\"status\":\"UNKNOWN\",\"details\":{\"db\":{\"status\":\"UNKNOWN\""
						+ ",\"details\":{\"db1\":{\"status\":\"UNKNOWN\",\"details\""
						+ ":{\"1\":\"1\"}},\"db2\":{\"status\":\"UNKNOWN\",\"details\":{\"2\":\"2\"}}}}}}");
	}

	@Test
	void testWithSequentialStrategy() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow-1", new TimeoutHealth(200, Status.UP));
		indicators.put("slow-2", new TimeoutHealth(300, Status.UP));
		CompositeHealthIndicator indicator = new CompositeHealthIndicator(this.healthAggregator, indicators)
				.sequential();
		StopWatch watch = new StopWatch();
		watch.start();
		Health health = indicator.health();
		watch.stop();
		assertThat(watch.getLastTaskTimeMillis()).isBetween(500L, 750L);
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys("slow-1", "slow-2");
		assertThat(health.getDetails().get("slow-1")).isEqualTo(Health.up().build());
		assertThat(health.getDetails().get("slow-2")).isEqualTo(Health.up().build());
	}

	@Test
	@Timeout(1)
	void testWithParallelStrategy() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow-1", new TimeoutHealth(300, Status.UP));
		indicators.put("slow-2", new TimeoutHealth(800, Status.UP));
		IllegalStateException ex = new IllegalStateException("No Connection");
		indicators.put("error", () -> {
			throw ex;
		});
		CompositeHealthIndicator indicator = new CompositeHealthIndicator(this.healthAggregator, indicators)
				.parallel(executor);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsOnlyKeys("slow-1", "slow-2", "error");
		assertThat(health.getDetails().get("slow-1")).isEqualTo(Health.up().build());
		assertThat(health.getDetails().get("slow-2")).isEqualTo(Health.up().build());
		assertThat(health.getDetails().get("error")).isEqualTo(Health.down(ex).build());
	}

	@Test
	void testWithParallelStrategyTimeoutReached() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow", new TimeoutHealth(250, Status.UP));
		indicators.put("fast", new TimeoutHealth(10, Status.UP));
		CompositeHealthIndicator indicator = new CompositeHealthIndicator(this.healthAggregator, indicators)
				.parallel(executor, 200, null);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys("slow", "fast");
		assertThat(health.getDetails().get("slow")).isEqualTo(Health.unknown().build());
		assertThat(health.getDetails().get("fast")).isEqualTo(Health.up().build());
	}

	@Test
	void testWithParallelStrategyTimeoutReachedCustomTimeoutFallback() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow", new TimeoutHealth(250, Status.UP));
		indicators.put("fast", new TimeoutHealth(10, Status.UP));
		CompositeHealthIndicator indicator = new CompositeHealthIndicator(this.healthAggregator, indicators)
				.parallel(executor, 200, Health.down().build());
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsOnlyKeys("slow", "fast");
		assertThat(health.getDetails().get("slow")).isEqualTo(Health.down().build());
		assertThat(health.getDetails().get("fast")).isEqualTo(Health.up().build());
	}

	@Test
	void testWithParallelStrategyInterrupted() throws InterruptedException {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("slow", new TimeoutHealth(800, Status.UP));
		indicators.put("fast", new TimeoutHealth(300, Status.UP));
		InterruptedException ex = new InterruptedException();
		CompositeHealthIndicator indicator = new CompositeHealthIndicator(this.healthAggregator, indicators)
				.parallel(executor);
		AtomicReference<Health> healthReference = new AtomicReference<>();
		Thread thread = new Thread(() -> healthReference.set(indicator.health()));
		thread.start();
		thread.join(100);
		thread.interrupt();
		thread.join();
		Health health = healthReference.get();
		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
		assertThat(health.getDetails()).containsOnlyKeys("slow", "fast");
		assertThat(health.getDetails().get("slow")).isEqualTo(Health.unknown().withException(ex).build());
		assertThat(health.getDetails().get("fast")).isEqualTo(Health.unknown().withException(ex).build());
	}

	private static final class TimeoutHealth implements HealthIndicator {

		private final long timeout;

		private final Status status;

		TimeoutHealth(long timeout, Status status) {
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
