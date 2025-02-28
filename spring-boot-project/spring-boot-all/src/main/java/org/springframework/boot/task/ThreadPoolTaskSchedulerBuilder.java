/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.task;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link ThreadPoolTaskScheduler}.
 * Provides convenience methods to set common {@link ThreadPoolTaskScheduler} settings.
 * For advanced configuration, consider using {@link ThreadPoolTaskSchedulerCustomizer}.
 * <p>
 * In a typical auto-configured Spring Boot application this builder is available as a
 * bean and can be injected whenever a {@link ThreadPoolTaskScheduler} is needed.
 *
 * @author Stephane Nicoll
 * @since 3.2.0
 */
public class ThreadPoolTaskSchedulerBuilder {

	private final Integer poolSize;

	private final Boolean awaitTermination;

	private final Duration awaitTerminationPeriod;

	private final String threadNamePrefix;

	private final TaskDecorator taskDecorator;

	private final Set<ThreadPoolTaskSchedulerCustomizer> customizers;

	public ThreadPoolTaskSchedulerBuilder() {
		this(null, null, null, null, null, null);
	}

	/**
	 * Constructs a new {@code ThreadPoolTaskSchedulerBuilder} instance with the specified
	 * configuration.
	 * @param poolSize the maximum allowed number of threads
	 * @param awaitTermination whether the executor should wait for scheduled tasks to
	 * complete on shutdown
	 * @param awaitTerminationPeriod the maximum time the executor is supposed to block on
	 * shutdown
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 * @param taskSchedulerCustomizers the customizers to apply to the
	 * {@link ThreadPoolTaskScheduler}
	 * @deprecated since 3.5.0 for removal in 3.7.0 in favor of the default constructor
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	public ThreadPoolTaskSchedulerBuilder(Integer poolSize, Boolean awaitTermination, Duration awaitTerminationPeriod,
			String threadNamePrefix, Set<ThreadPoolTaskSchedulerCustomizer> taskSchedulerCustomizers) {
		this(poolSize, awaitTermination, awaitTerminationPeriod, threadNamePrefix, null, taskSchedulerCustomizers);
	}

	private ThreadPoolTaskSchedulerBuilder(Integer poolSize, Boolean awaitTermination, Duration awaitTerminationPeriod,
			String threadNamePrefix, TaskDecorator taskDecorator,
			Set<ThreadPoolTaskSchedulerCustomizer> taskSchedulerCustomizers) {
		this.poolSize = poolSize;
		this.awaitTermination = awaitTermination;
		this.awaitTerminationPeriod = awaitTerminationPeriod;
		this.threadNamePrefix = threadNamePrefix;
		this.taskDecorator = taskDecorator;
		this.customizers = taskSchedulerCustomizers;
	}

	/**
	 * Set the maximum allowed number of threads.
	 * @param poolSize the pool size to set
	 * @return a new builder instance
	 */
	public ThreadPoolTaskSchedulerBuilder poolSize(int poolSize) {
		return new ThreadPoolTaskSchedulerBuilder(poolSize, this.awaitTermination, this.awaitTerminationPeriod,
				this.threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set whether the executor should wait for scheduled tasks to complete on shutdown,
	 * not interrupting running tasks and executing all tasks in the queue.
	 * @param awaitTermination whether the executor needs to wait for the tasks to
	 * complete on shutdown
	 * @return a new builder instance
	 * @see #awaitTerminationPeriod(Duration)
	 */
	public ThreadPoolTaskSchedulerBuilder awaitTermination(boolean awaitTermination) {
		return new ThreadPoolTaskSchedulerBuilder(this.poolSize, awaitTermination, this.awaitTerminationPeriod,
				this.threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set the maximum time the executor is supposed to block on shutdown. When set, the
	 * executor blocks on shutdown in order to wait for remaining tasks to complete their
	 * execution before the rest of the container continues to shut down. This is
	 * particularly useful if your remaining tasks are likely to need access to other
	 * resources that are also managed by the container.
	 * @param awaitTerminationPeriod the await termination period to set
	 * @return a new builder instance
	 */
	public ThreadPoolTaskSchedulerBuilder awaitTerminationPeriod(Duration awaitTerminationPeriod) {
		return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, awaitTerminationPeriod,
				this.threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set the prefix to use for the names of newly created threads.
	 * @param threadNamePrefix the thread name prefix to set
	 * @return a new builder instance
	 */
	public ThreadPoolTaskSchedulerBuilder threadNamePrefix(String threadNamePrefix) {
		return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
				threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set the {@link TaskDecorator} to be applied to the {@link ThreadPoolTaskScheduler}.
	 * @param taskDecorator the task decorator to set
	 * @return a new builder instance
	 * @since 3.5.0
	 */
	public ThreadPoolTaskSchedulerBuilder taskDecorator(TaskDecorator taskDecorator) {
		return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
				this.threadNamePrefix, taskDecorator, this.customizers);
	}

	/**
	 * Set the {@link ThreadPoolTaskSchedulerCustomizer
	 * threadPoolTaskSchedulerCustomizers} that should be applied to the
	 * {@link ThreadPoolTaskScheduler}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied. Setting this value will
	 * replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(ThreadPoolTaskSchedulerCustomizer...)
	 */
	public ThreadPoolTaskSchedulerBuilder customizers(ThreadPoolTaskSchedulerCustomizer... customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set the {@link ThreadPoolTaskSchedulerCustomizer
	 * threadPoolTaskSchedulerCustomizers} that should be applied to the
	 * {@link ThreadPoolTaskScheduler}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied. Setting this value will
	 * replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(ThreadPoolTaskSchedulerCustomizer...)
	 */
	public ThreadPoolTaskSchedulerBuilder customizers(
			Iterable<? extends ThreadPoolTaskSchedulerCustomizer> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
				this.threadNamePrefix, this.taskDecorator, append(null, customizers));
	}

	/**
	 * Add {@link ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizers}
	 * that should be applied to the {@link ThreadPoolTaskScheduler}. Customizers are
	 * applied in the order that they were added after builder configuration has been
	 * applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(ThreadPoolTaskSchedulerCustomizer...)
	 */
	public ThreadPoolTaskSchedulerBuilder additionalCustomizers(ThreadPoolTaskSchedulerCustomizer... customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add {@link ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizers}
	 * that should be applied to the {@link ThreadPoolTaskScheduler}. Customizers are
	 * applied in the order that they were added after builder configuration has been
	 * applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(ThreadPoolTaskSchedulerCustomizer...)
	 */
	public ThreadPoolTaskSchedulerBuilder additionalCustomizers(
			Iterable<? extends ThreadPoolTaskSchedulerCustomizer> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
				this.threadNamePrefix, this.taskDecorator, append(this.customizers, customizers));
	}

	/**
	 * Build a new {@link ThreadPoolTaskScheduler} instance and configure it using this
	 * builder.
	 * @return a configured {@link ThreadPoolTaskScheduler} instance.
	 * @see #configure(ThreadPoolTaskScheduler)
	 */
	public ThreadPoolTaskScheduler build() {
		return configure(new ThreadPoolTaskScheduler());
	}

	/**
	 * Configure the provided {@link ThreadPoolTaskScheduler} instance using this builder.
	 * @param <T> the type of task scheduler
	 * @param taskScheduler the {@link ThreadPoolTaskScheduler} to configure
	 * @return the task scheduler instance
	 * @see #build()
	 */
	public <T extends ThreadPoolTaskScheduler> T configure(T taskScheduler) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.poolSize).to(taskScheduler::setPoolSize);
		map.from(this.awaitTermination).to(taskScheduler::setWaitForTasksToCompleteOnShutdown);
		map.from(this.awaitTerminationPeriod).asInt(Duration::getSeconds).to(taskScheduler::setAwaitTerminationSeconds);
		map.from(this.threadNamePrefix).to(taskScheduler::setThreadNamePrefix);
		map.from(this.taskDecorator).to(taskScheduler::setTaskDecorator);
		if (!CollectionUtils.isEmpty(this.customizers)) {
			this.customizers.forEach((customizer) -> customizer.customize(taskScheduler));
		}
		return taskScheduler;
	}

	private <T> Set<T> append(Set<T> set, Iterable<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
		additions.forEach(result::add);
		return Collections.unmodifiableSet(result);
	}

}
