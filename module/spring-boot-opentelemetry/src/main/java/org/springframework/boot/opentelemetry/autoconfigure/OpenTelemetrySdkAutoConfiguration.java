/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the OpenTelemetry SDK.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ OpenTelemetry.class, OpenTelemetrySdk.class })
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public final class OpenTelemetrySdkAutoConfiguration {

	OpenTelemetrySdkAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean(OpenTelemetry.class)
	@ConditionalOnEnabledOpenTelemetry
	OpenTelemetrySdk openTelemetrySdk(ObjectProvider<SdkTracerProvider> openTelemetrySdkTracerProvider,
			ObjectProvider<ContextPropagators> openTelemetryContextPropagators,
			ObjectProvider<SdkLoggerProvider> openTelemetrySdkLoggerProvider,
			ObjectProvider<SdkMeterProvider> openTelemetrySdkMeterProvider) {
		OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
		openTelemetrySdkTracerProvider.ifAvailable(builder::setTracerProvider);
		openTelemetryContextPropagators.ifAvailable(builder::setPropagators);
		openTelemetrySdkLoggerProvider.ifAvailable(builder::setLoggerProvider);
		openTelemetrySdkMeterProvider.ifAvailable(builder::setMeterProvider);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean(OpenTelemetry.class)
	@Conditional(OnDisabledOpenTelemetryCondition.class)
	OpenTelemetrySdk disabledOpenTelemetrySdk(ObjectProvider<ContextPropagators> openTelemetryContextPropagators) {
		OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
		openTelemetryContextPropagators.ifAvailable(builder::setPropagators);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	Resource openTelemetryResource(Environment environment, OpenTelemetryProperties properties) {
		return Resource.getDefault().merge(toResource(environment, properties));
	}

	private Resource toResource(Environment environment, OpenTelemetryProperties properties) {
		ResourceBuilder builder = Resource.builder();
		new OpenTelemetryResourceAttributes(environment, properties.getResourceAttributes()).applyTo(builder::put);
		return builder.build();
	}

	static class OnDisabledOpenTelemetryCondition extends NoneNestedConditions {

		OnDisabledOpenTelemetryCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnEnabledOpenTelemetry
		static class EnabledCondition {

		}

	}

}
