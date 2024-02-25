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

	/**
     * TaskSchedulerConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
	@ConditionalOnMissingBean({ TaskScheduler.class, ScheduledExecutorService.class })
	@SuppressWarnings("removal")
	static class TaskSchedulerConfiguration {

		/**
         * Creates a task scheduler bean with the name "taskScheduler" and is conditionally enabled based on the threading type being virtual.
         * 
         * @param builder the builder used to construct the task scheduler
         * @return the created task scheduler bean
         */
        @Bean(name = "taskScheduler")
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(SimpleAsyncTaskSchedulerBuilder builder) {
			return builder.build();
		}

		/**
         * Creates a ThreadPoolTaskScheduler bean if the threading is set to PLATFORM.
         * 
         * @param taskSchedulerBuilder The TaskSchedulerBuilder used to build the task scheduler.
         * @param threadPoolTaskSchedulerBuilderProvider The provider for the ThreadPoolTaskSchedulerBuilder.
         * @return The ThreadPoolTaskScheduler bean.
         */
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

	/**
     * TaskSchedulerBuilderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class TaskSchedulerBuilderConfiguration {

		/**
         * Configure the TaskSchedulerBuilder based on the provided properties and customizers.
         * If no customizers are provided, the default configuration will be used.
         * 
         * @param properties the TaskSchedulingProperties containing the configuration properties
         * @param taskSchedulerCustomizers the customizers to apply to the TaskSchedulerBuilder
         * @return the configured TaskSchedulerBuilder
         */
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

	/**
     * ThreadPoolTaskSchedulerBuilderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class ThreadPoolTaskSchedulerBuilderConfiguration {

		/**
         * Creates a {@link ThreadPoolTaskSchedulerBuilder} bean if no other bean of type {@link TaskSchedulerBuilder} or {@link ThreadPoolTaskSchedulerBuilder} is present.
         * 
         * The {@link ThreadPoolTaskSchedulerBuilder} is used to configure a {@link ThreadPoolTaskScheduler} for task scheduling.
         * 
         * The configuration properties for the task scheduler are obtained from the {@link TaskSchedulingProperties} bean.
         * 
         * The {@link ThreadPoolTaskSchedulerBuilder} is customized with any available {@link ThreadPoolTaskSchedulerCustomizer} beans and deprecated {@link TaskSchedulerCustomizer} beans.
         * 
         * @param properties the task scheduling properties
         * @param threadPoolTaskSchedulerCustomizers the thread pool task scheduler customizers
         * @param taskSchedulerCustomizers the task scheduler customizers
         * @return the configured {@link ThreadPoolTaskSchedulerBuilder}
         */
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

		/**
         * Adapts a {@link TaskSchedulerCustomizer} to a {@link ThreadPoolTaskSchedulerCustomizer}.
         * 
         * @param customizer the {@link TaskSchedulerCustomizer} to adapt
         * @return the adapted {@link ThreadPoolTaskSchedulerCustomizer}
         */
        private ThreadPoolTaskSchedulerCustomizer adapt(TaskSchedulerCustomizer customizer) {
			return customizer::customize;
		}

	}

	/**
     * SimpleAsyncTaskSchedulerBuilderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	static class SimpleAsyncTaskSchedulerBuilderConfiguration {

		private final TaskSchedulingProperties properties;

		private final ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers;

		/**
         * Constructs a new SimpleAsyncTaskSchedulerBuilderConfiguration with the specified properties and task scheduler customizers.
         * 
         * @param properties the task scheduling properties to be used
         * @param taskSchedulerCustomizers the customizers to be applied to the task scheduler
         */
        SimpleAsyncTaskSchedulerBuilderConfiguration(TaskSchedulingProperties properties,
				ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
			this.properties = properties;
			this.taskSchedulerCustomizers = taskSchedulerCustomizers;
		}

		/**
         * Creates a SimpleAsyncTaskSchedulerBuilder bean if no other bean of the same type is present in the application context.
         * This bean is conditionally created only if the threading mode is set to PLATFORM.
         * 
         * @return the SimpleAsyncTaskSchedulerBuilder bean
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.PLATFORM)
		SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder() {
			return builder();
		}

		/**
         * Configures a SimpleAsyncTaskSchedulerBuilder bean with the name "simpleAsyncTaskSchedulerBuilderVirtualThreads".
         * This bean is conditionally created if there is no existing bean of the same type.
         * It is also conditionally created if the threading type is set to virtual.
         * 
         * @return The configured SimpleAsyncTaskSchedulerBuilder bean.
         */
        @Bean(name = "simpleAsyncTaskSchedulerBuilder")
		@ConditionalOnMissingBean
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilderVirtualThreads() {
			SimpleAsyncTaskSchedulerBuilder builder = builder();
			builder = builder.virtualThreads(true);
			return builder;
		}

		/**
         * Returns a builder for creating a SimpleAsyncTaskScheduler.
         * 
         * @return the SimpleAsyncTaskSchedulerBuilder
         */
        private SimpleAsyncTaskSchedulerBuilder builder() {
			SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();
			builder = builder.threadNamePrefix(this.properties.getThreadNamePrefix());
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
