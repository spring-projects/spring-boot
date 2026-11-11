/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.task;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * {@link TaskScheduler} configurations to be imported by
 * {@link TaskSchedulingAutoConfiguration} in a specific order.
 *
 * @author Moritz Halbritter
 */
class TaskSchedulingConfigurations {

	private static @Nullable TaskDecorator getTaskDecorator(ObjectProvider<TaskDecorator> taskDecorator) {
		List<TaskDecorator> taskDecorators = taskDecorator.orderedStream().toList();
		if (taskDecorators.size() == 1) {
			return taskDecorators.get(0);
		}
		return (!taskDecorators.isEmpty()) ? new CompositeTaskDecorator(taskDecorators) : null;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
	@ConditionalOnMissingBean({ TaskScheduler.class, ScheduledExecutorService.class })
	@Import(DefaultTaskSchedulerConfiguration.class)
	static class TaskSchedulerConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ThreadPoolTaskSchedulerBuilderConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder(TaskSchedulingProperties properties,
				ObjectProvider<TaskDecorator> taskDecorator,
				ObjectProvider<ThreadPoolTaskSchedulerCustomizer> threadPoolTaskSchedulerCustomizers) {
			TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
			ThreadPoolTaskSchedulerBuilder builder = new ThreadPoolTaskSchedulerBuilder();
			builder = builder.poolSize(properties.getPool().getSize());
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.taskDecorator(getTaskDecorator(taskDecorator));
			builder = builder.customizers(threadPoolTaskSchedulerCustomizers);
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleAsyncTaskSchedulerBuilderConfiguration {

		private final TaskSchedulingProperties properties;

		private final ObjectProvider<TaskDecorator> taskDecorator;

		private final ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers;

		SimpleAsyncTaskSchedulerBuilderConfiguration(TaskSchedulingProperties properties,
				ObjectProvider<TaskDecorator> taskDecorator,
				ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
			this.properties = properties;
			this.taskDecorator = taskDecorator;
			this.taskSchedulerCustomizers = taskSchedulerCustomizers;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.PLATFORM)
		SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder() {
			return builder();
		}

		@Bean(name = "simpleAsyncTaskSchedulerBuilder")
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilderVirtualThreads() {
			return builder().virtualThreads(true);
		}

		private SimpleAsyncTaskSchedulerBuilder builder() {
			SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();
			builder = builder.threadNamePrefix(this.properties.getThreadNamePrefix());
			builder = builder.taskDecorator(getTaskDecorator(this.taskDecorator));
			builder = builder.customizers(this.taskSchedulerCustomizers.orderedStream()::iterator);
			TaskSchedulingProperties.Simple simple = this.properties.getSimple();
			builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
			TaskSchedulingProperties.Shutdown shutdown = this.properties.getShutdown();
			if (shutdown.isAwaitTermination()) {
				builder = builder.taskTerminationTimeout(shutdown.getAwaitTerminationPeriod());
			}
			return builder;
		}

	}

}
