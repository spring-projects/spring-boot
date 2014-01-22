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

package org.springframework.boot.actuate.metrics.writer;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class CodahaleMetricWriterTests {

	private final MetricRegistry registry = new MetricRegistry();
	private final CodahaleMetricWriter writer = new CodahaleMetricWriter(this.registry);

	@Test
	public void incrementCounter() {
		this.writer.increment(new Delta<Number>("foo", 2));
		this.writer.increment(new Delta<Number>("foo", 1));
		assertEquals(3, this.registry.counter("foo").getCount());
	}

	@Test
	public void updatePredefinedMeter() {
		this.writer.increment(new Delta<Number>("meter.foo", 2));
		this.writer.increment(new Delta<Number>("meter.foo", 1));
		assertEquals(3, this.registry.meter("meter.foo").getCount());
	}

	@Test
	public void updatePredefinedCounter() {
		this.writer.increment(new Delta<Number>("counter.foo", 2));
		this.writer.increment(new Delta<Number>("counter.foo", 1));
		assertEquals(3, this.registry.counter("counter.foo").getCount());
	}

	@Test
	public void setGauge() {
		this.writer.set(new Metric<Number>("foo", 2.1));
		this.writer.set(new Metric<Number>("foo", 2.3));
		@SuppressWarnings("unchecked")
		Gauge<Double> gauge = (Gauge<Double>) this.registry.getMetrics().get("foo");
		assertEquals(new Double(2.3), gauge.getValue());
	}

	@Test
	public void setPredfinedTimer() {
		this.writer.set(new Metric<Number>("timer.foo", 200));
		this.writer.set(new Metric<Number>("timer.foo", 300));
		assertEquals(2, this.registry.timer("timer.foo").getCount());
	}

	@Test
	public void setPredfinedHistogram() {
		this.writer.set(new Metric<Number>("histogram.foo", 2.1));
		this.writer.set(new Metric<Number>("histogram.foo", 2.3));
		assertEquals(2, this.registry.histogram("histogram.foo").getCount());
	}

}
