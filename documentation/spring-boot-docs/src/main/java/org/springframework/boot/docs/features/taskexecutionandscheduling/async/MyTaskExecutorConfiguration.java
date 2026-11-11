/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.taskexecutionandscheduling.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

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
