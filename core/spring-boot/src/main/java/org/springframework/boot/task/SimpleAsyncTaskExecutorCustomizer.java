/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.task;

import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Callback interface that can be used to customize a {@link SimpleAsyncTaskExecutor}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 3.2.0
 * @see SimpleAsyncTaskExecutorBuilder
 */
@FunctionalInterface
public interface SimpleAsyncTaskExecutorCustomizer {

	/**
	 * Callback to customize a {@link SimpleAsyncTaskExecutor} instance.
	 * @param taskExecutor the task executor to customize
	 */
	void customize(SimpleAsyncTaskExecutor taskExecutor);

}
