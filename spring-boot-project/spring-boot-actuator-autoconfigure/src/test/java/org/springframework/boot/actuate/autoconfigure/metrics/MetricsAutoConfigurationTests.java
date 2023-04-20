/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class MetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class));

	@Test
	void autoConfiguresAClock() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Clock.class));
	}

	@Test
	void allowsACustomClockToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClockConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Clock.class).hasBean("customClock"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void configuresMeterRegistries() {
		this.contextRunner.withUserConfiguration(MeterRegistryConfiguration.class).run((context) -> {
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			MeterFilter[] filters = (MeterFilter[]) ReflectionTestUtils.getField(meterRegistry, "filters");
			assertThat(filters).hasSize(3);
			assertThat(filters[0].accept((Meter.Id) null)).isEqualTo(MeterFilterReply.DENY);
			assertThat(filters[1]).isInstanceOf(PropertiesMeterFilter.class);
			assertThat(filters[2].accept((Meter.Id) null)).isEqualTo(MeterFilterReply.ACCEPT);
			then((MeterBinder) context.getBean("meterBinder")).should().bindTo(meterRegistry);
			then(context.getBean(MeterRegistryCustomizer.class)).should().customize(meterRegistry);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClockConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MeterRegistryConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
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

		@Bean
		@Order(1)
		MeterFilter acceptMeterFilter() {
			return MeterFilter.accept();
		}

		@Bean
		@Order(-1)
		MeterFilter denyMeterFilter() {
			return MeterFilter.deny();
		}

	}

}
