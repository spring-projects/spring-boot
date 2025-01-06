/*
 * Copyright 2012-2024 the original author or authors.
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

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.weaver.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsAspectsAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class MetricsAspectsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withPropertyValues("management.observations.annotations.enabled=true")
		.withConfiguration(AutoConfigurations.of(MetricsAspectsAutoConfiguration.class));

	@Test
	void shouldNotConfigureAspectsByDefault() {
		new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MetricsAspectsAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(CountedAspect.class);
				assertThat(context).doesNotHaveBean(TimedAspect.class);
			});
	}

	@Test
	void shouldConfigureAspectsWithLegacyProperty() {
		new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(MetricsAspectsAutoConfiguration.class))
			.withPropertyValues("micrometer.observations.annotations.enabled=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(CountedAspect.class);
				assertThat(context).hasSingleBean(TimedAspect.class);
			});
	}

	@Test
	void shouldConfigureAspects() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CountedAspect.class);
			assertThat(context).hasSingleBean(TimedAspect.class);
		});
	}

	@Test
	void shouldConfigureMeterTagAnnotationHandler() {
		this.contextRunner.withUserConfiguration(MeterTagAnnotationHandlerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CountedAspect.class);
			assertThat(ReflectionTestUtils.getField(context.getBean(TimedAspect.class), "meterTagAnnotationHandler"))
				.isSameAs(context.getBean(MeterTagAnnotationHandler.class));
		});
	}

	@Test
	void shouldNotConfigureAspectsIfMicrometerIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(MeterRegistry.class)).run((context) -> {
			assertThat(context).doesNotHaveBean(CountedAspect.class);
			assertThat(context).doesNotHaveBean(TimedAspect.class);
		});
	}

	@Test
	void shouldNotConfigureAspectsIfAspectjIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Advice.class)).run((context) -> {
			assertThat(context).doesNotHaveBean(CountedAspect.class);
			assertThat(context).doesNotHaveBean(TimedAspect.class);
		});
	}

	@Test
	void shouldNotConfigureAspectsIfMeterRegistryBeanIsMissing() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MetricsAspectsAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(MeterRegistry.class);
				assertThat(context).doesNotHaveBean(CountedAspect.class);
				assertThat(context).doesNotHaveBean(TimedAspect.class);
			});
	}

	@Test
	void shouldBackOffIfAspectBeansExist() {
		this.contextRunner.withUserConfiguration(CustomAspectsConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CountedAspect.class).hasBean("customCountedAspect");
			assertThat(context).hasSingleBean(TimedAspect.class).hasBean("customTimedAspect");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAspectsConfiguration {

		@Bean
		CountedAspect customCountedAspect(MeterRegistry registry) {
			return new CountedAspect(registry);
		}

		@Bean
		TimedAspect customTimedAspect(MeterRegistry registry) {
			return new TimedAspect(registry);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MeterTagAnnotationHandlerConfiguration {

		@Bean
		MeterTagAnnotationHandler meterTagAnnotationHandler() {
			return new MeterTagAnnotationHandler(null, null);
		}

	}

}
