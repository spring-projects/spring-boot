/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.MetricsEndpointMetricReader;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.export.MetricExportProperties;
import org.springframework.boot.actuate.metrics.export.MetricExporters;
import org.springframework.boot.actuate.metrics.reader.CompositeMetricReader;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.statsd.StatsdMetricWriter;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.CollectionUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics export.
 *
 * @author Dave Syer
 * @author Simon Buettner
 * @since 1.3.0
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "spring.metrics.export.enabled", matchIfMissing = true)
@EnableConfigurationProperties
public class MetricExportAutoConfiguration {

	private final MetricsEndpointMetricReader endpointReader;

	private final List<MetricReader> readers;

	private final Map<String, GaugeWriter> writers;

	private final Map<String, Exporter> exporters;

	public MetricExportAutoConfiguration(MetricExportProperties properties,
			ObjectProvider<MetricsEndpointMetricReader> endpointReader,
			@ExportMetricReader ObjectProvider<List<MetricReader>> readers,
			@ExportMetricWriter ObjectProvider<Map<String, GaugeWriter>> writers,
			ObjectProvider<Map<String, Exporter>> exporters) {
		this.endpointReader = endpointReader.getIfAvailable();
		this.readers = readers.getIfAvailable();
		this.writers = writers.getIfAvailable();
		this.exporters = exporters.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean(name = "metricWritersMetricExporter")
	public SchedulingConfigurer metricWritersMetricExporter(
			MetricExportProperties properties) {
		Map<String, GaugeWriter> writers = new HashMap<String, GaugeWriter>();
		MetricReader reader = this.endpointReader;
		if (reader == null && !CollectionUtils.isEmpty(this.readers)) {
			reader = new CompositeMetricReader(
					this.readers.toArray(new MetricReader[this.readers.size()]));
		}
		if (reader == null && CollectionUtils.isEmpty(this.exporters)) {
			return new NoOpSchedulingConfigurer();
		}
		MetricExporters exporters = new MetricExporters(properties);
		if (reader != null) {
			if (!CollectionUtils.isEmpty(this.writers)) {
				writers.putAll(this.writers);
			}
			exporters.setReader(reader);
			exporters.setWriters(writers);
		}
		exporters.setExporters((this.exporters != null) ? this.exporters
				: Collections.<String, Exporter>emptyMap());
		return exporters;
	}

	@Configuration
	static class StatsdConfiguration {

		@Bean
		@ExportMetricWriter
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "spring.metrics.export.statsd", name = "host")
		public StatsdMetricWriter statsdMetricWriter(MetricExportProperties properties) {
			MetricExportProperties.Statsd statsdProperties = properties.getStatsd();
			return new StatsdMetricWriter(statsdProperties.getPrefix(),
					statsdProperties.getHost(), statsdProperties.getPort());
		}

	}

	@Configuration
	protected static class MetricExportPropertiesConfiguration {

		@Value("${spring.application.name:application}.${random.value:0000}")
		private String prefix = "";

		private String aggregateKeyPattern = "k.d";

		@Bean(name = "spring.metrics.export-org.springframework.boot.actuate.metrics.export.MetricExportProperties")
		@ConditionalOnMissingBean
		public MetricExportProperties metricExportProperties() {
			MetricExportProperties export = new MetricExportProperties();
			export.getRedis().setPrefix("spring.metrics"
					+ ((this.prefix.length() > 0) ? "." : "") + this.prefix);
			export.getAggregate().setPrefix(this.prefix);
			export.getAggregate().setKeyPattern(this.aggregateKeyPattern);
			return export;
		}

	}

	private static class NoOpSchedulingConfigurer implements SchedulingConfigurer {

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		}

	}

}
