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

package org.springframework.boot.docs.features.taskexecutionandscheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class TaskExecutionConfigurationExamples {

	// tag::default-candidate-task-executor[]
	@Bean(defaultCandidate = false)
	@Qualifier("scheduledExecutorService")
	ScheduledExecutorService scheduledExecutorService() {
		return Executors.newSingleThreadScheduledExecutor();
	}
	// end::default-candidate-task-executor[]

	// tag::application-task-executor[]
	@Bean("applicationTaskExecutor")
	SimpleAsyncTaskExecutor applicationTaskExecutor() {
		return new SimpleAsyncTaskExecutor("app-");
	}
	// end::application-task-executor[]

	// tag::executor-builder[]
	@Bean
	SimpleAsyncTaskExecutor taskExecutor(SimpleAsyncTaskExecutorBuilder builder) {
		return builder.build();
	}
	// end::executor-builder[]

	static class MultipleTaskExecutor {

		// tag::multiple-task-executor[]
		@Bean("applicationTaskExecutor")
		SimpleAsyncTaskExecutor applicationTaskExecutor() {
			return new SimpleAsyncTaskExecutor("app-");
		}

		@Bean("taskExecutor")
		ThreadPoolTaskExecutor taskExecutor() {
			ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
			threadPoolTaskExecutor.setThreadNamePrefix("async-");
			return threadPoolTaskExecutor;
		}
		// end::multiple-task-executor[]

	}

	// tag::async-configurer[]
	@Configuration(proxyBeanMethods = false)
	public class TaskExecutionConfiguration {

		@Bean
		AsyncConfigurer asyncConfigurer(ExecutorService executorService) {
			return new AsyncConfigurer() {

				@Override
				public Executor getAsyncExecutor() {
					return executorService;
				}

			};
		}

		@Bean
		ExecutorService executorService() {
			return Executors.newCachedThreadPool();
		}

	}
	// end::async-configurer[]

}
