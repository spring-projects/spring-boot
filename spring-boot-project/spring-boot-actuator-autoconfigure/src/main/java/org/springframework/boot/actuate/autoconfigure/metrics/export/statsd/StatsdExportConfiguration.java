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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.export.MetricsExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for exporting metrics to StatsD.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(StatsdMeterRegistry.class)
@EnableConfigurationProperties(StatsdProperties.class)
public class StatsdExportConfiguration {

	@Bean
	@ConditionalOnMissingBean(StatsdConfig.class)
	public StatsdConfig statsdConfig(StatsdProperties statsdProperties) {
		return new StatsdPropertiesConfigAdapter(statsdProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.metrics.export.statsd.enabled", matchIfMissing = true)
	public MetricsExporter statsdExporter(StatsdConfig statsdConfig,
			HierarchicalNameMapper hierarchicalNameMapper, Clock clock) {
		return () -> new StatsdMeterRegistry(statsdConfig, hierarchicalNameMapper, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}

	@Bean
	@ConditionalOnMissingBean
	public HierarchicalNameMapper hierarchicalNameMapper() {
		return HierarchicalNameMapper.DEFAULT;
	}

}
