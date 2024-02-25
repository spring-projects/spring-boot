/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.task;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ThreadPoolTaskExecutor task executors} and {@link ThreadPoolTaskScheduler task
 * schedulers}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.6.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
		TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class })
@ConditionalOnClass(ExecutorServiceMetrics.class)
@ConditionalOnBean({ Executor.class, MeterRegistry.class })
public class TaskExecutorMetricsAutoConfiguration {

	/**
	 * Binds the task executors to the registry for monitoring purposes.
	 * @param executors the map of executor bean names to executor instances
	 * @param registry the meter registry for monitoring
	 */
	@Autowired
	public void bindTaskExecutorsToRegistry(Map<String, Executor> executors, MeterRegistry registry) {
		executors.forEach((beanName, executor) -> {
			if (executor instanceof ThreadPoolTaskExecutor threadPoolTaskExecutor) {
				monitor(registry, safeGetThreadPoolExecutor(threadPoolTaskExecutor), beanName);
			}
			else if (executor instanceof ThreadPoolTaskScheduler threadPoolTaskScheduler) {
				monitor(registry, safeGetThreadPoolExecutor(threadPoolTaskScheduler), beanName);
			}
		});
	}

	/**
	 * Returns a LazyInitializationExcludeFilter for the
	 * TaskExecutorMetricsAutoConfiguration class. This filter excludes the
	 * TaskExecutorMetricsAutoConfiguration class from being eagerly initialized.
	 * @return the LazyInitializationExcludeFilter for the
	 * TaskExecutorMetricsAutoConfiguration class
	 */
	@Bean
	static LazyInitializationExcludeFilter eagerTaskExecutorMetrics() {
		return LazyInitializationExcludeFilter.forBeanTypes(TaskExecutorMetricsAutoConfiguration.class);
	}

	/**
	 * Monitors the given ThreadPoolExecutor and binds the metrics to the provided
	 * MeterRegistry.
	 * @param registry the MeterRegistry to bind the metrics to
	 * @param threadPoolExecutor the ThreadPoolExecutor to monitor
	 * @param name the name of the ThreadPoolExecutor
	 */
	private void monitor(MeterRegistry registry, ThreadPoolExecutor threadPoolExecutor, String name) {
		if (threadPoolExecutor != null) {
			new ExecutorServiceMetrics(threadPoolExecutor, name, Collections.emptyList()).bindTo(registry);
		}
	}

	/**
	 * Safely retrieves the underlying ThreadPoolExecutor from the given
	 * ThreadPoolTaskExecutor.
	 * @param taskExecutor the ThreadPoolTaskExecutor to retrieve the ThreadPoolExecutor
	 * from
	 * @return the ThreadPoolExecutor if it exists, or null if an IllegalStateException
	 * occurs
	 */
	private ThreadPoolExecutor safeGetThreadPoolExecutor(ThreadPoolTaskExecutor taskExecutor) {
		try {
			return taskExecutor.getThreadPoolExecutor();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * Safely retrieves the underlying ThreadPoolExecutor from the given
	 * ThreadPoolTaskScheduler.
	 * @param taskScheduler the ThreadPoolTaskScheduler to retrieve the ThreadPoolExecutor
	 * from
	 * @return the ThreadPoolExecutor if it exists, or null if an IllegalStateException
	 * occurs
	 */
	private ThreadPoolExecutor safeGetThreadPoolExecutor(ThreadPoolTaskScheduler taskScheduler) {
		try {
			return taskScheduler.getScheduledThreadPoolExecutor();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

}
