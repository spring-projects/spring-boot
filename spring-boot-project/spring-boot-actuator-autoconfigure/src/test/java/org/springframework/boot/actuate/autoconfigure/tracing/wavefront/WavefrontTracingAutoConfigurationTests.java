/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.wavefront;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.reporter.wavefront.SpanMetrics;
import io.micrometer.tracing.reporter.wavefront.WavefrontBraveSpanHandler;
import io.micrometer.tracing.reporter.wavefront.WavefrontOtelSpanHandler;
import io.micrometer.tracing.reporter.wavefront.WavefrontSpanHandler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WavefrontTracingAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class WavefrontTracingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WavefrontTracingAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(WavefrontSenderConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ApplicationTags.class);
			assertThat(context).hasSingleBean(WavefrontSpanHandler.class);
			assertThat(context).hasSingleBean(SpanMetrics.class);
			assertThat(context).hasSingleBean(WavefrontBraveSpanHandler.class);
			assertThat(context).hasSingleBean(WavefrontOtelSpanHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfWavefrontSenderIsMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ApplicationTags.class);
			assertThat(context).doesNotHaveBean(WavefrontSpanHandler.class);
			assertThat(context).doesNotHaveBean(SpanMetrics.class);
			assertThat(context).doesNotHaveBean(WavefrontBraveSpanHandler.class);
			assertThat(context).doesNotHaveBean(WavefrontOtelSpanHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerReporterWavefrontIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing.reporter.wavefront"))
				.withUserConfiguration(WavefrontSenderConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(WavefrontSpanHandler.class);
					assertThat(context).doesNotHaveBean(SpanMetrics.class);
					assertThat(context).doesNotHaveBean(WavefrontBraveSpanHandler.class);
					assertThat(context).doesNotHaveBean(WavefrontOtelSpanHandler.class);
				});
	}

	@Test
	void shouldNotSupplyBeansIfTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false")
				.withUserConfiguration(WavefrontSenderConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(ApplicationTags.class);
					assertThat(context).doesNotHaveBean(WavefrontSpanHandler.class);
					assertThat(context).doesNotHaveBean(SpanMetrics.class);
					assertThat(context).doesNotHaveBean(WavefrontBraveSpanHandler.class);
					assertThat(context).doesNotHaveBean(WavefrontOtelSpanHandler.class);
				});
	}

	@Test
	void shouldSupplyMeterRegistrySpanMetricsIfMeterRegistryIsAvailable() {
		this.contextRunner.withUserConfiguration(WavefrontSenderConfiguration.class, MeterRegistryConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(SpanMetrics.class);
					assertThat(context).hasSingleBean(MeterRegistrySpanMetrics.class);
				});
	}

	@Test
	void shouldNotSupplyWavefrontBraveSpanHandlerIfBraveIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("brave"))
				.withUserConfiguration(WavefrontSenderConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(WavefrontBraveSpanHandler.class));
	}

	@Test
	void shouldNotSupplyWavefrontOtelSpanHandlerIfOtelIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.sdk.trace"))
				.withUserConfiguration(WavefrontSenderConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(WavefrontOtelSpanHandler.class));
	}

	@Test
	void shouldHaveADefaultApplicationName() {
		this.contextRunner.withUserConfiguration(WavefrontSenderConfiguration.class).run((context) -> {
			ApplicationTags applicationTags = context.getBean(ApplicationTags.class);
			assertThat(applicationTags.getApplication()).isEqualTo("application");
		});
	}

	@Test
	void shouldHonorConfigProperties() {
		this.contextRunner.withUserConfiguration(WavefrontSenderConfiguration.class)
				.withPropertyValues("spring.application.name=super-application",
						"management.wavefront.tracing.service-name=super-service")
				.run((context) -> {
					ApplicationTags applicationTags = context.getBean(ApplicationTags.class);
					assertThat(applicationTags.getApplication()).isEqualTo("super-application");
					assertThat(applicationTags.getService()).isEqualTo("super-service");
				});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(WavefrontSenderConfiguration.class, CustomConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("customApplicationTags");
					assertThat(context).hasSingleBean(ApplicationTags.class);
					assertThat(context).hasBean("customWavefrontSpanHandler");
					assertThat(context).hasSingleBean(WavefrontSpanHandler.class);
					assertThat(context).hasBean("customSpanMetrics");
					assertThat(context).hasSingleBean(SpanMetrics.class);
					assertThat(context).hasBean("customWavefrontBraveSpanHandler");
					assertThat(context).hasSingleBean(WavefrontBraveSpanHandler.class);
					assertThat(context).hasBean("customWavefrontOtelSpanHandler");
					assertThat(context).hasSingleBean(WavefrontOtelSpanHandler.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		ApplicationTags customApplicationTags() {
			return mock(ApplicationTags.class);
		}

		@Bean
		WavefrontSpanHandler customWavefrontSpanHandler() {
			return mock(WavefrontSpanHandler.class);
		}

		@Bean
		SpanMetrics customSpanMetrics() {
			return mock(SpanMetrics.class);
		}

		@Bean
		WavefrontBraveSpanHandler customWavefrontBraveSpanHandler() {
			return mock(WavefrontBraveSpanHandler.class);
		}

		@Bean
		WavefrontOtelSpanHandler customWavefrontOtelSpanHandler() {
			return mock(WavefrontOtelSpanHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class WavefrontSenderConfiguration {

		@Bean
		WavefrontSender wavefrontSender() {
			return mock(WavefrontSender.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class MeterRegistryConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}
