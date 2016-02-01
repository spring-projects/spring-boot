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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AsyncHealthIndicatorRunner}.
 *
 * @author Vedran Pavic
 */
public class AsyncHealthIndicatorRunnerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private HealthIndicator one = mock(HealthIndicator.class);

	private HealthIndicator two = mock(HealthIndicator.class);

	@Before
	public void setUp() {
		given(this.one.health()).willReturn(new Health.Builder().up().build());
		given(this.two.health()).willReturn(new Health.Builder().unknown().build());
	}

	@Test
	public void createAndRun() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize(2);
		taskExecutor.afterPropertiesSet();
		HealthIndicatorRunner healthIndicatorRunner = new AsyncHealthIndicatorRunner(
				taskExecutor);
		Map<String, Health> healths = healthIndicatorRunner.run(indicators);
		assertThat(healths.size()).isEqualTo(2);
		assertThat(healths.get("one").getStatus()).isEqualTo(Status.UP);
		assertThat(healths.get("two").getStatus()).isEqualTo(Status.UNKNOWN);
	}

	@Test
	public void createWithNullTaskExecutor() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TaskExecutor must not be null");
		new AsyncHealthIndicatorRunner(null);
	}

}
