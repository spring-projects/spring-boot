/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.task;

import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

/**
 * Callback interface that can be used to customize a {@link SimpleAsyncTaskScheduler}.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@FunctionalInterface
public interface SimpleAsyncTaskSchedulerCustomizer {

	/**
	 * Callback to customize a {@link SimpleAsyncTaskScheduler} instance.
	 * @param taskScheduler the task scheduler to customize
	 */
	void customize(SimpleAsyncTaskScheduler taskScheduler);

}
