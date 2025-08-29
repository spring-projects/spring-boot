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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry Metrics.
 *
 * @author Thomas Vitale
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(SdkMeterProvider.class)
@EnableConfigurationProperties(OpenTelemetryMetricsProperties.class)
public final class OpenTelemetryMetricsAutoConfiguration {

	static final String INSTRUMENTATION_SCOPE_NAME = "org.springframework.boot";

	@Bean
	@ConditionalOnMissingBean
	SdkMeterProvider meterProvider(Clock clock, ExemplarFilter exemplarFilter,
			OpenTelemetryMetricsProperties properties, Resource resource,
			ObjectProvider<SdkMeterProviderBuilderCustomizer> customizers) {
		SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setClock(clock).setResource(resource);
		if (properties.getExemplars().isEnabled()) {
			SdkMeterProviderUtil.setExemplarFilter(builder, exemplarFilter);
		}
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	CardinalityLimitSelector cardinalityLimitSelector(OpenTelemetryMetricsProperties properties) {
		return (instrumentType) -> properties.getCardinalityLimit();
	}

	@Bean
	@ConditionalOnMissingBean
	ExemplarFilter exemplarFilter(OpenTelemetryMetricsProperties properties) {
		return switch (properties.getExemplars().getFilter()) {
			case ALWAYS_ON -> ExemplarFilter.alwaysOn();
			case ALWAYS_OFF -> ExemplarFilter.alwaysOff();
			case TRACE_BASED -> ExemplarFilter.traceBased();
		};
	}

	@Bean
	@ConditionalOnMissingBean
	Meter meter(OpenTelemetry openTelemetry) {
		return openTelemetry.getMeter(INSTRUMENTATION_SCOPE_NAME);
	}

}
