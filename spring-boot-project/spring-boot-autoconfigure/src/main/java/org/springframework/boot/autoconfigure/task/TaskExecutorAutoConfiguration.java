/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskExecutor}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@Configuration
@EnableConfigurationProperties(TaskProperties.class)
public class TaskExecutorAutoConfiguration {

	/**
	 * Bean name of the application {@link TaskExecutor}.
	 */
	public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

	private final TaskProperties properties;

	private final ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers;

	private final ObjectProvider<TaskDecorator> taskDecorator;

	public TaskExecutorAutoConfiguration(TaskProperties properties,
			ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers,
			ObjectProvider<TaskDecorator> taskDecorator) {
		this.properties = properties;
		this.taskExecutorCustomizers = taskExecutorCustomizers;
		this.taskDecorator = taskDecorator;
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskExecutorBuilder taskExecutorBuilder() {
		TaskProperties.Pool pool = this.properties.getPool();
		return new TaskExecutorBuilder().queueCapacity(pool.getQueueCapacity())
				.corePoolSize(pool.getCoreSize()).maxPoolSize(pool.getMaxSize())
				.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout())
				.keepAlive(pool.getKeepAlive())
				.threadNamePrefix(this.properties.getThreadNamePrefix())
				.customizers(this.taskExecutorCustomizers.stream()
						.collect(Collectors.toList()))
				.taskDecorator(this.taskDecorator.getIfUnique());
	}

	@Bean(name = APPLICATION_TASK_EXECUTOR_BEAN_NAME)
	@ConditionalOnMissingBean(Executor.class)
	public ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
		return builder.build();
	}

}
