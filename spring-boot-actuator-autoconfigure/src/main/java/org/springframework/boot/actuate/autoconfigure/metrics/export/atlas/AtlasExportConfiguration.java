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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import com.netflix.spectator.atlas.AtlasConfig;
import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.Clock;

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
 * Configuration for exporting metrics to Atlas.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(AtlasMeterRegistry.class)
@Import(StringToDurationConverter.class)
@EnableConfigurationProperties(AtlasProperties.class)
public class AtlasExportConfiguration {

	@Bean
	@ConditionalOnMissingBean(AtlasConfig.class)
	public AtlasConfig atlasConfig(AtlasProperties atlasProperties) {
		return new AtlasPropertiesConfigAdapter(atlasProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.metrics.atlas.enabled", matchIfMissing = true)
	public MetricsExporter atlasExporter(AtlasConfig atlasConfig, Clock clock) {
		return () -> new AtlasMeterRegistry(atlasConfig, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock clock() {
		return Clock.SYSTEM;
	}

}
