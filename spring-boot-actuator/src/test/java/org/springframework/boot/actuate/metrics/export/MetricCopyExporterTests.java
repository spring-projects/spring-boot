/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.export;

import java.util.Date;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricCopyExporter}.
 *
 * @author Dave Syer
 */
public class MetricCopyExporterTests {

	private final InMemoryMetricRepository writer = new InMemoryMetricRepository();

	private final InMemoryMetricRepository reader = new InMemoryMetricRepository();

	private final MetricCopyExporter exporter = new MetricCopyExporter(this.reader,
			this.writer);

	@Test
	public void export() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(1);
	}

	@Test
	public void counter() {
		this.reader.increment(new Delta<Number>("counter.foo", 2));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(1);
		this.reader.increment(new Delta<Number>("counter.foo", 3));
		this.exporter.export();
		this.exporter.flush();
		assertThat(this.writer.findOne("counter.foo").getValue()).isEqualTo(5L);
	}

	@Test
	public void counterWithGaugeWriter() throws Exception {
		SimpleGaugeWriter writer = new SimpleGaugeWriter();
		MetricCopyExporter exporter = new MetricCopyExporter(this.reader, writer);
		try {
			this.reader.increment(new Delta<Number>("counter.foo", 2));
			exporter.export();
			this.reader.increment(new Delta<Number>("counter.foo", 3));
			exporter.export();
			exporter.flush();
			assertThat(writer.getValue().getValue()).isEqualTo(5L);
		}
		finally {
			exporter.close();
		}
	}

	@Test
	public void exportIncludes() {
		this.exporter.setIncludes("*");
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(1);
	}

	@Test
	public void exportExcludesWithIncludes() {
		this.exporter.setIncludes("*");
		this.exporter.setExcludes("foo");
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.reader.set(new Metric<Number>("bar", 2.4));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(1);
	}

	@Test
	public void exportExcludesDefaultIncludes() {
		this.exporter.setExcludes("foo");
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.reader.set(new Metric<Number>("bar", 2.4));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(1);
	}

	@Test
	public void timestamp() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.setEarliestTimestamp(new Date(System.currentTimeMillis() + 10000));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(0);
	}

	@Test
	public void ignoreTimestamp() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.setIgnoreTimestamps(true);
		this.exporter.setEarliestTimestamp(new Date(System.currentTimeMillis() + 10000));
		this.exporter.export();
		assertThat(this.writer.count()).isEqualTo(1);
	}

	private static class SimpleGaugeWriter implements GaugeWriter {

		private Metric<?> value;

		@Override
		public void set(Metric<?> value) {
			this.value = value;
		}

		public Metric<?> getValue() {
			return this.value;
		}

	}

}
