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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.MetricExportProperties;
import org.springframework.boot.actuate.metrics.export.MetricExporters;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Dave Syer
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "spring.metrics.export.enabled", matchIfMissing = true)
public class MetricExportAutoConfiguration {

	@Autowired(required = false)
	private Map<String, MetricWriter> writers = Collections.emptyMap();

	@Autowired
	private MetricExportProperties metrics;

	@Autowired(required = false)
	@ActuatorMetricRepository
	private MetricWriter actuatorMetricRepository;

	@Bean
	@Autowired(required = false)
	@ConditionalOnMissingBean
	public MetricExporters metricWritersMetricExporter(
			@ActuatorMetricRepository MetricReader reader) {
		Map<String, MetricWriter> writers = new HashMap<String, MetricWriter>();
		if (reader != null) {
			writers.putAll(this.writers);
			if (this.actuatorMetricRepository != null
					&& writers.containsValue(this.actuatorMetricRepository)) {
				for (String name : this.writers.keySet()) {
					if (writers.get(name).equals(this.actuatorMetricRepository)) {
						writers.remove(name);
					}
				}
			}
			MetricExporters exporters = new MetricExporters(reader, writers, this.metrics);
			return exporters;
		}
		return null;
	}

}
