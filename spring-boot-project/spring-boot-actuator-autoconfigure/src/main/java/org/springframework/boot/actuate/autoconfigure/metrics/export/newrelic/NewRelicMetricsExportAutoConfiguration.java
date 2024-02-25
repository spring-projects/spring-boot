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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.newrelic.ClientProviderType;
import io.micrometer.newrelic.NewRelicClientProvider;
import io.micrometer.newrelic.NewRelicConfig;
import io.micrometer.newrelic.NewRelicInsightsAgentClientProvider;
import io.micrometer.newrelic.NewRelicInsightsApiClientProvider;
import io.micrometer.newrelic.NewRelicMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to New Relic.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(NewRelicMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("newrelic")
@EnableConfigurationProperties(NewRelicProperties.class)
public class NewRelicMetricsExportAutoConfiguration {

	private final NewRelicProperties properties;

	/**
	 * Constructs a new instance of the NewRelicMetricsExportAutoConfiguration class with
	 * the specified NewRelicProperties.
	 * @param properties the NewRelicProperties object containing the configuration
	 * properties for New Relic metrics export
	 */
	public NewRelicMetricsExportAutoConfiguration(NewRelicProperties properties) {
		this.properties = properties;
	}

	/**
	 * Creates a new instance of {@link NewRelicConfig} if no bean of type
	 * {@link NewRelicConfig} is already present in the application context.
	 * @return the newly created {@link NewRelicConfig} instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public NewRelicConfig newRelicConfig() {
		return new NewRelicPropertiesConfigAdapter(this.properties);
	}

	/**
	 * Creates a new instance of NewRelicClientProvider based on the provided
	 * NewRelicConfig. If the clientProviderType is set to INSIGHTS_AGENT, it returns a
	 * NewRelicInsightsAgentClientProvider initialized with the given NewRelicConfig.
	 * Otherwise, it returns a NewRelicInsightsApiClientProvider initialized with the
	 * given NewRelicConfig and a HttpUrlConnectionSender with the specified
	 * connectTimeout and readTimeout.
	 * @param newRelicConfig the NewRelicConfig object used to initialize the client
	 * provider
	 * @return a NewRelicClientProvider instance based on the provided NewRelicConfig
	 */
	@Bean
	@ConditionalOnMissingBean
	public NewRelicClientProvider newRelicClientProvider(NewRelicConfig newRelicConfig) {
		if (newRelicConfig.clientProviderType() == ClientProviderType.INSIGHTS_AGENT) {
			return new NewRelicInsightsAgentClientProvider(newRelicConfig);
		}
		return new NewRelicInsightsApiClientProvider(newRelicConfig,
				new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()));

	}

	/**
	 * Creates a new instance of NewRelicMeterRegistry if no other bean of the same type
	 * is present.
	 * @param newRelicConfig the configuration for New Relic
	 * @param clock the clock used for measuring time
	 * @param newRelicClientProvider the provider for the New Relic client
	 * @return a new instance of NewRelicMeterRegistry
	 */
	@Bean
	@ConditionalOnMissingBean
	public NewRelicMeterRegistry newRelicMeterRegistry(NewRelicConfig newRelicConfig, Clock clock,
			NewRelicClientProvider newRelicClientProvider) {
		return NewRelicMeterRegistry.builder(newRelicConfig)
			.clock(clock)
			.clientProvider(newRelicClientProvider)
			.build();
	}

}
