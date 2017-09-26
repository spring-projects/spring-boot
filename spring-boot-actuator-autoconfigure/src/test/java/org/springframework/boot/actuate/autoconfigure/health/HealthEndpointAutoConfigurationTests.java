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

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HealthEndpointAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class HealthEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(HealthEndpointAutoConfiguration.class));

	@Test
	public void healthEndpointAdaptReactiveHealthIndicator() {
		this.contextRunner
				.withUserConfiguration(ReactiveHealthIndicatorConfiguration.class)
				.run((context) -> {
					ReactiveHealthIndicator indicator = context.getBean(
							"reactiveHealthIndicator", ReactiveHealthIndicator.class);
					verify(indicator, times(0)).health();
					Health health = context.getBean(HealthEndpoint.class).health();
					assertThat(health.getStatus()).isEqualTo(Status.UP);
					assertThat(health.getDetails()).containsOnlyKeys("reactive");
					verify(indicator, times(1)).health();
				});
	}

	@Test
	public void healthEndpointMergeRegularAndReactive() {
		this.contextRunner.withUserConfiguration(HealthIndicatorConfiguration.class,
				ReactiveHealthIndicatorConfiguration.class).run((context) -> {
					HealthIndicator indicator = context.getBean("simpleHealthIndicator",
							HealthIndicator.class);
					ReactiveHealthIndicator reactiveHealthIndicator = context.getBean(
							"reactiveHealthIndicator", ReactiveHealthIndicator.class);
					verify(indicator, times(0)).health();
					verify(reactiveHealthIndicator, times(0)).health();
					Health health = context.getBean(HealthEndpoint.class).health();
					assertThat(health.getStatus()).isEqualTo(Status.UP);
					assertThat(health.getDetails()).containsOnlyKeys("simple",
							"reactive");
					verify(indicator, times(1)).health();
					verify(reactiveHealthIndicator, times(1)).health();
				});
	}

	@Test
	public void runShouldHaveStatusEndpointBeanEvenIfDefaultIsDisabled() {
		this.contextRunner.withPropertyValues("endpoints.default.enabled:false").run(
				(context) -> assertThat(context).hasSingleBean(StatusEndpoint.class));
	}

	@Test
	public void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean()
			throws Exception {
		this.contextRunner.withPropertyValues("endpoints.status.enabled:false").run(
				(context) -> assertThat(context).doesNotHaveBean(StatusEndpoint.class));
	}

	@Configuration
	static class HealthIndicatorConfiguration {

		@Bean
		public HealthIndicator simpleHealthIndicator() {
			HealthIndicator mock = mock(HealthIndicator.class);
			given(mock.health()).willReturn(Health.status(Status.UP).build());
			return mock;
		}

	}

	@Configuration
	static class ReactiveHealthIndicatorConfiguration {

		@Bean
		public ReactiveHealthIndicator reactiveHealthIndicator() {
			ReactiveHealthIndicator mock = mock(ReactiveHealthIndicator.class);
			given(mock.health()).willReturn(Mono.just(Health.status(Status.UP).build()));
			return mock;
		}

	}
}
