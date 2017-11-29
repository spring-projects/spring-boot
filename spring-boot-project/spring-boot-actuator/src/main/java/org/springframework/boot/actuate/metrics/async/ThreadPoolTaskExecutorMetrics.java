/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.async;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Monitors the status of {@link ThreadPoolTaskExecutor} pools. Does not record timings on
 * operations executed in the {@link ExecutorService}, as this requires the instance to be
 * wrapped. Timings are provided separately by wrapping the executor service with
 * {@link TimedThreadPoolTaskExecutor}.
 *
 * @author David Held
 */
public class ThreadPoolTaskExecutorMetrics implements MeterBinder {
	/**
	 * Returns a new {@link ThreadPoolTaskExecutor} with recorded metrics.
	 *
	 * @param registry The registry to bind metrics to.
	 * @param name The name prefix of the metrics.
	 * @param tags Tags to apply to all recorded metrics.
	 * @return The instrumented executor, proxied.
	 */
	public static ThreadPoolTaskExecutor monitor(MeterRegistry registry, String name,
			Iterable<Tag> tags) {
		return new TimedThreadPoolTaskExecutor(registry, name, tags);
	}

	/**
	 * Returns a new {@link ThreadPoolTaskExecutor} with recorded metrics.
	 *
	 * @param registry The registry to bind metrics to.
	 * @param name The name prefix of the metrics.
	 * @param tags Tags to apply to all recorded metrics.
	 * @return The instrumented executor, proxied.
	 */
	public static Executor monitor(MeterRegistry registry, String name, Tag... tags) {
		return monitor(registry, name, Arrays.asList(tags));
	}

	private final ThreadPoolTaskExecutor executor;
	private final String name;
	private final Iterable<Tag> tags;

	public ThreadPoolTaskExecutorMetrics(ThreadPoolTaskExecutor executor, String name,
			Iterable<Tag> tags) {
		this.name = name;
		this.tags = tags;
		this.executor = executor;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		if (this.executor == null) {
			return;
		}
		monitor(registry, this.executor.getThreadPoolExecutor());
	}

	private void monitor(MeterRegistry registry, ThreadPoolExecutor tp) {
		FunctionCounter
				.builder(this.name + ".completed", tp,
						ThreadPoolExecutor::getCompletedTaskCount)
				.tags(this.tags)
				.description(
						"The approximate total number of tasks that have completed execution")
				.register(registry);

		Gauge.builder(this.name + ".active", tp, ThreadPoolExecutor::getActiveCount)
				.tags(this.tags)
				.description(
						"The approximate number of threads that are actively executing tasks")
				.register(registry);

		Gauge.builder(this.name + ".queued", tp, tpRef -> tpRef.getQueue().size())
				.tags(this.tags)
				.description(
						"The approximate number of threads that are queued for execution")
				.register(registry);

		Gauge.builder(this.name + ".pool", tp, ThreadPoolExecutor::getPoolSize)
				.tags(this.tags).description("The current number of threads in the pool")
				.register(registry);
	}
}
