/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.taskexecutionandscheduling.async

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

	@Bean
	fun asyncConfigurer(executorService: ExecutorService): AsyncConfigurer {
		return object : AsyncConfigurer {
			override fun getAsyncExecutor(): Executor {
				return executorService
			}
		}
	}

	@Bean
	fun executorService(): ExecutorService {
		return Executors.newCachedThreadPool()
	}

}
