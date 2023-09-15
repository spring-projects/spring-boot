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

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link SimpleAsyncTaskScheduler}.
 * Provides convenience methods to set common {@link SimpleAsyncTaskScheduler} settings.
 * For advanced configuration, consider using {@link SimpleAsyncTaskSchedulerCustomizer}.
 * <p>
 * In a typical auto-configured Spring Boot application this builder is available as a
 * bean and can be injected whenever a {@link SimpleAsyncTaskScheduler} is needed.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class SimpleAsyncTaskSchedulerBuilder {

	private final String threadNamePrefix;

	private final Integer concurrencyLimit;

	private final Boolean virtualThreads;

	private final Set<SimpleAsyncTaskSchedulerCustomizer> customizers;

	public SimpleAsyncTaskSchedulerBuilder() {
		this(null, null, null, null);
	}

	private SimpleAsyncTaskSchedulerBuilder(String threadNamePrefix, Integer concurrencyLimit, Boolean virtualThreads,
			Set<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
		this.threadNamePrefix = threadNamePrefix;
		this.concurrencyLimit = concurrencyLimit;
		this.virtualThreads = virtualThreads;
		this.customizers = taskSchedulerCustomizers;
	}

	/**
	 * Set the prefix to use for the names of newly created threads.
	 * @param threadNamePrefix the thread name prefix to set
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskSchedulerBuilder threadNamePrefix(String threadNamePrefix) {
		return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, this.concurrencyLimit, this.virtualThreads,
				this.customizers);
	}

	/**
	 * Set the concurrency limit.
	 * @param concurrencyLimit the concurrency limit
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskSchedulerBuilder concurrencyLimit(Integer concurrencyLimit) {
		return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, concurrencyLimit, this.virtualThreads,
				this.customizers);
	}

	/**
	 * Set whether to use virtual threads.
	 * @param virtualThreads whether to use virtual threads
	 * @return a new builder instance
	 */
	public SimpleAsyncTaskSchedulerBuilder virtualThreads(Boolean virtualThreads) {
		return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, this.concurrencyLimit, virtualThreads,
				this.customizers);
	}

	/**
	 * Set the {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be
	 * applied to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the
	 * order that they were added after builder configuration has been applied. Setting
	 * this value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(SimpleAsyncTaskSchedulerCustomizer...)
	 */
	public SimpleAsyncTaskSchedulerBuilder customizers(SimpleAsyncTaskSchedulerCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set the {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be
	 * applied to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the
	 * order that they were added after builder configuration has been applied. Setting
	 * this value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(Iterable)
	 */
	public SimpleAsyncTaskSchedulerBuilder customizers(
			Iterable<? extends SimpleAsyncTaskSchedulerCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, this.concurrencyLimit, this.virtualThreads,
				append(null, customizers));
	}

	/**
	 * Add {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be applied
	 * to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the order that
	 * they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(SimpleAsyncTaskSchedulerCustomizer...)
	 */
	public SimpleAsyncTaskSchedulerBuilder additionalCustomizers(SimpleAsyncTaskSchedulerCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be applied
	 * to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the order that
	 * they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(Iterable)
	 */
	public SimpleAsyncTaskSchedulerBuilder additionalCustomizers(
			Iterable<? extends SimpleAsyncTaskSchedulerCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, this.concurrencyLimit, this.virtualThreads,
				append(this.customizers, customizers));
	}

	/**
	 * Build a new {@link SimpleAsyncTaskScheduler} instance and configure it using this
	 * builder.
	 * @return a configured {@link SimpleAsyncTaskScheduler} instance.
	 * @see #configure(SimpleAsyncTaskScheduler)
	 */
	public SimpleAsyncTaskScheduler build() {
		return configure(new SimpleAsyncTaskScheduler());
	}

	/**
	 * Configure the provided {@link SimpleAsyncTaskScheduler} instance using this
	 * builder.
	 * @param <T> the type of task scheduler
	 * @param taskScheduler the {@link SimpleAsyncTaskScheduler} to configure
	 * @return the task scheduler instance
	 * @see #build()
	 */
	public <T extends SimpleAsyncTaskScheduler> T configure(T taskScheduler) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.threadNamePrefix).to(taskScheduler::setThreadNamePrefix);
		map.from(this.concurrencyLimit).to(taskScheduler::setConcurrencyLimit);
		map.from(this.virtualThreads).to(taskScheduler::setVirtualThreads);
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
