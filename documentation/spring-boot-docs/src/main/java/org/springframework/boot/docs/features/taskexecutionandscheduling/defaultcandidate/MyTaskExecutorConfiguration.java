/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.taskexecutionandscheduling.defaultcandidate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

	@Bean(defaultCandidate = false)
	@Qualifier("scheduledExecutorService")
	ScheduledExecutorService scheduledExecutorService() {
		return Executors.newSingleThreadScheduledExecutor();
	}

}
