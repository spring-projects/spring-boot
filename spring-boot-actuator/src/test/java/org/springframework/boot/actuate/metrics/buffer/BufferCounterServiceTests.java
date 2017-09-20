/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.buffer;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.Metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Standard tests for {@link BufferCounterService}.
 *
 * @author Venil Noronha
 */
public class BufferCounterServiceTests {

	private CounterBuffers counters = new CounterBuffers();

	private CounterService service = new BufferCounterService(this.counters);

	private BufferMetricReader reader = new BufferMetricReader(this.counters, new GaugeBuffers());

	@Test
	public void matchExtendedPrefix() {
		this.service.increment("foo");
		Metric<?> fooMetric = this.reader.findOne("foo");
		Metric<?> counterFooMetric = this.reader.findOne("counter.foo");
		assertNull(fooMetric);
		assertNotNull(counterFooMetric);
		assertEquals(1L, counterFooMetric.getValue());
	}

	@Test
	public void matchCounterPrefix() {
		this.service.increment("counterfoo");
		Metric<?> counterfooMetric = this.reader.findOne("counterfoo");
		Metric<?> counterCounterfooMetric = this.reader.findOne("counter.counterfoo");
		assertNull(counterfooMetric);
		assertNotNull(counterCounterfooMetric);
		assertEquals(1L, counterCounterfooMetric.getValue());
	}

	@Test
	public void matchCounterDotPrefix() {
		this.service.increment("counter.foo");
		Metric<?> counterFooMetric = this.reader.findOne("counter.foo");
		Metric<?> counterCounterFooMetric = this.reader.findOne("counter.counter.foo");
		assertNull(counterCounterFooMetric);
		assertNotNull(counterFooMetric);
		assertEquals(1L, counterFooMetric.getValue());
	}

	@Test
	public void matchMeterPrefix() {
		this.service.increment("meterfoo");
		Metric<?> meterfooMetric = this.reader.findOne("meterfoo");
		Metric<?> counterMeterfooMetric = this.reader.findOne("counter.meterfoo");
		assertNull(meterfooMetric);
		assertNotNull(counterMeterfooMetric);
		assertEquals(1L, counterMeterfooMetric.getValue());
	}

	@Test
	public void matchMeterDotPrefix() {
		this.service.increment("meter.foo");
		Metric<?> meterFooMetric = this.reader.findOne("meter.foo");
		Metric<?> counterMeterFooMetric = this.reader.findOne("counter.meter.foo");
		assertNull(counterMeterFooMetric);
		assertNotNull(meterFooMetric);
		assertEquals(1L, meterFooMetric.getValue());
	}

}
