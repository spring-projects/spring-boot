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

package org.springframework.boot.opentelemetry.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class OpenTelemetrySdkAutoConfiguration {

	OpenTelemetrySdkAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean(OpenTelemetry.class)
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
	@ConditionalOnMissingBean
	Resource openTelemetryResource(Environment environment, OpenTelemetryProperties properties) {
		return Resource.getDefault().merge(toResource(environment, properties));
	}

	private Resource toResource(Environment environment, OpenTelemetryProperties properties) {
		ResourceBuilder builder = Resource.builder();
		new OpenTelemetryResourceAttributes(environment, properties.getResourceAttributes()).applyTo(builder::put);
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SdkLoggerProvider.class)
	static class LoggerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		BatchLogRecordProcessor openTelemetryBatchLogRecordProcessor(
				ObjectProvider<LogRecordExporter> logRecordExporters) {
			LogRecordExporter exporter = LogRecordExporter.composite(logRecordExporters.orderedStream().toList());
			return BatchLogRecordProcessor.builder(exporter).build();
		}

		@Bean
		@ConditionalOnMissingBean
		SdkLoggerProvider openTelemetrySdkLoggerProvider(Resource openTelemetryResource,
				ObjectProvider<LogRecordProcessor> logRecordProcessors,
				ObjectProvider<SdkLoggerProviderBuilderCustomizer> customizers) {
			SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder();
			builder.setResource(openTelemetryResource);
			logRecordProcessors.orderedStream().forEach(builder::addLogRecordProcessor);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		}

	}

}
