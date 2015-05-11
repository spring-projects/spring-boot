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

package org.springframework.boot.actuate.metrics.export;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.actuate.metrics.export.MetricExportProperties.Export;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @author Dave Syer
 */
public class MetricExporters implements SchedulingConfigurer {

	private MetricExportProperties export;

	private Map<String, MetricWriter> writers;

	private Map<String, Exporter> exporters = new HashMap<String, Exporter>();

	private MetricReader reader;

	public MetricExporters(MetricReader reader, Map<String, MetricWriter> writers,
			MetricExportProperties export) {
		this.reader = reader;
		this.export = export;
		this.writers = writers;
	}

	public Map<String, Exporter> getExporters() {
		return this.exporters;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

		for (Entry<String, MetricWriter> entry : this.writers.entrySet()) {
			String name = entry.getKey();
			Export trigger = this.export.findExport(name);

			if (trigger != null) {

				MetricWriter writer = entry.getValue();
				final MetricCopyExporter exporter = new MetricCopyExporter(this.reader,
						writer);
				if (trigger.getIncludes() != null || trigger.getExcludes() != null) {
					exporter.setIncludes(trigger.getIncludes());
					exporter.setExcludes(trigger.getExcludes());
				}
				exporter.setIgnoreTimestamps(trigger.isIgnoreTimestamps());
				exporter.setSendLatest(trigger.isSendLatest());

				this.exporters.put(name, exporter);

				taskRegistrar.addFixedDelayTask(new IntervalTask(new Runnable() {
					@Override
					public void run() {
						exporter.export();
					}
				}, trigger.getDelayMillis(), trigger.getDelayMillis()));

			}

		}

	}

}
