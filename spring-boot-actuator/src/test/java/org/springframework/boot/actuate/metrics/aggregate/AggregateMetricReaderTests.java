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

package org.springframework.boot.actuate.metrics.aggregate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.writer.Delta;

/**
 * Tests for {@link AggregateMetricReader}.
 *
 * @author Dave Syer
 */
public class AggregateMetricReaderTests {

	private InMemoryMetricRepository source = new InMemoryMetricRepository();

	private AggregateMetricReader reader = new AggregateMetricReader(this.source);

	@Test
	public void writeAndReadDefaults() {
		this.source.set(new Metric<Double>("foo.bar.spam", 2.3));
		assertEquals(2.3, this.reader.findOne("aggregate.spam").getValue());
	}

	@Test
	public void defaultKeyPattern() {
		this.source.set(new Metric<Double>("foo.bar.spam.bucket.wham", 2.3));
		assertEquals(2.3, this.reader.findOne("aggregate.spam.bucket.wham").getValue());
	}

	@Test
	public void addKeyPattern() {
		this.source.set(new Metric<Double>("foo.bar.spam.bucket.wham", 2.3));
		this.reader.setKeyPattern("d.d.k.d");
		assertEquals(2.3, this.reader.findOne("aggregate.spam.wham").getValue());
	}

	@Test
	public void addPrefix() {
		this.source.set(new Metric<Double>("foo.bar.spam.bucket.wham", 2.3));
		this.source.set(new Metric<Double>("off.bar.spam.bucket.wham", 2.4));
		this.reader.setPrefix("www");
		this.reader.setKeyPattern("k.d.k.d");
		assertEquals(2.3, this.reader.findOne("www.foo.spam.wham").getValue());
		assertEquals(2, this.reader.count());
	}

	@Test
	public void writeAndReadExtraLong() {
		this.source.set(new Metric<Double>("blee.foo.bar.spam", 2.3));
		this.reader.setKeyPattern("d.d.d.k");
		assertEquals(2.3, this.reader.findOne("aggregate.spam").getValue());
	}

	@Test
	public void writeAndReadLatestValue() {
		this.source.set(new Metric<Double>("foo.bar.spam", 2.3, new Date(100L)));
		this.source.set(new Metric<Double>("oof.rab.spam", 2.4, new Date(0L)));
		assertEquals(2.3, this.reader.findOne("aggregate.spam").getValue());
	}

	@Test
	public void onlyPrefixed() {
		this.source.set(new Metric<Double>("foo.bar.spam", 2.3));
		assertNull(this.reader.findOne("spam"));
	}

	@Test
	public void incrementCounter() {
		this.source.increment(new Delta<Long>("foo.bar.counter.spam", 2L));
		this.source.increment(new Delta<Long>("oof.rab.counter.spam", 3L));
		assertEquals(5L, this.reader.findOne("aggregate.counter.spam").getValue());
	}

	@Test
	public void countGauges() {
		this.source.set(new Metric<Double>("foo.bar.spam", 2.3));
		this.source.set(new Metric<Double>("oof.rab.spam", 2.4));
		assertEquals(1, this.reader.count());
	}

	@Test
	public void countGaugesAndCounters() {
		this.source.set(new Metric<Double>("foo.bar.spam", 2.3));
		this.source.set(new Metric<Double>("oof.rab.spam", 2.4));
		this.source.increment(new Delta<Long>("foo.bar.counter.spam", 2L));
		this.source.increment(new Delta<Long>("oof.rab.counter.spam", 3L));
		assertEquals(2, this.reader.count());
	}

}
