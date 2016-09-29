/*
 * Copyright 2012-2014 the original author or authors.
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

import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;

/**
 * Tests for {@link CompositeHealthIndicator}
 *
 * @author Tyler J. Frederick
 */
public class CompositeHealthIndicatorTests {

	@Test
	public void multipleHealthIndicators() throws Exception {
		Map<String, HealthIndicator<?>> healthIndicators =
				new HashMap<String, HealthIndicator<?>>();

		healthIndicators.put("one", newHealthIndicatorWithResult("one-ok"));
		healthIndicators.put("two", newHealthIndicatorWithResult("two-ok"));

		CompositeHealthIndicator healthIndicator =
				new CompositeHealthIndicator(healthIndicators);

		Map<String, Object> resultMap = healthIndicator.health();
		assertThat(resultMap, IsMapContaining.<String, Object>hasEntry("one", "one-ok"));
		assertThat(resultMap, IsMapContaining.<String, Object>hasEntry("two", "two-ok"));
	}

	private HealthIndicator<?> newHealthIndicatorWithResult(final String result) {
		return new HealthIndicator<Object>() {
			@Override
			public Object health() {
				return result;
			}
		};
	}
}
