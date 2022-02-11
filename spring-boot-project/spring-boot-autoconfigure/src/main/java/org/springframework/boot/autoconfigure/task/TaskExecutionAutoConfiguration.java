/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties.Shutdown;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskExecutor}.
 *
 * @author Stephane Nicoll
 * @author Camille Vienot
 * @since 2.1.0
 */
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@AutoConfiguration
@EnableConfigurationProperties(TaskExecutionProperties.class)
public class TaskExecutionAutoConfiguration {

	/**
	 * Bean name of the application {@link TaskExecutor}.
	 */
	public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

	@Bean
	@ConditionalOnMissingBean
	public TaskExecutorBuilder taskExecutorBuilder(TaskExecutionProperties properties,
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

	@Lazy
	@Bean(name = { APPLICATION_TASK_EXECUTOR_BEAN_NAME,
			AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
	@ConditionalOnMissingBean(Executor.class)
	public ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
		return builder.build();
	}

}
