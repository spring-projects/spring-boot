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

package org.springframework.boot.actuate.autoconfigure.metrics.export.influx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Influx.
 *
 * @author Jon Schneider
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(InfluxMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("influx")
@EnableConfigurationProperties(InfluxProperties.class)
public class InfluxMetricsExportAutoConfiguration {

	private final InfluxProperties properties;

	/**
     * Constructs a new instance of InfluxMetricsExportAutoConfiguration with the specified InfluxProperties.
     * 
     * @param properties the InfluxProperties to be used for configuration
     */
    public InfluxMetricsExportAutoConfiguration(InfluxProperties properties) {
		this.properties = properties;
	}

	/**
     * Creates a new instance of InfluxConfig if no bean of type InfluxConfig is already present in the application context.
     * This method is annotated with @Bean and @ConditionalOnMissingBean, which ensures that this bean is only created if no other bean of the same type is already present.
     * The method returns an instance of InfluxPropertiesConfigAdapter, which is a configuration adapter for InfluxDB properties.
     * 
     * @return the InfluxConfig bean
     */
    @Bean
	@ConditionalOnMissingBean
	public InfluxConfig influxConfig() {
		return new InfluxPropertiesConfigAdapter(this.properties);
	}

	/**
     * Creates an instance of InfluxMeterRegistry if no other bean of the same type is present.
     * 
     * @param influxConfig the configuration for InfluxDB
     * @param clock the clock used for measuring time
     * @return the created InfluxMeterRegistry instance
     */
    @Bean
	@ConditionalOnMissingBean
	public InfluxMeterRegistry influxMeterRegistry(InfluxConfig influxConfig, Clock clock) {
		return InfluxMeterRegistry.builder(influxConfig)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();

	}

}
