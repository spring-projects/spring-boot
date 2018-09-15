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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.List;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class MetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class));

	@Test
	public void autoConfiguresAClock() {
		this.contextRunner
				.run((context) -> assertThat(context).hasSingleBean(Clock.class));
	}

	@Test
	public void allowsACustomClockToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClockConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasBean("customClock"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void configuresMeterRegistries() {
		this.contextRunner.withUserConfiguration(MeterRegistryConfiguration.class)
				.run((context) -> {
					MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
					List<MeterFilter> filters = (List<MeterFilter>) ReflectionTestUtils
							.getField(meterRegistry, "filters");
					assertThat(filters).isNotEmpty();
					verify((MeterBinder) context.getBean("meterBinder"))
							.bindTo(meterRegistry);
					verify(context.getBean(MeterRegistryCustomizer.class))
							.customize(meterRegistry);
				});
	}

	@Configuration
	static class CustomClockConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration
	static class MeterRegistryConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
			return spy(meterRegistry);
		}

		@Bean
		@SuppressWarnings("rawtypes")
		MeterRegistryCustomizer meterRegistryCustomizer() {
			return mock(MeterRegistryCustomizer.class);
		}

		@Bean
		MeterBinder meterBinder() {
			return mock(MeterBinder.class);
		}

	}

}
