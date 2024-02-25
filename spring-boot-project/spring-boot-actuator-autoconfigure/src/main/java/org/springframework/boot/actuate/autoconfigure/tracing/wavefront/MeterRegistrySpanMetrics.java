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

	/**
	 * Initializes a new instance of the MeterRegistrySpanMetrics class.
	 * @param meterRegistry The MeterRegistry to use for creating metrics.
	 */
	MeterRegistrySpanMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.spansReceived = meterRegistry.counter("wavefront.reporter.spans.received");
		this.spansDropped = meterRegistry.counter("wavefront.reporter.spans.dropped");
		this.reportErrors = meterRegistry.counter("wavefront.reporter.errors");
	}

	/**
	 * Reports that a span has been dropped. This method increments the counter for
	 * dropped spans in the MeterRegistrySpanMetrics class.
	 */
	@Override
	public void reportDropped() {
		this.spansDropped.increment();
	}

	/**
	 * Increments the count of received reports.
	 */
	@Override
	public void reportReceived() {
		this.spansReceived.increment();
	}

	/**
	 * Increments the error count for reporting errors.
	 *
	 * This method is used to increment the error count in the MeterRegistrySpanMetrics
	 * class. The error count is incremented by calling the increment() method of the
	 * reportErrors field.
	 *
	 * @see MeterRegistrySpanMetrics
	 * @see MeterRegistrySpanMetrics#reportErrors
	 * @see MeterRegistrySpanMetrics#reportErrors.increment()
	 */
	@Override
	public void reportErrors() {
		this.reportErrors.increment();
	}

	/**
	 * Registers the size of the specified blocking queue in the Wavefront reporter.
	 * @param queue the blocking queue whose size needs to be registered
	 */
	@Override
	public void registerQueueSize(BlockingQueue<?> queue) {
		this.meterRegistry.gauge("wavefront.reporter.queue.size", queue, (q) -> (double) q.size());
	}

	/**
	 * Registers a gauge metric to monitor the remaining capacity of a given
	 * BlockingQueue.
	 * @param queue the BlockingQueue to monitor
	 */
	@Override
	public void registerQueueRemainingCapacity(BlockingQueue<?> queue) {
		this.meterRegistry.gauge("wavefront.reporter.queue.remaining_capacity", queue, this::remainingCapacity);
	}

	/**
	 * Returns the remaining capacity of the specified blocking queue.
	 * @param queue the blocking queue to get the remaining capacity from
	 * @return the remaining capacity of the blocking queue
	 */
	private double remainingCapacity(BlockingQueue<?> queue) {
		return queue.remainingCapacity();
	}

}
