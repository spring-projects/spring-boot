/*
 * Copyright 2012-present the original author or authors.
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
