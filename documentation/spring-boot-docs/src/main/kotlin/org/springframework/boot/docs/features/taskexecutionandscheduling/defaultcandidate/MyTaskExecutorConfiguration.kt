/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.taskexecutionandscheduling.defaultcandidate

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

	@Bean(defaultCandidate = false)
	@Qualifier("scheduledExecutorService")
	fun scheduledExecutorService(): ScheduledExecutorService {
		return Executors.newSingleThreadScheduledExecutor()
	}

}
