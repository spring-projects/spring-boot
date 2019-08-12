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

package org.springframework.boot.actuate.autoconfigure.health;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HealthEndpointAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class HealthEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(HealthIndicatorAutoConfiguration.class, HealthEndpointAutoConfiguration.class));

	@Test
	void healthEndpointShowDetailsDefault() {
		this.contextRunner.withBean(ReactiveHealthIndicator.class, this::reactiveHealthIndicator).run((context) -> {
			ReactiveHealthIndicator indicator = context.getBean("reactiveHealthIndicator",
					ReactiveHealthIndicator.class);
			verify(indicator, never()).health();
			Health health = context.getBean(HealthEndpoint.class).health();
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).isNotEmpty();
			verify(indicator, times(1)).health();
		});
	}

	@Test
	void healthEndpointAdaptReactiveHealthIndicator() {
		this.contextRunner.withPropertyValues("management.endpoint.health.show-details=always")
				.withBean(ReactiveHealthIndicator.class, this::reactiveHealthIndicator).run((context) -> {
					ReactiveHealthIndicator indicator = context.getBean("reactiveHealthIndicator",
							ReactiveHealthIndicator.class);
					verify(indicator, never()).health();
					Health health = context.getBean(HealthEndpoint.class).health();
					assertThat(health.getStatus()).isEqualTo(Status.UP);
					assertThat(health.getDetails()).containsOnlyKeys("reactive");
					verify(indicator, times(1)).health();
				});
	}

	@Test
	void healthEndpointMergeRegularAndReactive() {
		this.contextRunner.withPropertyValues("management.endpoint.health.show-details=always")
				.withBean("simpleHealthIndicator", HealthIndicator.class, this::simpleHealthIndicator)
				.withBean("reactiveHealthIndicator", ReactiveHealthIndicator.class, this::reactiveHealthIndicator)
				.run((context) -> {
					HealthIndicator indicator = context.getBean("simpleHealthIndicator", HealthIndicator.class);
					ReactiveHealthIndicator reactiveHealthIndicator = context.getBean("reactiveHealthIndicator",
							ReactiveHealthIndicator.class);
					verify(indicator, never()).health();
					verify(reactiveHealthIndicator, never()).health();
					Health health = context.getBean(HealthEndpoint.class).health();
					assertThat(health.getStatus()).isEqualTo(Status.UP);
					assertThat(health.getDetails()).containsOnlyKeys("simple", "reactive");
					verify(indicator, times(1)).health();
					verify(reactiveHealthIndicator, times(1)).health();
				});
	}

	private HealthIndicator simpleHealthIndicator() {
		HealthIndicator mock = mock(HealthIndicator.class);
		given(mock.health()).willReturn(Health.status(Status.UP).build());
		return mock;
	}

	private ReactiveHealthIndicator reactiveHealthIndicator() {
		ReactiveHealthIndicator mock = mock(ReactiveHealthIndicator.class);
		given(mock.health()).willReturn(Mono.just(Health.status(Status.UP).build()));
		return mock;
	}

}
