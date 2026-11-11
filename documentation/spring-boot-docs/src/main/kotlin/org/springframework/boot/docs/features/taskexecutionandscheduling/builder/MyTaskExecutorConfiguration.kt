/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.taskexecutionandscheduling.builder

import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

	@Bean
	fun taskExecutor(builder: SimpleAsyncTaskExecutorBuilder): SimpleAsyncTaskExecutor {
		return builder.build()
	}

}
