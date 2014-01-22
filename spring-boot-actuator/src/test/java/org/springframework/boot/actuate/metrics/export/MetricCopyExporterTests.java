/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Date;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class MetricCopyExporterTests {

	private final InMemoryMetricRepository writer = new InMemoryMetricRepository();
	private final InMemoryMetricRepository reader = new InMemoryMetricRepository();
	private final MetricCopyExporter exporter = new MetricCopyExporter(this.reader, this.writer);

	@Test
	public void export() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.export();
		assertEquals(1, this.writer.count());
	}

	@Test
	public void timestamp() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.setEarliestTimestamp(new Date(System.currentTimeMillis() + 10000));
		this.exporter.export();
		assertEquals(0, this.writer.count());
	}

	@Test
	public void ignoreTimestamp() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.setIgnoreTimestamps(true);
		this.exporter.setEarliestTimestamp(new Date(System.currentTimeMillis() + 10000));
		this.exporter.export();
		assertEquals(1, this.writer.count());
	}

}
