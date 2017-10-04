/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import io.micrometer.core.instrument.Clock;
import io.micrometer.datadog.DatadogConfig;
import io.micrometer.datadog.DatadogMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.export.MetricsExporter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.StringToDurationConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for exporting metrics to Datadog.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(DatadogMeterRegistry.class)
@ConditionalOnProperty("spring.metrics.datadog.api-key")
@Import(StringToDurationConverter.class)
@EnableConfigurationProperties(DatadogProperties.class)
public class DatadogExportConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DatadogConfig datadogConfig(DatadogProperties datadogProperties) {
		return new DatadogPropertiesConfigAdapter(datadogProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.metrics.datadog.enabled", matchIfMissing = true)
	public MetricsExporter datadogExporter(DatadogConfig datadogConfig, Clock clock) {
		return () -> new DatadogMeterRegistry(datadogConfig, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock clock() {
		return Clock.SYSTEM;
	}

}
