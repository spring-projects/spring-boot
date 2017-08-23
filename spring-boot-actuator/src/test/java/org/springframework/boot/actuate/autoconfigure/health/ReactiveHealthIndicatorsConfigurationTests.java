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

package org.springframework.boot.actuate.autoconfigure.health;

import org.assertj.core.api.Condition;
import org.junit.Test;

import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.RedisReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHealthIndicatorsConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class ReactiveHealthIndicatorsConfigurationTests {

	public final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(HealthIndicatorAutoConfiguration.class));

	@Test
	public void redisHealthIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(
						RedisAutoConfiguration.class))
				.withPropertyValues("management.health.diskspace.enabled:false")
				.run(hasSingleReactiveHealthIndicator(RedisReactiveHealthIndicator.class));
	}

	private ContextConsumer<AssertableApplicationContext> hasSingleReactiveHealthIndicator(
			Class<? extends ReactiveHealthIndicator> type) {
		return (context) -> assertThat(context).getBeans(ReactiveHealthIndicator.class)
				.hasSize(1)
				.hasValueSatisfying(
						new Condition<>((indicator) -> indicator.getClass().equals(type),
								"Wrong indicator type"));
	}

}
