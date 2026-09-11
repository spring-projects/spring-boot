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

package org.springframework.boot.micrometer.metrics.autoconfigure;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadMeterConventions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.system.SystemMetricsAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for semantic conventions for metrics
 * and observations.
 *
 * @since 4.1.0
 */
@AutoConfiguration(before = { JvmMetricsAutoConfiguration.class, SystemMetricsAutoConfiguration.class })
@EnableConfigurationProperties(ObservationProperties.class)
public final class SemanticConventionAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.observations", name = "conventions", havingValue = "micrometer",
			matchIfMissing = true)
	static class MicrometerSemanticConventionConfiguration {

		@Bean
		@ConditionalOnMissingBean(JvmMemoryMeterConventions.class)
		MicrometerJvmMemoryMeterConventions micrometerJvmMemoryMeterConventions() {
			return new MicrometerJvmMemoryMeterConventions();
		}

		@Bean
		@ConditionalOnMissingBean(JvmClassLoadingMeterConventions.class)
		MicrometerJvmClassLoadingMeterConventions micrometerJvmClassLoadingMeterConventions() {
			return new MicrometerJvmClassLoadingMeterConventions();
		}

		@Bean
		@ConditionalOnMissingBean(JvmCpuMeterConventions.class)
		MicrometerJvmCpuMeterConventions micrometerJvmCpuMeterConventions() {
			return new MicrometerJvmCpuMeterConventions(Tags.empty());
		}

		@Bean
		@ConditionalOnMissingBean(JvmThreadMeterConventions.class)
		MicrometerJvmThreadMeterConventions micrometerJvmThreadMeterConventions() {
			return new MicrometerJvmThreadMeterConventions(Tags.empty());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.observations", name = "conventions", havingValue = "opentelemetry")
	static class OpenTelemetrySemanticConventionConfiguration {

		@Bean
		@ConditionalOnMissingBean(JvmMemoryMeterConventions.class)
		OpenTelemetryJvmMemoryMeterConventions openTelemetryJvmMemoryMeterConventions() {
			return new OpenTelemetryJvmMemoryMeterConventions(Tags.empty());
		}

		@Bean
		@ConditionalOnMissingBean(JvmClassLoadingMeterConventions.class)
		OpenTelemetryJvmClassLoadingMeterConventions openTelemetryJvmClassLoadingMeterConventions() {
			return new OpenTelemetryJvmClassLoadingMeterConventions();
		}

		@Bean
		@ConditionalOnMissingBean(JvmCpuMeterConventions.class)
		OpenTelemetryJvmCpuMeterConventions openTelemetryJvmCpuMeterConventions() {
			return new OpenTelemetryJvmCpuMeterConventions(Tags.empty());
		}

		@Bean
		@ConditionalOnMissingBean(JvmThreadMeterConventions.class)
		OpenTelemetryJvmThreadMeterConventions openTelemetryJvmThreadMeterConventions() {
			return new OpenTelemetryJvmThreadMeterConventions(Tags.empty());
		}

	}

}
