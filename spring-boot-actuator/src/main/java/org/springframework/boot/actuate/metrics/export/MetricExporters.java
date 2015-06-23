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

import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * {@link SchedulingConfigurer} to handle metrics {@link MetricCopyExporter export}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class MetricExporters implements SchedulingConfigurer {

	private final MetricReader reader;

	private final Map<String, MetricWriter> writers;

	private final MetricExportProperties properties;

	private final Map<String, Exporter> exporters = new HashMap<String, Exporter>();

	public MetricExporters(MetricReader reader, Map<String, MetricWriter> writers,
			MetricExportProperties properties) {
		this.reader = reader;
		this.writers = writers;
		this.properties = properties;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		for (Entry<String, MetricWriter> entry : this.writers.entrySet()) {
			String name = entry.getKey();
			MetricWriter writer = entry.getValue();
			TriggerProperties trigger = this.properties.findTrigger(name);
			if (trigger != null) {
				MetricCopyExporter exporter = getExporter(writer, trigger);
				this.exporters.put(name, exporter);
				ExportRunner runner = new ExportRunner(exporter);
				IntervalTask task = new IntervalTask(runner, trigger.getDelayMillis(),
						trigger.getDelayMillis());
				taskRegistrar.addFixedDelayTask(task);
			}
		}
	}

	private MetricCopyExporter getExporter(MetricWriter writer, TriggerProperties trigger) {
		MetricCopyExporter exporter = new MetricCopyExporter(this.reader, writer);
		exporter.setIncludes(trigger.getIncludes());
		exporter.setExcludes(trigger.getExcludes());
		exporter.setSendLatest(trigger.isSendLatest());
		return exporter;
	}

	public Map<String, Exporter> getExporters() {
		return this.exporters;
	}

	private static class ExportRunner implements Runnable {

		private final MetricCopyExporter exporter;

		public ExportRunner(MetricCopyExporter exporter) {
			this.exporter = exporter;
		}

		@Override
		public void run() {
			this.exporter.export();
		}

	}

}
