/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.otlp;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to OTLP.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(OtlpMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("otlp")
@EnableConfigurationProperties({ OtlpProperties.class, OpenTelemetryProperties.class })
public class OtlpMetricsExportAutoConfiguration {

	private final OtlpProperties properties;

	/**
	 * Constructs a new instance of OtlpMetricsExportAutoConfiguration with the specified
	 * OtlpProperties.
	 * @param properties the OtlpProperties to be used for configuration
	 */
	OtlpMetricsExportAutoConfiguration(OtlpProperties properties) {
		this.properties = properties;
	}

	/**
	 * Creates an instance of OtlpMetricsConnectionDetails if no other bean of the same
	 * type is present. Uses the properties provided to create a new instance of
	 * PropertiesOtlpMetricsConnectionDetails.
	 * @return the created instance of OtlpMetricsConnectionDetails
	 */
	@Bean
	@ConditionalOnMissingBean
	OtlpMetricsConnectionDetails otlpMetricsConnectionDetails() {
		return new PropertiesOtlpMetricsConnectionDetails(this.properties);
	}

	/**
	 * Creates an instance of {@link OtlpConfig} based on the provided properties,
	 * connection details, and environment. This method is annotated with
	 * {@link ConditionalOnMissingBean} to ensure that it is only executed if there is no
	 * existing bean of type {@link OtlpConfig}.
	 * @param openTelemetryProperties The properties for OpenTelemetry.
	 * @param connectionDetails The connection details for OTLP metrics.
	 * @param environment The environment.
	 * @return An instance of {@link OtlpConfig} based on the provided properties,
	 * connection details, and environment.
	 */
	@Bean
	@ConditionalOnMissingBean
	OtlpConfig otlpConfig(OpenTelemetryProperties openTelemetryProperties,
			OtlpMetricsConnectionDetails connectionDetails, Environment environment) {
		return new OtlpPropertiesConfigAdapter(this.properties, openTelemetryProperties, connectionDetails,
				environment);
	}

	/**
	 * Creates an instance of OtlpMeterRegistry if no other bean of the same type is
	 * present.
	 * @param otlpConfig The configuration for the OTLP exporter.
	 * @param clock The clock used for measuring time.
	 * @return An instance of OtlpMeterRegistry.
	 */
	@Bean
	@ConditionalOnMissingBean
	public OtlpMeterRegistry otlpMeterRegistry(OtlpConfig otlpConfig, Clock clock) {
		return new OtlpMeterRegistry(otlpConfig, clock);
	}

	/**
	 * Adapts {@link OtlpProperties} to {@link OtlpMetricsConnectionDetails}.
	 */
	static class PropertiesOtlpMetricsConnectionDetails implements OtlpMetricsConnectionDetails {

		private final OtlpProperties properties;

		/**
		 * Constructs a new instance of PropertiesOtlpMetricsConnectionDetails with the
		 * specified properties.
		 * @param properties the OTLP properties to be used for the connection details
		 */
		PropertiesOtlpMetricsConnectionDetails(OtlpProperties properties) {
			this.properties = properties;
		}

		/**
		 * Returns the URL of the connection details.
		 * @return the URL of the connection details
		 */
		@Override
		public String getUrl() {
			return this.properties.getUrl();
		}

	}

}
