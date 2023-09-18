/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link SimpleAsyncTaskExecutor}.
 * Provides convenience methods to set common {@link SimpleAsyncTaskExecutor} settings and
 * register {@link #taskDecorator(TaskDecorator)}). For advanced configuration, consider
 * using {@link SimpleAsyncTaskExecutorCustomizer}.
 * <p>
 * In a typical auto-configured Spring Boot application this builder is available as a
 * bean and can be injected whenever a {@link SimpleAsyncTaskExecutor} is needed.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class SimpleAsyncTaskExecutorBuilder {

	private final Boolean virtualThreads;

	private final String threadNamePrefix;

	private final Integer concurrencyLimit;

	private final TaskDecorator taskDecorator;

	private final Set<SimpleAsyncTaskExecutorCustomizer> customizers;

	public SimpleAsyncTaskExecutorBuilder() {
		this(null, null, null, null, null);
	}

	private SimpleAsyncTaskExecutorBuilder(Boolean virtualThreads, String threadNamePrefix, Integer concurrencyLimit,
			TaskDecorator taskDecorator, Set<SimpleAsyncTaskExecutorCustomizer> customizers) {
		this.virtualThreads = virtualThreads;
		this.threadNamePrefix = threadNamePrefix;
		this.concurrencyLimit = concurrencyLimit;
		this.taskDecorator = taskDecorator;
		this.customizers = customizers;
	}

	/**
	 * Set the prefix to use for the names of newly created threads.
	 * @param threadNamePrefix the thread name prefix to set
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskExecutorBuilder threadNamePrefix(String threadNamePrefix) {
		return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, threadNamePrefix, this.concurrencyLimit,
				this.taskDecorator, this.customizers);
	}

	/**
	 * Set whether to use virtual threads.
	 * @param virtualThreads whether to use virtual threads
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskExecutorBuilder virtualThreads(Boolean virtualThreads) {
		return new SimpleAsyncTaskExecutorBuilder(virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
				this.taskDecorator, this.customizers);
	}

	/**
	 * Set the concurrency limit.
	 * @param concurrencyLimit the concurrency limit
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskExecutorBuilder concurrencyLimit(Integer concurrencyLimit) {
		return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, concurrencyLimit,
				this.taskDecorator, this.customizers);
	}

	/**
	 * Set the {@link TaskDecorator} to use or {@code null} to not use any.
	 * @param taskDecorator the task decorator to use
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskExecutorBuilder taskDecorator(TaskDecorator taskDecorator) {
		return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
				taskDecorator, this.customizers);
	}

	/**
	 * Set the {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be
	 * applied to the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the
	 * order that they were added after builder configuration has been applied. Setting
	 * this value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(SimpleAsyncTaskExecutorCustomizer...)
	 */
	public SimpleAsyncTaskExecutorBuilder customizers(SimpleAsyncTaskExecutorCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set the {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be
	 * applied to the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the
	 * order that they were added after builder configuration has been applied. Setting
	 * this value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(Iterable)
	 */
	public SimpleAsyncTaskExecutorBuilder customizers(
			Iterable<? extends SimpleAsyncTaskExecutorCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
				this.taskDecorator, append(null, customizers));
	}

	/**
	 * Add {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be applied to
	 * the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(SimpleAsyncTaskExecutorCustomizer...)
	 */
	public SimpleAsyncTaskExecutorBuilder additionalCustomizers(SimpleAsyncTaskExecutorCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be applied to
	 * the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(Iterable)
	 */
	public SimpleAsyncTaskExecutorBuilder additionalCustomizers(
			Iterable<? extends SimpleAsyncTaskExecutorCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
				this.taskDecorator, append(this.customizers, customizers));
	}

	/**
	 * Build a new {@link SimpleAsyncTaskExecutor} instance and configure it using this
	 * builder.
	 * @return a configured {@link SimpleAsyncTaskExecutor} instance.
	 * @see #build(Class)
	 * @see #configure(SimpleAsyncTaskExecutor)
	 */
	public SimpleAsyncTaskExecutor build() {
		return configure(new SimpleAsyncTaskExecutor());
	}

	/**
	 * Build a new {@link SimpleAsyncTaskExecutor} instance of the specified type and
	 * configure it using this builder.
	 * @param <T> the type of task executor
	 * @param taskExecutorClass the template type to create
	 * @return a configured {@link SimpleAsyncTaskExecutor} instance.
	 * @see #build()
	 * @see #configure(SimpleAsyncTaskExecutor)
	 */
	public <T extends SimpleAsyncTaskExecutor> T build(Class<T> taskExecutorClass) {
		return configure(BeanUtils.instantiateClass(taskExecutorClass));
	}

	/**
	 * Configure the provided {@link SimpleAsyncTaskExecutor} instance using this builder.
	 * @param <T> the type of task executor
	 * @param taskExecutor the {@link SimpleAsyncTaskExecutor} to configure
	 * @return the task executor instance
	 * @see #build()
	 * @see #build(Class)
	 */
	public <T extends SimpleAsyncTaskExecutor> T configure(T taskExecutor) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.virtualThreads).to(taskExecutor::setVirtualThreads);
		map.from(this.threadNamePrefix).whenHasText().to(taskExecutor::setThreadNamePrefix);
		map.from(this.concurrencyLimit).to(taskExecutor::setConcurrencyLimit);
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
