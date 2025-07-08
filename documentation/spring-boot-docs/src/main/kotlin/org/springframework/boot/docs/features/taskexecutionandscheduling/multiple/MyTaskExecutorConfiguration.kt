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

package org.springframework.boot.docs.features.taskexecutionandscheduling.multiple

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor


@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

	@Bean("applicationTaskExecutor")
	fun applicationTaskExecutor(): SimpleAsyncTaskExecutor {
		return SimpleAsyncTaskExecutor("app-")
	}

	@Bean("taskExecutor")
	fun taskExecutor(): ThreadPoolTaskExecutor {
		val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
		threadPoolTaskExecutor.setThreadNamePrefix("async-")
		return threadPoolTaskExecutor
	}

}
