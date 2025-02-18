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

import java.util.concurrent.BlockingQueue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.reporter.wavefront.SpanMetrics;

/**
 * Bridges {@link SpanMetrics} to a {@link MeterRegistry}.
 *
 * @author Moritz Halbritter
 */
class MeterRegistrySpanMetrics implements SpanMetrics {

	private final Counter spansReceived;

	private final Counter spansDropped;

	private final Counter reportErrors;

	private final MeterRegistry meterRegistry;

	MeterRegistrySpanMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.spansReceived = meterRegistry.counter("wavefront.reporter.spans.received");
		this.spansDropped = meterRegistry.counter("wavefront.reporter.spans.dropped");
		this.reportErrors = meterRegistry.counter("wavefront.reporter.errors");
	}

	@Override
	public void reportDropped() {
		this.spansDropped.increment();
	}

	@Override
	public void reportReceived() {
		this.spansReceived.increment();
	}

	@Override
	public void reportErrors() {
		this.reportErrors.increment();
	}

	@Override
	public void registerQueueSize(BlockingQueue<?> queue) {
		this.meterRegistry.gauge("wavefront.reporter.queue.size", queue, (q) -> (double) q.size());
	}

	@Override
	public void registerQueueRemainingCapacity(BlockingQueue<?> queue) {
		this.meterRegistry.gauge("wavefront.reporter.queue.remaining_capacity", queue, this::remainingCapacity);
	}

	private double remainingCapacity(BlockingQueue<?> queue) {
		return queue.remainingCapacity();
	}

}
