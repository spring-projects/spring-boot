/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.task;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Callback interface that can be used to customize a {@link ThreadPoolTaskScheduler}.
 *
 * @author Stephane Nicoll
 * @since 3.2.0
 */
@FunctionalInterface
public interface ThreadPoolTaskSchedulerCustomizer {

	/**
	 * Callback to customize a {@link ThreadPoolTaskScheduler} instance.
	 * @param taskScheduler the task scheduler to customize
	 */
	void customize(ThreadPoolTaskScheduler taskScheduler);

}
