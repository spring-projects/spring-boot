/*
 * Copyright 2012-2024 the original author or authors.
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
 * @author Yanming Zhou
 */
class TaskExecutorConfigurations {

	/**
	 * TaskExecutorConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(Executor.class)
	@SuppressWarnings("removal")
	static class TaskExecutorConfiguration {

		/**
		 * Creates a SimpleAsyncTaskExecutor bean with virtual threads for executing tasks
		 * asynchronously. This bean is conditionally created based on the
		 * Threading.VIRTUAL condition.
		 * @param builder the SimpleAsyncTaskExecutorBuilder used to build the task
		 * executor
		 * @return the created SimpleAsyncTaskExecutor bean
		 */
		@Bean(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
				AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
			return builder.build();
		}

		/**
		 * Creates a ThreadPoolTaskExecutor bean for executing tasks asynchronously. The
		 * bean is named as either
		 * TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME or
		 * AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME. The creation
		 * of the bean is conditional on the Threading.PLATFORM condition.
		 * @param taskExecutorBuilder The builder for creating the ThreadPoolTaskExecutor.
		 * @param threadPoolTaskExecutorBuilderProvider The provider for the
		 * ThreadPoolTaskExecutorBuilder.
		 * @return The created ThreadPoolTaskExecutor bean.
		 */
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

	/**
	 * TaskExecutorBuilderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class TaskExecutorBuilderConfiguration {

		/**
		 * Creates a TaskExecutorBuilder bean if no other bean of the same type is
		 * present.
		 *
		 * This method is deprecated since version 3.2.0 and is scheduled for removal.
		 * @param properties the TaskExecutionProperties object containing the task
		 * execution properties
		 * @param taskExecutorCustomizers the ObjectProvider of TaskExecutorCustomizer
		 * objects
		 * @param taskDecorator the ObjectProvider of TaskDecorator objects
		 * @return a TaskExecutorBuilder object configured with the provided properties
		 * and customizers
		 */
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
			TaskExecutionProperties.Shutdown shutdown = properties.getShutdown();
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.customizers(taskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(taskDecorator.getIfUnique());
			return builder;
		}

	}

	/**
	 * ThreadPoolTaskExecutorBuilderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class ThreadPoolTaskExecutorBuilderConfiguration {

		/**
		 * Creates a {@link ThreadPoolTaskExecutorBuilder} bean if no other bean of type
		 * {@link TaskExecutorBuilder} or {@link ThreadPoolTaskExecutorBuilder} is present
		 * in the application context.
		 *
		 * The bean is configured based on the provided {@link TaskExecutionProperties}
		 * and other optional dependencies.
		 * @param properties the task execution properties
		 * @param threadPoolTaskExecutorCustomizers the customizers for the
		 * {@link ThreadPoolTaskExecutor}
		 * @param taskExecutorCustomizers the customizers for the {@link TaskExecutor}
		 * @param taskDecorator the task decorator
		 * @return the {@link ThreadPoolTaskExecutorBuilder} bean
		 */
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
			builder = builder.acceptTasksAfterContextClose(pool.getShutdown().isAcceptTasksAfterContextClose());
			TaskExecutionProperties.Shutdown shutdown = properties.getShutdown();
			builder = builder.awaitTermination(shutdown.isAwaitTermination());
			builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
			builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
			builder = builder.customizers(threadPoolTaskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(taskDecorator.getIfUnique());
			// Apply the deprecated TaskExecutorCustomizers, too
			builder = builder.additionalCustomizers(taskExecutorCustomizers.orderedStream().map(this::adapt).toList());
			return builder;
		}

		/**
		 * Adapts a TaskExecutorCustomizer to a ThreadPoolTaskExecutorCustomizer.
		 * @param customizer the TaskExecutorCustomizer to adapt
		 * @return the adapted ThreadPoolTaskExecutorCustomizer
		 */
		private ThreadPoolTaskExecutorCustomizer adapt(TaskExecutorCustomizer customizer) {
			return customizer::customize;
		}

	}

	/**
	 * SimpleAsyncTaskExecutorBuilderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	static class SimpleAsyncTaskExecutorBuilderConfiguration {

		private final TaskExecutionProperties properties;

		private final ObjectProvider<SimpleAsyncTaskExecutorCustomizer> taskExecutorCustomizers;

		private final ObjectProvider<TaskDecorator> taskDecorator;

		/**
		 * Constructs a new SimpleAsyncTaskExecutorBuilderConfiguration with the specified
		 * properties, task executor customizers, and task decorator.
		 * @param properties the properties for task execution
		 * @param taskExecutorCustomizers the customizers for the task executor
		 * @param taskDecorator the decorator for tasks
		 */
		SimpleAsyncTaskExecutorBuilderConfiguration(TaskExecutionProperties properties,
				ObjectProvider<SimpleAsyncTaskExecutorCustomizer> taskExecutorCustomizers,
				ObjectProvider<TaskDecorator> taskDecorator) {
			this.properties = properties;
			this.taskExecutorCustomizers = taskExecutorCustomizers;
			this.taskDecorator = taskDecorator;
		}

		/**
		 * Creates a SimpleAsyncTaskExecutorBuilder bean if no other bean of the same type
		 * is present in the application context. This bean is conditionally created based
		 * on the threading type being PLATFORM.
		 * @return the SimpleAsyncTaskExecutorBuilder bean
		 */
		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.PLATFORM)
		SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilder() {
			return builder();
		}

		/**
		 * Creates a SimpleAsyncTaskExecutorBuilder bean with the name
		 * "simpleAsyncTaskExecutorBuilder" if no other bean of the same type is present.
		 * This bean is conditionally created only if the threading type is set to
		 * virtual.
		 * @return The SimpleAsyncTaskExecutorBuilder bean with virtual threads enabled.
		 */
		@Bean(name = "simpleAsyncTaskExecutorBuilder")
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilderVirtualThreads() {
			SimpleAsyncTaskExecutorBuilder builder = builder();
			builder = builder.virtualThreads(true);
			return builder;
		}

		/**
		 * Returns a new instance of {@link SimpleAsyncTaskExecutorBuilder}.
		 * @return the {@link SimpleAsyncTaskExecutorBuilder} instance
		 */
		private SimpleAsyncTaskExecutorBuilder builder() {
			SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();
			builder = builder.threadNamePrefix(this.properties.getThreadNamePrefix());
			builder = builder.customizers(this.taskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(this.taskDecorator.getIfUnique());
			TaskExecutionProperties.Simple simple = this.properties.getSimple();
			builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
			TaskExecutionProperties.Shutdown shutdown = this.properties.getShutdown();
			if (shutdown.isAwaitTermination()) {
				builder = builder.taskTerminationTimeout(shutdown.getAwaitTerminationPeriod());
			}
			return builder;
		}

	}

}
