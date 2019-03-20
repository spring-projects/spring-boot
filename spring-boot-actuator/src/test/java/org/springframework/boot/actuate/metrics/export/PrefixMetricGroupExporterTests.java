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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Iterables;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMultiMetricRepository;
import org.springframework.boot.actuate.metrics.writer.Delta;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrefixMetricGroupExporter}.
 *
 * @author Dave Syer
 */
public class PrefixMetricGroupExporterTests {

	private final InMemoryMultiMetricRepository reader = new InMemoryMultiMetricRepository();

	private final InMemoryMultiMetricRepository writer = new InMemoryMultiMetricRepository();

	private final PrefixMetricGroupExporter exporter = new PrefixMetricGroupExporter(
			this.reader, this.writer);

	@Test
	public void prefixedMetricsCopied() {
		this.reader.set("foo", Arrays.<Metric<?>>asList(new Metric<Number>("bar", 2.3),
				new Metric<Number>("spam", 1.3)));
		this.exporter.setGroups(Collections.singleton("foo"));
		this.exporter.export();
		assertThat(Iterables.collection(this.writer.groups())).hasSize(1);
	}

	@Test
	public void countersIncremented() {
		this.writer.increment("counter.foo", new Delta<Long>("bar", 1L));
		this.reader.set("counter", Collections
				.<Metric<?>>singletonList(new Metric<Number>("counter.foo.bar", 1)));
		this.exporter.setGroups(Collections.singleton("counter.foo"));
		this.exporter.export();
		assertThat(this.writer.findAll("counter.foo").iterator().next().getValue())
				.isEqualTo(2L);
	}

	@Test
	public void unprefixedMetricsNotCopied() {
		this.reader.set("foo", Arrays.<Metric<?>>asList(
				new Metric<Number>("foo.bar", 2.3), new Metric<Number>("foo.spam", 1.3)));
		this.exporter.setGroups(Collections.singleton("bar"));
		this.exporter.export();
		assertThat(Iterables.collection(this.writer.groups())).isEmpty();
	}

	@Test
	public void multiMetricGroupsCopiedAsDefault() {
		this.reader.set("foo", Arrays.<Metric<?>>asList(new Metric<Number>("bar", 2.3),
				new Metric<Number>("spam", 1.3)));
		this.exporter.export();
		assertThat(this.writer.countGroups()).isEqualTo(1);
		assertThat(Iterables.collection(this.writer.findAll("foo"))).hasSize(2);
	}

	@Test
	public void onlyPrefixedMetricsCopied() {
		this.reader.set("foo", Arrays.<Metric<?>>asList(
				new Metric<Number>("foo.bar", 2.3), new Metric<Number>("foo.spam", 1.3)));
		this.reader.set("foobar", Collections
				.<Metric<?>>singletonList(new Metric<Number>("foobar.spam", 1.3)));
		this.exporter.setGroups(Collections.singleton("foo"));
		this.exporter.export();
		assertThat(Iterables.collection(this.writer.groups())).hasSize(1);
	}

}
