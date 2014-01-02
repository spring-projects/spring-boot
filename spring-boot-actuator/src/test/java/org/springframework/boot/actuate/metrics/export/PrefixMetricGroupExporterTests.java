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

import java.util.Collections;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Iterables;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link PrefixMetricGroupExporter}.
 * 
 * @author Dave Syer
 */
public class PrefixMetricGroupExporterTests {

	private InMemoryMetricRepository reader = new InMemoryMetricRepository();

	private InMemoryMetricRepository writer = new InMemoryMetricRepository();

	private PrefixMetricGroupExporter exporter = new PrefixMetricGroupExporter(
			this.reader, this.writer);

	@Test
	public void prefixedMetricsCopied() {
		this.reader.set(new Metric<Number>("foo.bar", 2.3));
		this.reader.set(new Metric<Number>("foo.spam", 1.3));
		this.exporter.setGroups(Collections.singleton("foo"));
		this.exporter.export();
		assertEquals(1, Iterables.collection(this.writer.groups()).size());
	}

	@Test
	public void unprefixedMetricsNotCopied() {
		this.reader.set(new Metric<Number>("foo.bar", 2.3));
		this.reader.set(new Metric<Number>("foo.spam", 1.3));
		this.exporter.setGroups(Collections.singleton("bar"));
		this.exporter.export();
		assertEquals(0, Iterables.collection(this.writer.groups()).size());
	}

	@Test
	public void onlyPrefixedMetricsCopied() {
		this.reader.set(new Metric<Number>("foo.bar", 2.3));
		this.reader.set(new Metric<Number>("foo.spam", 1.3));
		this.reader.set(new Metric<Number>("foobar.spam", 1.3));
		this.exporter.setGroups(Collections.singleton("foo"));
		this.exporter.export();
		assertEquals(1, Iterables.collection(this.writer.groups()).size());
	}

}
