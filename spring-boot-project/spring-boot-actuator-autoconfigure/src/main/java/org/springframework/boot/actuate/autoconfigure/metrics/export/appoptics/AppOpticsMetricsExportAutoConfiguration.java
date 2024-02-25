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

package org.springframework.boot.actuate.autoconfigure.metrics.export.appoptics;

import io.micrometer.appoptics.AppOpticsConfig;
import io.micrometer.appoptics.AppOpticsMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to AppOptics.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(AppOpticsMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("appoptics")
@EnableConfigurationProperties(AppOpticsProperties.class)
public class AppOpticsMetricsExportAutoConfiguration {

	private final AppOpticsProperties properties;

	/**
     * Constructs a new instance of the {@code AppOpticsMetricsExportAutoConfiguration} class with the specified {@code AppOpticsProperties}.
     *
     * @param properties the {@code AppOpticsProperties} object to be used for configuration
     */
    public AppOpticsMetricsExportAutoConfiguration(AppOpticsProperties properties) {
		this.properties = properties;
	}

	/**
     * Creates an instance of {@link AppOpticsConfig} if no other bean of the same type is present.
     * This method is annotated with {@link ConditionalOnMissingBean} to ensure that it is only executed
     * if there is no other bean of type {@link AppOpticsConfig} already defined.
     * The created instance is of type {@link AppOpticsPropertiesConfigAdapter} and is initialized with
     * the properties provided by {@code this.properties}.
     *
     * @return an instance of {@link AppOpticsConfig} configured with the properties from {@code this.properties}
     */
    @Bean
	@ConditionalOnMissingBean
	public AppOpticsConfig appOpticsConfig() {
		return new AppOpticsPropertiesConfigAdapter(this.properties);
	}

	/**
     * Creates a new instance of AppOpticsMeterRegistry if no other bean of the same type is present.
     * 
     * @param config The configuration for the AppOpticsMeterRegistry.
     * @param clock The clock used for measuring time.
     * @return The created AppOpticsMeterRegistry instance.
     */
    @Bean
	@ConditionalOnMissingBean
	public AppOpticsMeterRegistry appOpticsMeterRegistry(AppOpticsConfig config, Clock clock) {
		return AppOpticsMeterRegistry.builder(config)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();
	}

}
