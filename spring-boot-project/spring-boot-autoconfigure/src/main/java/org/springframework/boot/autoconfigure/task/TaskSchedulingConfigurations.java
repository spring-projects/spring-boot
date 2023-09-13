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

package org.springframework.boot.autoconfigure.task;

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * {@link TaskScheduler} configurations to be imported by
 * {@link TaskSchedulingAutoConfiguration} in a specific order.
 *
 * @author Moritz Halbritter
 */
class TaskSchedulingConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
	@ConditionalOnMissingBean({ TaskScheduler.class, ScheduledExecutorService.class })
	@SuppressWarnings("removal")
	static class TaskSchedulerConfiguration {

		@Bean(name = "taskScheduler")
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(SimpleAsyncTaskSchedulerBuilder builder) {
			return builder.build();
		}

		@Bean
		@ConditionalOnThreading(Threading.PLATFORM)
		ThreadPoolTaskScheduler taskScheduler(TaskSchedulerBuilder taskSchedulerBuilder,
				ObjectProvider<ThreadPoolTaskSchedulerBuilder> threadPoolTaskSchedulerBuilderProvider) {
			ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder = threadPoolTaskSchedulerBuilderProvider
				.getIfUnique();
			if (threadPoolTaskSchedulerBuilder != null) {
				return threadPoolTaskSchedulerBuilder.build();
			}
			return taskSchedulerBuilder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class TaskSchedulerBuilderConfiguration {

		@Bean
		@ConditionalOnMissingBean
		TaskSchedulerBuilder taskSchedulerBuilder(TaskSchedulingProperties properties,
				ObjectProvider<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
			TaskSchedulerBuilder builder = new TaskSchedulerBuilder();
			builder = builder.poolSize(properties.getPool().getSize());
			TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.customizers(taskSchedulerCustomizers);
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class ThreadPoolTaskSchedulerBuilderConfiguration {

		@Bean
		@ConditionalOnMissingBean({ TaskSchedulerBuilder.class, ThreadPoolTaskSchedulerBuilder.class })
		ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder(TaskSchedulingProperties properties,
				ObjectProvider<ThreadPoolTaskSchedulerCustomizer> threadPoolTaskSchedulerCustomizers,
				ObjectProvider<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
			TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
			ThreadPoolTaskSchedulerBuilder builder = new ThreadPoolTaskSchedulerBuilder();
			builder = builder.poolSize(properties.getPool().getSize());
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.customizers(threadPoolTaskSchedulerCustomizers);
			// Apply the deprecated TaskSchedulerCustomizers, too
			builder = builder.additionalCustomizers(taskSchedulerCustomizers.orderedStream().map(this::adapt).toList());
			return builder;
		}

		private ThreadPoolTaskSchedulerCustomizer adapt(TaskSchedulerCustomizer customizer) {
			return customizer::customize;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleAsyncTaskSchedulerBuilderConfiguration {

		private final TaskSchedulingProperties properties;

		private final ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers;

		SimpleAsyncTaskSchedulerBuilderConfiguration(TaskSchedulingProperties properties,
				ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
			this.properties = properties;
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
			SimpleAsyncTaskSchedulerBuilder builder = builder();
			builder = builder.virtualThreads(true);
			return builder;
		}

		private SimpleAsyncTaskSchedulerBuilder builder() {
			SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();
			builder = builder.threadNamePrefix(this.properties.getThreadNamePrefix());
			builder = builder.customizers(this.taskSchedulerCustomizers.orderedStream()::iterator);
			TaskSchedulingProperties.Simple simple = this.properties.getSimple();
			builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
			return builder;
		}

	}

}
