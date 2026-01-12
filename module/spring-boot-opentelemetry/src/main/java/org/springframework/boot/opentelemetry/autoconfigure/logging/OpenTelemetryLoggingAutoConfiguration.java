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

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry logging.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(after = OpenTelemetrySdkAutoConfiguration.class)
@ConditionalOnClass(SdkLoggerProvider.class)
public final class OpenTelemetryLoggingAutoConfiguration {

	OpenTelemetryLoggingAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean
	BatchLogRecordProcessor openTelemetryBatchLogRecordProcessor(ObjectProvider<LogRecordExporter> logRecordExporters) {
		LogRecordExporter exporter = LogRecordExporter.composite(logRecordExporters.orderedStream().toList());
		return BatchLogRecordProcessor.builder(exporter).build();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(Resource.class)
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
