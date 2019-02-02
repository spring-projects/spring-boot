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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ConcurrentCompositeHealthIndicator}
 *
 * @author Rik vd Ende
 */
public class ConcurrentCompositeHealthIndicatorTest {

	private HealthAggregator healthAggregator;

	@Mock
	private HealthIndicator one;

	@Mock
	private HealthIndicator two;

	@Mock
	private ThreadPoolTaskExecutor executor;

	private ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(2);

	@Before
	@SuppressWarnings("unchecked")
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health())
				.willReturn(new Health.Builder().unknown().withDetail("1", "1").build());
		given(this.two.health())
				.willReturn(new Health.Builder().unknown().withDetail("2", "2").build());
		given(this.executor.getThreadPoolExecutor()).willReturn(this.threadPoolExecutor);
		given(this.executor.submit(isA(Callable.class)))
				.will((InvocationOnMock invocation) -> this.threadPoolExecutor
						.submit((Callable<Health>) invocation.getArgument(0)));

		this.healthAggregator = new OrderedHealthAggregator();
	}

	@Test
	public void createWithIndicators() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		ConcurrentCompositeHealthIndicator composite = new ConcurrentCompositeHealthIndicator(
				this.healthAggregator, indicators, this.executor);
		Health result = composite.health();
		assertThat(result.getDetails()).hasSize(2);
		assertThat(result.getDetails()).containsEntry("one",
				new Health.Builder().unknown().withDetail("1", "1").build());
		assertThat(result.getDetails()).containsEntry("two",
				new Health.Builder().unknown().withDetail("2", "2").build());
	}

	@Test
	public void testSerialization() throws Exception {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("db1", this.one);
		indicators.put("db2", this.two);
		ConcurrentCompositeHealthIndicator innerComposite = new ConcurrentCompositeHealthIndicator(
				this.healthAggregator, indicators, this.executor);
		ConcurrentCompositeHealthIndicator composite = new ConcurrentCompositeHealthIndicator(
				this.healthAggregator, Collections.singletonMap("db", innerComposite),
				this.executor);
		Health result = composite.health();
		ObjectMapper mapper = new ObjectMapper();
		assertThat(mapper.writeValueAsString(result)).isEqualTo(
				"{\"status\":\"UNKNOWN\",\"details\":{\"db\":{\"status\":\"UNKNOWN\""
						+ ",\"details\":{\"db1\":{\"status\":\"UNKNOWN\",\"details\""
						+ ":{\"1\":\"1\"}},\"db2\":{\"status\":\"UNKNOWN\",\"details\""
						+ ":{\"2\":\"2\"}}}}}}");
	}

	@Test
	public void testWithTimeout() throws Exception {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		ConcurrentCompositeHealthIndicator innerComposite = new ConcurrentCompositeHealthIndicator(
				this.healthAggregator, indicators, this.executor);
		ConcurrentCompositeHealthIndicator composite = new ConcurrentCompositeHealthIndicator(
				this.healthAggregator, Collections.singletonMap("db", innerComposite),
				this.executor, Duration.ZERO);

		Health result = composite.health();
		assertThat(result.getDetails()).hasSize(1);
		assertThat(result.getDetails()).containsEntry("db",
				new Health.Builder().down().withException(
						new IllegalStateException("Health check timed out after PT0S"))
						.build());

	}

}
