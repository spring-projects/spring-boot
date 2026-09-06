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

import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.jspecify.annotations.Nullable;
import reactor.pool.PoolMetricsRecorder;
import reactor.pool.introspection.micrometer.Micrometer;

import org.springframework.beans.factory.DisposableBean;

/**
 * Records metrics for the auto-configured connection pool once meter binding is complete.
 *
 * @author Goutam Adwant
 */
final class ConnectionPoolMetricsRecorder implements PoolMetricsRecorder, MeterBinder, DisposableBean {

	private final CompositeMeterRegistry registry = new CompositeMeterRegistry();

	private volatile @Nullable PoolMetricsRecorder delegate;

	@Override
	public void bindTo(MeterRegistry registry) {
		this.registry.add(registry);
	}

	@Override
	public void destroy() {
		Set.copyOf(this.registry.getRegistries()).forEach(this.registry::remove);
		this.registry.close();
	}

	private PoolMetricsRecorder getDelegate() {
		PoolMetricsRecorder delegate = this.delegate;
		if (delegate == null) {
			synchronized (this) {
				delegate = this.delegate;
				if (delegate == null) {
					delegate = Micrometer.recorder("connectionFactory", this.registry);
					this.delegate = delegate;
				}
			}
		}
		return delegate;
	}

	@Override
	public void recordAllocationSuccessAndLatency(long latencyMs) {
		getDelegate().recordAllocationSuccessAndLatency(latencyMs);
	}

	@Override
	public void recordAllocationFailureAndLatency(long latencyMs) {
		getDelegate().recordAllocationFailureAndLatency(latencyMs);
	}

	@Override
	public void recordResetLatency(long latencyMs) {
		getDelegate().recordResetLatency(latencyMs);
	}

	@Override
	public void recordDestroyLatency(long latencyMs) {
		getDelegate().recordDestroyLatency(latencyMs);
	}

	@Override
	public void recordRecycled() {
		getDelegate().recordRecycled();
	}

	@Override
	public void recordLifetimeDuration(long millisecondsSinceAllocation) {
		getDelegate().recordLifetimeDuration(millisecondsSinceAllocation);
	}

	@Override
	public void recordIdleTime(long millisecondsIdle) {
		getDelegate().recordIdleTime(millisecondsIdle);
	}

	@Override
	public void recordSlowPath() {
		getDelegate().recordSlowPath();
	}

	@Override
	public void recordFastPath() {
		getDelegate().recordFastPath();
	}

	@Override
	public void recordPendingSuccessAndLatency(long latencyMs) {
		getDelegate().recordPendingSuccessAndLatency(latencyMs);
	}

	@Override
	public void recordPendingFailureAndLatency(long latencyMs) {
		getDelegate().recordPendingFailureAndLatency(latencyMs);
	}

}
