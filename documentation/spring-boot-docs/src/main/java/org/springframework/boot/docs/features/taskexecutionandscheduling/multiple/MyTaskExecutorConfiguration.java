/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.taskexecutionandscheduling.multiple;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

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

}
