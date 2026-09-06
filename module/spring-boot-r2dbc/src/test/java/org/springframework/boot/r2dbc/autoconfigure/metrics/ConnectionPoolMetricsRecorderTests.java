/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.r2dbc.autoconfigure.metrics;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionPoolMetricsRecorder}.
 *
 * @author Goutam Adwant
 */
class ConnectionPoolMetricsRecorderTests {

	@Test
	void unusedRecorderDoesNotRegisterMeters() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		try {
			new ConnectionPoolMetricsRecorder().bindTo(registry);
			assertThat(registry.getMeters()).isEmpty();
		}
		finally {
			registry.close();
		}
	}

	@Test
	void recordsAllLifecycleEvents() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		try {
			ConnectionPoolMetricsRecorder recorder = new ConnectionPoolMetricsRecorder();
			recorder.bindTo(registry);
			recorder.recordAllocationSuccessAndLatency(10);
			recorder.recordAllocationFailureAndLatency(20);
			recorder.recordResetLatency(30);
			recorder.recordDestroyLatency(40);
			recorder.recordRecycled();
			recorder.recordLifetimeDuration(50);
			recorder.recordIdleTime(60);
			recorder.recordSlowPath();
			recorder.recordFastPath();
			recorder.recordPendingSuccessAndLatency(70);
			recorder.recordPendingFailureAndLatency(80);
			assertThat(registry.get("reactor.pool.allocation")
				.tag("pool.allocation.outcome", "success")
				.timer()
				.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(10);
			assertThat(registry.get("reactor.pool.allocation")
				.tag("pool.allocation.outcome", "failure")
				.timer()
				.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(20);
			assertThat(registry.get("reactor.pool.reset").timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(30);
			assertThat(registry.get("reactor.pool.destroyed").timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(40);
			assertThat(registry.get("reactor.pool.recycled").counter().count()).isEqualTo(1);
			assertThat(registry.get("reactor.pool.resources.summary.lifetime").timer().totalTime(TimeUnit.MILLISECONDS))
				.isEqualTo(50);
			assertThat(registry.get("reactor.pool.resources.summary.idleness").timer().totalTime(TimeUnit.MILLISECONDS))
				.isEqualTo(60);
			assertThat(
					registry.get("reactor.pool.recycled.notable").tag("pool.recycling.path", "slow").counter().count())
				.isEqualTo(1);
			assertThat(
					registry.get("reactor.pool.recycled.notable").tag("pool.recycling.path", "fast").counter().count())
				.isEqualTo(1);
			assertThat(registry.get("reactor.pool.pending")
				.tag("pool.pending.outcome", "success")
				.timer()
				.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(70);
			assertThat(registry.get("reactor.pool.pending")
				.tag("pool.pending.outcome", "failure")
				.timer()
				.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(80);
			assertThat(registry.getMeters())
				.allSatisfy((meter) -> assertThat(meter.getId().getTag("pool.name")).isEqualTo("connectionFactory"));
		}
		finally {
			registry.close();
		}
	}

	@Test
	void eventsBeforeBindingAreNotReplayed() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		try {
			ConnectionPoolMetricsRecorder recorder = new ConnectionPoolMetricsRecorder();
			recorder.recordAllocationSuccessAndLatency(10);
			recorder.bindTo(registry);
			recorder.recordAllocationSuccessAndLatency(20);
			assertThat(
					registry.get("reactor.pool.allocation").tag("pool.allocation.outcome", "success").timer().count())
				.isEqualTo(1);
			assertThat(registry.get("reactor.pool.allocation")
				.tag("pool.allocation.outcome", "success")
				.timer()
				.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(20);
		}
		finally {
			registry.close();
		}
	}

	@Test
	void bindingMultipleRegistriesIsIdempotent() {
		SimpleMeterRegistry first = new SimpleMeterRegistry();
		SimpleMeterRegistry second = new SimpleMeterRegistry();
		try {
			ConnectionPoolMetricsRecorder recorder = new ConnectionPoolMetricsRecorder();
			recorder.bindTo(first);
			recorder.bindTo(first);
			recorder.bindTo(second);
			recorder.recordRecycled();
			assertThat(first.get("reactor.pool.recycled").counter().count()).isEqualTo(1);
			assertThat(second.get("reactor.pool.recycled").counter().count()).isEqualTo(1);
		}
		finally {
			first.close();
			second.close();
		}
	}

	@Test
	void destructionDetachesWithoutClosingSharedRegistry() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		try {
			ConnectionPoolMetricsRecorder recorder = new ConnectionPoolMetricsRecorder();
			recorder.bindTo(registry);
			recorder.recordRecycled();
			recorder.destroy();
			recorder.recordRecycled();
			assertThat(registry.isClosed()).isFalse();
			assertThat(registry.get("reactor.pool.recycled").counter().count()).isEqualTo(1);
			registry.counter("application.requests").increment();
			assertThat(registry.get("application.requests").counter().count()).isEqualTo(1);
		}
		finally {
			registry.close();
		}
	}

	@Test
	void bindingCompositeAndChildDoesNotDuplicateEvents() {
		SimpleMeterRegistry child = new SimpleMeterRegistry();
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		composite.add(child);
		try {
			ConnectionPoolMetricsRecorder recorder = new ConnectionPoolMetricsRecorder();
			recorder.bindTo(composite);
			recorder.bindTo(child);
			recorder.recordRecycled();
			assertThat(child.get("reactor.pool.recycled").counter().count()).isEqualTo(1);
			recorder.destroy();
			assertThat(composite.isClosed()).isFalse();
			assertThat(child.isClosed()).isFalse();
		}
		finally {
			composite.close();
			child.close();
		}
	}

}
