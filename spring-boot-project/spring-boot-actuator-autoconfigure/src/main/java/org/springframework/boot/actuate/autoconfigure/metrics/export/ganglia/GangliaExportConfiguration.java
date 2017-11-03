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

package org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.ganglia.GangliaConfig;
import io.micrometer.ganglia.GangliaMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.export.MetricsExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for exporting metrics to Ganglia.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(GangliaMeterRegistry.class)
@EnableConfigurationProperties(GangliaProperties.class)
public class GangliaExportConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GangliaConfig gangliaConfig(GangliaProperties gangliaProperties) {
		return new GangliaPropertiesConfigAdapter(gangliaProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.metrics.export.ganglia.enabled", matchIfMissing = true)
	public MetricsExporter gangliaExporter(GangliaConfig gangliaConfig,
			HierarchicalNameMapper nameMapper, Clock clock) {
		return () -> new GangliaMeterRegistry(gangliaConfig, nameMapper, clock);
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
