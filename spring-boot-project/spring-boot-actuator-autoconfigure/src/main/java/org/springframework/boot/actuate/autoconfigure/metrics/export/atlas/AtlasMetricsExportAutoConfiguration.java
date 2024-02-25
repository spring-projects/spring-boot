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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import com.netflix.spectator.atlas.AtlasConfig;
import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.Clock;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Atlas.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(AtlasMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("atlas")
@EnableConfigurationProperties(AtlasProperties.class)
public class AtlasMetricsExportAutoConfiguration {

	/**
	 * Creates an instance of AtlasConfig if no other bean of the same type is present.
	 * Uses the provided AtlasProperties to create an instance of
	 * AtlasPropertiesConfigAdapter.
	 * @param atlasProperties the AtlasProperties object used to create the
	 * AtlasPropertiesConfigAdapter
	 * @return an instance of AtlasConfig
	 */
	@Bean
	@ConditionalOnMissingBean
	public AtlasConfig atlasConfig(AtlasProperties atlasProperties) {
		return new AtlasPropertiesConfigAdapter(atlasProperties);
	}

	/**
	 * Creates a new instance of {@link AtlasMeterRegistry} if no other bean of the same
	 * type is present.
	 * @param atlasConfig the configuration for Atlas
	 * @param clock the clock used for measuring time
	 * @return a new instance of {@link AtlasMeterRegistry}
	 */
	@Bean
	@ConditionalOnMissingBean
	public AtlasMeterRegistry atlasMeterRegistry(AtlasConfig atlasConfig, Clock clock) {
		return new AtlasMeterRegistry(atlasConfig, clock);
	}

}
