/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.wavefront;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MeterRegistrySpanMetrics}.
 *
 * @author Moritz Halbritter
 */
class MeterRegistrySpanMetricsTests {

	private SimpleMeterRegistry meterRegistry;

	private MeterRegistrySpanMetrics sut;

	@BeforeEach
	void setUp() {
		this.meterRegistry = new SimpleMeterRegistry();
		this.sut = new MeterRegistrySpanMetrics(this.meterRegistry);
	}

	@Test
	void reportDroppedShouldIncreaseCounter() {
		this.sut.reportDropped();
		assertThat(getCounterValue("wavefront.reporter.spans.dropped")).isEqualTo(1);
		this.sut.reportDropped();
		assertThat(getCounterValue("wavefront.reporter.spans.dropped")).isEqualTo(2);
	}

	@Test
	void reportReceivedShouldIncreaseCounter() {
		this.sut.reportReceived();
		assertThat(getCounterValue("wavefront.reporter.spans.received")).isEqualTo(1);
		this.sut.reportReceived();
		assertThat(getCounterValue("wavefront.reporter.spans.received")).isEqualTo(2);
	}

	@Test
	void reportErrorsShouldIncreaseCounter() {
		this.sut.reportErrors();
		assertThat(getCounterValue("wavefront.reporter.errors")).isEqualTo(1);
		this.sut.reportErrors();
		assertThat(getCounterValue("wavefront.reporter.errors")).isEqualTo(2);
	}

	@Test
	void registerQueueSizeShouldCreateGauge() {
		BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(2);
		this.sut.registerQueueSize(queue);
		assertThat(getGaugeValue("wavefront.reporter.queue.size")).isEqualTo(0);
		queue.offer(1);
		assertThat(getGaugeValue("wavefront.reporter.queue.size")).isEqualTo(1);
	}

	@Test
	void registerQueueRemainingCapacityShouldCreateGauge() {
		BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(2);
		this.sut.registerQueueRemainingCapacity(queue);
		assertThat(getGaugeValue("wavefront.reporter.queue.remaining_capacity")).isEqualTo(2);
		queue.offer(1);
		assertThat(getGaugeValue("wavefront.reporter.queue.remaining_capacity")).isEqualTo(1);
	}

	private double getGaugeValue(String name) {
		Gauge gauge = this.meterRegistry.find(name).gauge();
		assertThat(gauge).withFailMessage("Gauge '%s' not found", name).isNotNull();
		return gauge.value();
	}

	private double getCounterValue(String name) {
		Counter counter = this.meterRegistry.find(name).counter();
		assertThat(counter).withFailMessage("Counter '%s' not found", name).isNotNull();
		return counter.count();
	}

}
