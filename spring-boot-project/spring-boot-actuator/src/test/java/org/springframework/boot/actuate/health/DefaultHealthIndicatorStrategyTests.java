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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultHealthIndicatorStrategy}.
 *
 * @author Dmytro Nosan
 */
class DefaultHealthIndicatorStrategyTests {

	private final HealthIndicator one = mock(HealthIndicator.class);

	private final HealthIndicator two = mock(HealthIndicator.class);

	@BeforeEach
	void setup() {
		given(this.one.health()).willReturn(new Health.Builder().unknown().build());
		given(this.two.health()).willReturn(new Health.Builder().up().build());
	}

	@Test
	void testStrategy() {
		Map<String, HealthIndicator> indicators = new HashMap<>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		Map<String, Health> health = new DefaultHealthIndicatorStrategy().doHealth(indicators);
		assertThat(health).containsOnlyKeys("one", "two");
		assertThat(health).containsEntry("one", new Health.Builder().unknown().build());
		assertThat(health).containsEntry("two", new Health.Builder().up().build());
	}

}
