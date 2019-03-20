/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.buffer;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.Metric;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BufferCounterService}.
 *
 * @author Venil Noronha
 */
public class BufferCounterServiceTests {

	private CounterBuffers counters = new CounterBuffers();

	private CounterService service = new BufferCounterService(this.counters);

	private BufferMetricReader reader = new BufferMetricReader(this.counters,
			new GaugeBuffers());

	@Test
	public void matchExtendedPrefix() {
		this.service.increment("foo");
		assertThat(this.reader.findOne("foo")).isNull();
		Metric<?> counterFooMetric = this.reader.findOne("counter.foo");
		assertThat(counterFooMetric).isNotNull();
		assertThat(counterFooMetric.getValue()).isEqualTo(1L);
	}

	@Test
	public void matchCounterPrefix() {
		this.service.increment("counterfoo");
		assertThat(this.reader.findOne("counterfoo")).isNull();
		Metric<?> counterCounterfooMetric = this.reader.findOne("counter.counterfoo");
		assertThat(counterCounterfooMetric).isNotNull();
		assertThat(counterCounterfooMetric.getValue()).isEqualTo(1L);
	}

	@Test
	public void matchCounterDotPrefix() {
		this.service.increment("counter.foo");
		assertThat(this.reader.findOne("counter.counter.foo")).isNull();
		Metric<?> counterFooMetric = this.reader.findOne("counter.foo");
		assertThat(counterFooMetric).isNotNull();
		assertThat(counterFooMetric.getValue()).isEqualTo(1L);
	}

	@Test
	public void matchMeterPrefix() {
		this.service.increment("meterfoo");
		assertThat(this.reader.findOne("meterfoo")).isNull();
		Metric<?> counterMeterfooMetric = this.reader.findOne("counter.meterfoo");
		assertThat(counterMeterfooMetric).isNotNull();
		assertThat(counterMeterfooMetric.getValue()).isEqualTo(1L);
	}

	@Test
	public void matchMeterDotPrefix() {
		this.service.increment("meter.foo");
		assertThat(this.reader.findOne("counter.meter.foo")).isNull();
		Metric<?> meterFooMetric = this.reader.findOne("meter.foo");
		assertThat(meterFooMetric).isNotNull();
		assertThat(meterFooMetric.getValue()).isEqualTo(1L);
	}

}
