/*
 * Copyright 2012-present the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskSchedulerRouter;

/**
 * Configuration that can be imported to expose a standard {@link TaskScheduler} if the
 * user has not enabled task scheduling explicitly. A {@link SimpleAsyncTaskScheduler} is
 * exposed if the user enables virtual threads via
 * {@code spring.threads.virtual.enabled=true}, otherwise {@link ThreadPoolTaskScheduler}.
 * <p>
 * Configurations importing this one should be ordered after
 * {@link TaskSchedulingAutoConfiguration}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(name = DefaultTaskSchedulerConfiguration.DEFAULT_TASK_SCHEDULER_BEAN_NAME)
public class DefaultTaskSchedulerConfiguration {

	/**
	 * The bean name of the default task scheduler.
	 */
	public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = TaskSchedulerRouter.DEFAULT_TASK_SCHEDULER_BEAN_NAME;

	@Bean(name = DEFAULT_TASK_SCHEDULER_BEAN_NAME)
	@ConditionalOnBean(ThreadPoolTaskSchedulerBuilder.class)
	@ConditionalOnThreading(Threading.PLATFORM)
	ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder) {
		return threadPoolTaskSchedulerBuilder.build();
	}

	@Bean(name = DEFAULT_TASK_SCHEDULER_BEAN_NAME)
	@ConditionalOnBean(SimpleAsyncTaskSchedulerBuilder.class)
	@ConditionalOnThreading(Threading.VIRTUAL)
	SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(
			SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder) {
		return simpleAsyncTaskSchedulerBuilder.build();
	}

}
