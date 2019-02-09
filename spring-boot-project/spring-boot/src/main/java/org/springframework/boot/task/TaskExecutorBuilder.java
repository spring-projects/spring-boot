/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link TaskExecutor}. Provides
 * convenience methods to set common {@link ThreadPoolTaskExecutor} settings and register
 * {@link #taskDecorator(TaskDecorator)}). For advanced configuration, consider using
 * {@link TaskExecutorCustomizer}.
 * <p>
 * In a typical auto-configured Spring Boot application this builder is available as a
 * bean and can be injected whenever a {@link TaskExecutor} is needed.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class TaskExecutorBuilder {

	private final Integer queueCapacity;

	private final Integer corePoolSize;

	private final Integer maxPoolSize;

	private final Boolean allowCoreThreadTimeOut;

	private final Duration keepAlive;

	private final String threadNamePrefix;

	private final TaskDecorator taskDecorator;

	private final Set<TaskExecutorCustomizer> customizers;

	public TaskExecutorBuilder() {
		this.queueCapacity = null;
		this.corePoolSize = null;
		this.maxPoolSize = null;
		this.allowCoreThreadTimeOut = null;
		this.keepAlive = null;
		this.threadNamePrefix = null;
		this.taskDecorator = null;
		this.customizers = null;
	}

	private TaskExecutorBuilder(Integer queueCapacity, Integer corePoolSize,
			Integer maxPoolSize, Boolean allowCoreThreadTimeOut, Duration keepAlive,
			String threadNamePrefix, TaskDecorator taskDecorator,
			Set<TaskExecutorCustomizer> customizers) {
		this.queueCapacity = queueCapacity;
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
		this.keepAlive = keepAlive;
		this.threadNamePrefix = threadNamePrefix;
		this.taskDecorator = taskDecorator;
		this.customizers = customizers;
	}

	/**
	 * Set the capacity of the queue. An unbounded capacity does not increase the pool and
	 * therefore ignores {@link #maxPoolSize(int) maxPoolSize}.
	 * @param queueCapacity the queue capacity to set
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder queueCapacity(int queueCapacity) {
		return new TaskExecutorBuilder(queueCapacity, this.corePoolSize, this.maxPoolSize,
				this.allowCoreThreadTimeOut, this.keepAlive, this.threadNamePrefix,
				this.taskDecorator, this.customizers);
	}

	/**
	 * Set the core number of threads. Effectively that maximum number of threads as long
	 * as the queue is not full.
	 * <p>
	 * Core threads can grow and shrink if {@link #allowCoreThreadTimeOut(boolean)} is
	 * enabled.
	 * @param corePoolSize the core pool size to set
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder corePoolSize(int corePoolSize) {
		return new TaskExecutorBuilder(this.queueCapacity, corePoolSize, this.maxPoolSize,
				this.allowCoreThreadTimeOut, this.keepAlive, this.threadNamePrefix,
				this.taskDecorator, this.customizers);
	}

	/**
	 * Set the maximum allowed number of threads. When the {@link #queueCapacity(int)
	 * queue} is full, the pool can expand up to that size to accommodate the load.
	 * <p>
	 * If the {@link #queueCapacity(int) queue capacity} is unbounded, this setting is
	 * ignored.
	 * @param maxPoolSize the max pool size to set
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder maxPoolSize(int maxPoolSize) {
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize, maxPoolSize,
				this.allowCoreThreadTimeOut, this.keepAlive, this.threadNamePrefix,
				this.taskDecorator, this.customizers);
	}

	/**
	 * Set whether core threads are allow to time out. When enabled, this enables dynamic
	 * growing and shrinking of the pool.
	 * @param allowCoreThreadTimeOut if core threads are allowed to time out
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize,
				this.maxPoolSize, allowCoreThreadTimeOut, this.keepAlive,
				this.threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set the time limit for which threads may remain idle before being terminated.
	 * @param keepAlive the keep alive to set
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder keepAlive(Duration keepAlive) {
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize,
				this.maxPoolSize, this.allowCoreThreadTimeOut, keepAlive,
				this.threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set the prefix to use for the names of newly created threads.
	 * @param threadNamePrefix the thread name prefix to set
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder threadNamePrefix(String threadNamePrefix) {
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize,
				this.maxPoolSize, this.allowCoreThreadTimeOut, this.keepAlive,
				threadNamePrefix, this.taskDecorator, this.customizers);
	}

	/**
	 * Set the {@link TaskDecorator} to use or {@code null} to not use any.
	 * @param taskDecorator the task decorator to use
	 * @return a new builder instance
	 */
	public TaskExecutorBuilder taskDecorator(TaskDecorator taskDecorator) {
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize,
				this.maxPoolSize, this.allowCoreThreadTimeOut, this.keepAlive,
				this.threadNamePrefix, taskDecorator, this.customizers);
	}

	/**
	 * Set the {@link TaskExecutorCustomizer TaskExecutorCustomizers} that should be
	 * applied to the {@link ThreadPoolTaskExecutor}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied. Setting this
	 * value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(TaskExecutorCustomizer...)
	 */
	public TaskExecutorBuilder customizers(TaskExecutorCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set the {@link TaskExecutorCustomizer TaskExecutorCustomizers} that should be
	 * applied to the {@link ThreadPoolTaskExecutor}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied. Setting this
	 * value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(TaskExecutorCustomizer...)
	 */
	public TaskExecutorBuilder customizers(Iterable<TaskExecutorCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize,
				this.maxPoolSize, this.allowCoreThreadTimeOut, this.keepAlive,
				this.threadNamePrefix, this.taskDecorator, append(null, customizers));
	}

	/**
	 * Add {@link TaskExecutorCustomizer TaskExecutorCustomizers} that should be applied
	 * to the {@link ThreadPoolTaskExecutor}. Customizers are applied in the order that
	 * they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(TaskExecutorCustomizer...)
	 */
	public TaskExecutorBuilder additionalCustomizers(
			TaskExecutorCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add {@link TaskExecutorCustomizer TaskExecutorCustomizers} that should be applied
	 * to the {@link ThreadPoolTaskExecutor}. Customizers are applied in the order that
	 * they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(TaskExecutorCustomizer...)
	 */
	public TaskExecutorBuilder additionalCustomizers(
			Iterable<TaskExecutorCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new TaskExecutorBuilder(this.queueCapacity, this.corePoolSize,
				this.maxPoolSize, this.allowCoreThreadTimeOut, this.keepAlive,
				this.threadNamePrefix, this.taskDecorator,
				append(this.customizers, customizers));
	}

	/**
	 * Build a new {@link ThreadPoolTaskExecutor} instance and configure it using this
	 * builder.
	 * @return a configured {@link ThreadPoolTaskExecutor} instance.
	 * @see #build(Class)
	 * @see #configure(ThreadPoolTaskExecutor)
	 */
	public ThreadPoolTaskExecutor build() {
		return build(ThreadPoolTaskExecutor.class);
	}

	/**
	 * Build a new {@link ThreadPoolTaskExecutor} instance of the specified type and
	 * configure it using this builder.
	 * @param <T> the type of task executor
	 * @param taskExecutorClass the template type to create
	 * @return a configured {@link ThreadPoolTaskExecutor} instance.
	 * @see #build()
	 * @see #configure(ThreadPoolTaskExecutor)
	 */
	public <T extends ThreadPoolTaskExecutor> T build(Class<T> taskExecutorClass) {
		return configure(BeanUtils.instantiateClass(taskExecutorClass));
	}

	/**
	 * Configure the provided {@link ThreadPoolTaskExecutor} instance using this builder.
	 * @param <T> the type of task executor
	 * @param taskExecutor the {@link ThreadPoolTaskExecutor} to configure
	 * @return the task executor instance
	 * @see #build()
	 * @see #build(Class)
	 */
	public <T extends ThreadPoolTaskExecutor> T configure(T taskExecutor) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.queueCapacity).to(taskExecutor::setQueueCapacity);
		map.from(this.corePoolSize).to(taskExecutor::setCorePoolSize);
		map.from(this.maxPoolSize).to(taskExecutor::setMaxPoolSize);
		map.from(this.keepAlive).asInt(Duration::getSeconds)
				.to(taskExecutor::setKeepAliveSeconds);
		map.from(this.allowCoreThreadTimeOut).to(taskExecutor::setAllowCoreThreadTimeOut);
		map.from(this.threadNamePrefix).whenHasText()
				.to(taskExecutor::setThreadNamePrefix);
		map.from(this.taskDecorator).to(taskExecutor::setTaskDecorator);
		if (!CollectionUtils.isEmpty(this.customizers)) {
			this.customizers.forEach((customizer) -> customizer.customize(taskExecutor));
		}
		return taskExecutor;
	}

	private <T> Set<T> append(Set<T> set, Iterable<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
		additions.forEach(result::add);
		return Collections.unmodifiableSet(result);
	}

}
