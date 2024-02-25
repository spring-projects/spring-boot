/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.graphite;

import io.micrometer.core.instrument.Clock;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Graphite.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(GraphiteMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("graphite")
@EnableConfigurationProperties(GraphiteProperties.class)
public class GraphiteMetricsExportAutoConfiguration {

	/**
     * Creates a GraphiteConfig bean if no other bean of the same type is present.
     * Uses the provided GraphiteProperties to create a GraphitePropertiesConfigAdapter.
     * 
     * @param graphiteProperties the GraphiteProperties used to create the GraphiteConfig bean
     * @return the created GraphiteConfig bean
     */
    @Bean
	@ConditionalOnMissingBean
	public GraphiteConfig graphiteConfig(GraphiteProperties graphiteProperties) {
		return new GraphitePropertiesConfigAdapter(graphiteProperties);
	}

	/**
     * Creates a new instance of GraphiteMeterRegistry if no other bean of the same type is present.
     * 
     * @param graphiteConfig the configuration for Graphite
     * @param clock the clock used for measuring time
     * @return a new instance of GraphiteMeterRegistry
     */
    @Bean
	@ConditionalOnMissingBean
	public GraphiteMeterRegistry graphiteMeterRegistry(GraphiteConfig graphiteConfig, Clock clock) {
		return new GraphiteMeterRegistry(graphiteConfig, clock);
	}

}
