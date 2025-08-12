/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.metrics.autoconfigure;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.CountedMeterTagAnnotationHandler;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.weaver.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsAspectsAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class MetricsAspectsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
		.withPropertyValues("management.observations.annotations.enabled=true")
		.withConfiguration(AutoConfigurations.of(MetricsAspectsAutoConfiguration.class));

	@Test
	void shouldNotConfigureAspectsByDefault() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MetricsAspectsAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(CountedAspect.class);
				assertThat(context).doesNotHaveBean(TimedAspect.class);
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
	void shouldAutoConfigureMeterTagAnnotationHandlerWhenValueExpressionResolverIsAvailable() {
		this.contextRunner.withBean(ValueExpressionResolver.class, () -> mock(ValueExpressionResolver.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(TimedAspect.class).hasSingleBean(MeterTagAnnotationHandler.class);
				assertThat(context.getBean(TimedAspect.class)).extracting("meterTagAnnotationHandler")
					.isSameAs(context.getBean(MeterTagAnnotationHandler.class));
			});
	}

	@Test
	void shouldUseUserDefinedMeterTagAnnotationHandler() {
		this.contextRunner
			.withBean("customMeterTagAnnotationHandler", MeterTagAnnotationHandler.class,
					() -> new MeterTagAnnotationHandler(null, null))
			.run((context) -> {
				assertThat(context).hasSingleBean(TimedAspect.class).hasSingleBean(MeterTagAnnotationHandler.class);
				assertThat(context.getBean(TimedAspect.class)).extracting("meterTagAnnotationHandler")
					.isSameAs(context.getBean("customMeterTagAnnotationHandler"));
			});
	}

	@Test
	void shouldAutoConfigureCountedMeterTagAnnotationHandlerWhenValueExpressionResolverIsAvailable() {
		this.contextRunner.withBean(ValueExpressionResolver.class, () -> mock(ValueExpressionResolver.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(CountedAspect.class)
					.hasSingleBean(CountedMeterTagAnnotationHandler.class);
				assertThat(context.getBean(CountedAspect.class)).extracting("meterTagAnnotationHandler")
					.isSameAs(context.getBean(CountedMeterTagAnnotationHandler.class));
			});
	}

	@Test
	void shouldUseUserDefinedCountedMeterTagAnnotationHandler() {
		this.contextRunner
			.withBean("customCountedMeterTagAnnotationHandler", CountedMeterTagAnnotationHandler.class,
					() -> new CountedMeterTagAnnotationHandler(null, null))
			.run((context) -> {
				assertThat(context).hasSingleBean(CountedAspect.class)
					.hasSingleBean(CountedMeterTagAnnotationHandler.class);
				assertThat(context.getBean(CountedAspect.class)).extracting("meterTagAnnotationHandler")
					.isSameAs(context.getBean(CountedMeterTagAnnotationHandler.class));
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

}
