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

import java.util.concurrent.Executor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties.Shutdown;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.task.SimpleAsyncTaskExecutorCustomizer;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@link TaskExecutor} configurations to be imported by
 * {@link TaskExecutionAutoConfiguration} in a specific order.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
class TaskExecutorConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(Executor.class)
	@SuppressWarnings("removal")
	static class TaskExecutorConfiguration {

		@Bean(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
				AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
			return builder.build();
		}

		@Lazy
		@Bean(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
				AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
		@ConditionalOnThreading(Threading.PLATFORM)
		ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder taskExecutorBuilder,
				ObjectProvider<ThreadPoolTaskExecutorBuilder> threadPoolTaskExecutorBuilderProvider) {
			ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder = threadPoolTaskExecutorBuilderProvider
				.getIfUnique();
			if (threadPoolTaskExecutorBuilder != null) {
				return threadPoolTaskExecutorBuilder.build();
			}
			return taskExecutorBuilder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class TaskExecutorBuilderConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@Deprecated(since = "3.2.0", forRemoval = true)
		TaskExecutorBuilder taskExecutorBuilder(TaskExecutionProperties properties,
				ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers,
				ObjectProvider<TaskDecorator> taskDecorator) {
			TaskExecutionProperties.Pool pool = properties.getPool();
			TaskExecutorBuilder builder = new TaskExecutorBuilder();
			builder = builder.queueCapacity(pool.getQueueCapacity());
			builder = builder.corePoolSize(pool.getCoreSize());
			builder = builder.maxPoolSize(pool.getMaxSize());
			builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
			builder = builder.keepAlive(pool.getKeepAlive());
			Shutdown shutdown = properties.getShutdown();
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.customizers(taskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(taskDecorator.getIfUnique());
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class ThreadPoolTaskExecutorBuilderConfiguration {

		@Bean
		@ConditionalOnMissingBean({ TaskExecutorBuilder.class, ThreadPoolTaskExecutorBuilder.class })
		ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder(TaskExecutionProperties properties,
				ObjectProvider<ThreadPoolTaskExecutorCustomizer> threadPoolTaskExecutorCustomizers,
				ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers,
				ObjectProvider<TaskDecorator> taskDecorator) {
			TaskExecutionProperties.Pool pool = properties.getPool();
			ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder();
			builder = builder.queueCapacity(pool.getQueueCapacity());
			builder = builder.corePoolSize(pool.getCoreSize());
			builder = builder.maxPoolSize(pool.getMaxSize());
			builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
			builder = builder.keepAlive(pool.getKeepAlive());
			Shutdown shutdown = properties.getShutdown();
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.customizers(threadPoolTaskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(taskDecorator.getIfUnique());
			// Apply the deprecated TaskExecutorCustomizers, too
			builder = builder.additionalCustomizers(taskExecutorCustomizers.orderedStream().map(this::adapt).toList());
			return builder;
		}

		private ThreadPoolTaskExecutorCustomizer adapt(TaskExecutorCustomizer customizer) {
			return customizer::customize;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SimpleAsyncTaskExecutorBuilderConfiguration {

		private final TaskExecutionProperties properties;

		private final ObjectProvider<SimpleAsyncTaskExecutorCustomizer> taskExecutorCustomizers;

		private final ObjectProvider<TaskDecorator> taskDecorator;

		SimpleAsyncTaskExecutorBuilderConfiguration(TaskExecutionProperties properties,
				ObjectProvider<SimpleAsyncTaskExecutorCustomizer> taskExecutorCustomizers,
				ObjectProvider<TaskDecorator> taskDecorator) {
			this.properties = properties;
			this.taskExecutorCustomizers = taskExecutorCustomizers;
			this.taskDecorator = taskDecorator;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.PLATFORM)
		SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilder() {
			return builder();
		}

		@Bean(name = "simpleAsyncTaskExecutorBuilder")
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilderVirtualThreads() {
			SimpleAsyncTaskExecutorBuilder builder = builder();
			builder = builder.virtualThreads(true);
			return builder;
		}

		private SimpleAsyncTaskExecutorBuilder builder() {
			SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();
			builder = builder.threadNamePrefix(this.properties.getThreadNamePrefix());
			builder = builder.customizers(this.taskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(this.taskDecorator.getIfUnique());
			TaskExecutionProperties.Simple simple = this.properties.getSimple();
			builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
			return builder;
		}

	}

}
