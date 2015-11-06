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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link MetricExporters}.
 *
 * @author Dave Syer
 */
public class MetricExportersTests {

	private MetricExporters exporters;

	private MetricExportProperties export = new MetricExportProperties();

	private Map<String, GaugeWriter> writers = new LinkedHashMap<String, GaugeWriter>();

	private MetricReader reader = Mockito.mock(MetricReader.class);

	private MetricWriter writer = Mockito.mock(MetricWriter.class);

	@Test
	public void emptyWriters() {
		this.exporters = new MetricExporters(this.export);
		this.exporters.setReader(this.reader);
		this.exporters.setWriters(this.writers);
		this.exporters.configureTasks(new ScheduledTaskRegistrar());
		assertNotNull(this.exporters.getExporters());
		assertEquals(0, this.exporters.getExporters().size());
	}

	@Test
	public void oneWriter() {
		this.export.setUpDefaults();
		this.writers.put("foo", this.writer);
		this.exporters = new MetricExporters(this.export);
		this.exporters.setReader(this.reader);
		this.exporters.setWriters(this.writers);
		this.exporters.configureTasks(new ScheduledTaskRegistrar());
		assertNotNull(this.exporters.getExporters());
		assertEquals(1, this.exporters.getExporters().size());
	}

	@Test
	public void exporter() {
		this.export.setUpDefaults();
		this.exporters = new MetricExporters(this.export);
		this.exporters.setExporters(Collections.<String, Exporter>singletonMap("foo",
				new MetricCopyExporter(this.reader, this.writer)));
		this.exporters.configureTasks(new ScheduledTaskRegistrar());
		assertNotNull(this.exporters.getExporters());
		assertEquals(1, this.exporters.getExporters().size());
	}

}
