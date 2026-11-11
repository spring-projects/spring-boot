/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.task;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Callback interface that can be used to customize a {@link ThreadPoolTaskExecutor}.
 *
 * @author Stephane Nicoll
 * @since 3.2.0
 * @see ThreadPoolTaskExecutorBuilder
 */
@FunctionalInterface
public interface ThreadPoolTaskExecutorCustomizer {

	/**
	 * Callback to customize a {@link ThreadPoolTaskExecutor} instance.
	 * @param taskExecutor the task executor to customize
	 */
	void customize(ThreadPoolTaskExecutor taskExecutor);

}
