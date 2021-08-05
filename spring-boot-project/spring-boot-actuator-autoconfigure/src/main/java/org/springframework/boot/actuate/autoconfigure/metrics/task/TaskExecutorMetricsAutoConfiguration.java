/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ThreadPoolTaskExecutor task executors} and {@link ThreadPoolTaskScheduler task
 * schedulers}.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
		TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class })
@ConditionalOnClass(ExecutorServiceMetrics.class)
@ConditionalOnBean({ Executor.class, MeterRegistry.class })
public class TaskExecutorMetricsAutoConfiguration {

	private static final String TASK_EXECUTOR_SUFFIX = "taskExecutor";

	private static final String TASK_SCHEDULER_SUFFIX = "taskScheduler";

	@Autowired
	public void bindTaskExecutorsToRegistry(Map<String, Executor> executors, MeterRegistry registry) {
		executors.forEach((beanName, executor) -> {
			if (executor instanceof ThreadPoolTaskExecutor) {
				monitor(registry, safeGetThreadPoolExecutor((ThreadPoolTaskExecutor) executor),
						() -> getTaskExecutorName(beanName));
			}
			else if (executor instanceof ThreadPoolTaskScheduler) {
				monitor(registry, safeGetThreadPoolExecutor((ThreadPoolTaskScheduler) executor),
						() -> getTaskSchedulerName(beanName));
			}
		});
	}

	private void monitor(MeterRegistry registry, ThreadPoolExecutor threadPoolExecutor, Supplier<String> beanName) {
		if (threadPoolExecutor != null) {
			ExecutorServiceMetrics.monitor(registry, threadPoolExecutor, beanName.get());
		}
	}

	private ThreadPoolExecutor safeGetThreadPoolExecutor(ThreadPoolTaskExecutor taskExecutor) {
		try {
			return taskExecutor.getThreadPoolExecutor();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	private ThreadPoolExecutor safeGetThreadPoolExecutor(ThreadPoolTaskScheduler taskScheduler) {
		try {
			return taskScheduler.getScheduledThreadPoolExecutor();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * Get the name of a {@link ThreadPoolTaskExecutor} based on its {@code beanName}.
	 * @param beanName the name of the {@link ThreadPoolTaskExecutor} bean
	 * @return a name for the given task executor
	 */
	private String getTaskExecutorName(String beanName) {
		if (beanName.length() > TASK_EXECUTOR_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, TASK_EXECUTOR_SUFFIX)) {
			return beanName.substring(0, beanName.length() - TASK_EXECUTOR_SUFFIX.length());
		}
		return beanName;
	}

	/**
	 * Get the name of a {@link ThreadPoolTaskScheduler} based on its {@code beanName}.
	 * @param beanName the name of the {@link ThreadPoolTaskScheduler} bean
	 * @return a name for the given task scheduler
	 */
	private String getTaskSchedulerName(String beanName) {
		if (beanName.equals(TASK_SCHEDULER_SUFFIX)) {
			return "application";
		}
		if (beanName.length() > TASK_SCHEDULER_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, TASK_SCHEDULER_SUFFIX)) {
			return beanName.substring(0, beanName.length() - TASK_SCHEDULER_SUFFIX.length());
		}
		return beanName;
	}

}
