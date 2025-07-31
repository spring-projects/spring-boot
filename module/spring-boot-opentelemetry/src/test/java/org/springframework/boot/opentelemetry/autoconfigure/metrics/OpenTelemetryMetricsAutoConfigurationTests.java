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

package org.springframework.boot.opentelemetry.autoconfigure.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryMetricsAutoConfiguration}.
 *
 * @author Thomas Vitale
 */
class OpenTelemetryMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OpenTelemetrySdkAutoConfiguration.class,
				OpenTelemetryMetricsAutoConfiguration.class));

	@Test
	void whenSdkMeterProviderIsNotOnClasspathDoesNotProvideBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SdkMeterProvider.class)).run((context) -> {
			assertThat(context).doesNotHaveBean(SdkMeterProvider.class);
			assertThat(context).doesNotHaveBean(CardinalityLimitSelector.class);
			assertThat(context).doesNotHaveBean(ExemplarFilter.class);
			assertThat(context).doesNotHaveBean(Meter.class);
		});
	}

	@Test
	void meterProviderAvailableWithDefaultConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(SdkMeterProvider.class);
			assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
			assertThat(context).hasSingleBean(ExemplarFilter.class);
			assertThat(context).hasSingleBean(Meter.class);
		});
	}

	@Test
	void cardinalityLimitSelectorConfigurationApplied() {
		this.contextRunner.withPropertyValues("management.opentelemetry.metrics.cardinality-limit=200")
			.run((context) -> {
				CardinalityLimitSelector cardinalityLimitSelector = context.getBean(CardinalityLimitSelector.class);
				assertThat(cardinalityLimitSelector.getCardinalityLimit(InstrumentType.COUNTER)).isEqualTo(200);
			});
	}

	@Test
	void exemplarFilterConfigurationApplied() {
		this.contextRunner.withPropertyValues("management.opentelemetry.metrics.exemplars.filter=always-on")
			.run((context) -> {
				ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
				assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOn());
			});

		this.contextRunner.withPropertyValues("management.opentelemetry.metrics.exemplars.filter=always-off")
			.run((context) -> {
				ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
				assertThat(exemplarFilter).isEqualTo(ExemplarFilter.alwaysOff());
			});

		this.contextRunner.withPropertyValues("management.opentelemetry.metrics.exemplars.filter=trace-based")
			.run((context) -> {
				ExemplarFilter exemplarFilter = context.getBean(ExemplarFilter.class);
				assertThat(exemplarFilter).isEqualTo(ExemplarFilter.traceBased());
			});
	}

	@Test
	void customCardinalityLimitSelectorAvailable() {
		this.contextRunner.withUserConfiguration(CustomCardinalityLimitSelectorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CardinalityLimitSelector.class);
			assertThat(context.getBean(CardinalityLimitSelector.class))
				.isSameAs(context.getBean(CustomCardinalityLimitSelectorConfiguration.class)
					.customCardinalityLimitSelector());
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCardinalityLimitSelectorConfiguration {

		private final CardinalityLimitSelector customCardinalityLimitSelector = mock(CardinalityLimitSelector.class);

		@Bean
		CardinalityLimitSelector customCardinalityLimitSelector() {
			return this.customCardinalityLimitSelector;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMeterBuilderCustomizerConfiguration {

		private final SdkMeterProviderBuilderCustomizer customMeterBuilderCustomizer = mock(
				SdkMeterProviderBuilderCustomizer.class);

		@Bean
		SdkMeterProviderBuilderCustomizer customMetricBuilderCustomizer() {
			return this.customMeterBuilderCustomizer;
		}

	}

}
