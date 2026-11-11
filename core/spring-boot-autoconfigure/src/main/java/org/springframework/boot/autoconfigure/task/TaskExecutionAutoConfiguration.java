/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.task;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskExecutor}.
 *
 * @author Stephane Nicoll
 * @author Camille Vienot
 * @author Moritz Halbritter
 * @since 2.1.0
 */
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@AutoConfiguration
@EnableConfigurationProperties(TaskExecutionProperties.class)
@Import({ TaskExecutorConfigurations.TaskExecutorContextPropagationConfiguration.class,
		TaskExecutorConfigurations.ThreadPoolTaskExecutorBuilderConfiguration.class,
		TaskExecutorConfigurations.SimpleAsyncTaskExecutorBuilderConfiguration.class,
		TaskExecutorConfigurations.TaskExecutorConfiguration.class,
		TaskExecutorConfigurations.BootstrapExecutorConfiguration.class })
public final class TaskExecutionAutoConfiguration {

	/**
	 * Bean name of the application {@link TaskExecutor}.
	 */
	public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

}
