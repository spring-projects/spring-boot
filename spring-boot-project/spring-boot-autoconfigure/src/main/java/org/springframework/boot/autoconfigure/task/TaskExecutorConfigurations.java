/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.task.SimpleAsyncTaskExecutorCustomizer;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
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

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnExecutorCondition.class)
	@Import(AsyncConfigurerConfiguration.class)
	static class TaskExecutorConfiguration {

		@Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
		@ConditionalOnThreading(Threading.VIRTUAL)
		SimpleAsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
			return builder.build();
		}

		@Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
		@Lazy
		@ConditionalOnThreading(Threading.PLATFORM)
		ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder) {
			return threadPoolTaskExecutorBuilder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ThreadPoolTaskExecutorBuilderConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder(TaskExecutionProperties properties,
				ObjectProvider<ThreadPoolTaskExecutorCustomizer> threadPoolTaskExecutorCustomizers,
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
			return builder;
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
			return builder().virtualThreads(true);
		}

		private SimpleAsyncTaskExecutorBuilder builder() {
			SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();
			builder = builder.threadNamePrefix(this.properties.getThreadNamePrefix());
			builder = builder.customizers(this.taskExecutorCustomizers.orderedStream()::iterator);
			builder = builder.taskDecorator(this.taskDecorator.getIfUnique());
			TaskExecutionProperties.Simple simple = this.properties.getSimple();
			builder = builder.rejectTasksWhenLimitReached(simple.isRejectTasksWhenLimitReached());
			builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
			TaskExecutionProperties.Shutdown shutdown = this.properties.getShutdown();
			if (shutdown.isAwaitTermination()) {
				builder = builder.taskTerminationTimeout(shutdown.getAwaitTerminationPeriod());
			}
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(AsyncConfigurer.class)
	static class AsyncConfigurerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		AsyncConfigurer applicationTaskExecutorAsyncConfigurer(BeanFactory beanFactory) {
			return new AsyncConfigurer() {
				@Override
				public Executor getAsyncExecutor() {
					return beanFactory.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
							Executor.class);
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BootstrapExecutorConfiguration {

		@Bean
		static BeanFactoryPostProcessor bootstrapExecutorAliasPostProcessor() {
			return (beanFactory) -> {
				boolean hasBootstrapExecutor = beanFactory
					.containsBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
				boolean hasApplicationTaskExecutor = beanFactory
					.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
				if (!hasBootstrapExecutor && hasApplicationTaskExecutor) {
					beanFactory.registerAlias(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
							ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
				}
			};
		}

	}

	static class OnExecutorCondition extends AnyNestedCondition {

		OnExecutorCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingBean(Executor.class)
		private static final class ExecutorBeanCondition {

		}

		@ConditionalOnProperty(value = "spring.task.execution.mode", havingValue = "force")
		private static final class ModelCondition {

		}

	}

}
