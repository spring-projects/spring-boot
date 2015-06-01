/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.MetricsEndpointMetricReader;
import org.springframework.boot.actuate.metrics.export.MetricExportProperties;
import org.springframework.boot.actuate.metrics.export.MetricExporters;
import org.springframework.boot.actuate.metrics.reader.CompositeMetricReader;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @author Dave Syer
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "spring.metrics.export.enabled", matchIfMissing = true)
@EnableConfigurationProperties
public class MetricExportAutoConfiguration {

	@Autowired(required = false)
	@ExportMetricWriter
	private Map<String, MetricWriter> writers = Collections.emptyMap();

	@Autowired
	private MetricExportProperties metrics;

	@Autowired(required = false)
	@ExportMetricReader
	private List<MetricReader> readers;

	@Autowired(required = false)
	private MetricsEndpointMetricReader endpointReader;

	@Configuration
	protected static class MetricExportPropertiesConfiguration {
		@Value("spring.metrics.${random.value:0000}.${spring.application.name:application}")
		private String prefix = "spring.metrics";

		@Bean(name = "spring.metrics.export.CONFIGURATION_PROPERTIES")
		@ConditionalOnMissingBean
		public MetricExportProperties metricExportProperties() {
			MetricExportProperties export = new MetricExportProperties();
			export.getRedis().setPrefix(prefix);
			return export;
		}
	}

	@Bean
	@ConditionalOnMissingBean(name = "metricWritersMetricExporter")
	public SchedulingConfigurer metricWritersMetricExporter() {

		Map<String, MetricWriter> writers = new HashMap<String, MetricWriter>();

		MetricReader reader = this.endpointReader;
		if (reader == null && this.readers != null && !this.readers.isEmpty()) {
			reader = new CompositeMetricReader(
					this.readers.toArray(new MetricReader[this.readers.size()]));
		}

		if (reader != null) {
			writers.putAll(this.writers);
			MetricExporters exporters = new MetricExporters(reader, writers, this.metrics);
			return exporters;
		}

		return new SchedulingConfigurer() {

			@Override
			public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			}
		};

	}

}
