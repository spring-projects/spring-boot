package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import org.springframework.boot.actuate.autoconfigure.metrics.export.MetricsExporter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.StringToDurationConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.micrometer.core.instrument.Clock;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;

/**
 * Configuration for exporting metrics to StatsD.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(StatsdMeterRegistry.class)
@Import(StringToDurationConverter.class)
@EnableConfigurationProperties(StatsdProperties.class)
public class StatsdExportConfiguration {

	@Bean
	@ConditionalOnMissingBean(StatsdConfig.class)
	public StatsdConfig statsdConfig(StatsdProperties statsdProperties) {
		return new StatsdPropertiesConfigAdapter(statsdProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.metrics.statsd.enabled", matchIfMissing = true)
	public MetricsExporter statsdExporter(StatsdConfig statsdConfig, Clock clock) {
		return () -> new StatsdMeterRegistry(statsdConfig, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock clock() {
		return Clock.SYSTEM;
	}

}
