/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.atlas;

import com.netflix.spectator.atlas.AtlasConfig;
import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.Clock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Atlas.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(AtlasMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("atlas")
@EnableConfigurationProperties(AtlasProperties.class)
public final class AtlasMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	AtlasConfig atlasConfig(AtlasProperties atlasProperties) {
		return new AtlasPropertiesConfigAdapter(atlasProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	AtlasMeterRegistry atlasMeterRegistry(AtlasConfig atlasConfig, Clock clock) {
		return new AtlasMeterRegistry(atlasConfig, clock);
	}

}
