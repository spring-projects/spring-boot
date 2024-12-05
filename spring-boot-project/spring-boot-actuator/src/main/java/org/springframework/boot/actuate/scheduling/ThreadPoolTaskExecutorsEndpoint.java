/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.scheduling;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CustomizableThreadCreator;

/**
 * {@link Endpoint @Endpoint} to expose information about an application's threadPoolTaskExecutors.
 *
 * @author Tanfei Long
 * @since 3.4.1
 */
@Endpoint(id = "threadPoolTaskExecutors")
public class ThreadPoolTaskExecutorsEndpoint {

	private final Collection<ThreadPoolTaskExecutor> threadPoolTaskExecutors;

	public ThreadPoolTaskExecutorsEndpoint(Collection<ThreadPoolTaskExecutor> threadPoolTaskExecutors) {
		this.threadPoolTaskExecutors = threadPoolTaskExecutors;
	}

	@ReadOperation
	public Map<String, ThreadPoolTaskExecutorDescriptor> threadPoolTaskExecutors() {
		return this.threadPoolTaskExecutors.stream()
				.collect(Collectors.toMap(CustomizableThreadCreator::getThreadNamePrefix, ThreadPoolTaskExecutorDescriptor::new));
	}

	/**
	 * Description of an application's ThreadPoolTaskExecutor {@link ThreadPoolTaskExecutor}.
	 */
	public static final class ThreadPoolTaskExecutorDescriptor implements OperationResponseBody {
		private final int corePoolSize;
		private final int maxPoolSize;
		private final int largestPoolSize;
		private final int keepAliveSeconds;
		private final int queueCapacity;
		private final long taskCount;
		private final long completedTaskCount;

		public ThreadPoolTaskExecutorDescriptor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
			this.corePoolSize = threadPoolTaskExecutor.getCorePoolSize();
			this.maxPoolSize = threadPoolTaskExecutor.getMaxPoolSize();
			this.largestPoolSize = threadPoolTaskExecutor.getThreadPoolExecutor().getLargestPoolSize();
			this.keepAliveSeconds = threadPoolTaskExecutor.getKeepAliveSeconds();
			this.queueCapacity = threadPoolTaskExecutor.getQueueCapacity();
			this.taskCount = threadPoolTaskExecutor.getThreadPoolExecutor().getTaskCount();
			this.completedTaskCount = threadPoolTaskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
		}

		public int getCorePoolSize() {
			return this.corePoolSize;
		}

		public int getMaxPoolSize() {
			return this.maxPoolSize;
		}

		public int getKeepAliveSeconds() {
			return this.keepAliveSeconds;
		}

		public int getQueueCapacity() {
			return this.queueCapacity;
		}

		public long getTaskCount() {
			return this.taskCount;
		}

		public long getCompletedTaskCount() {
			return this.completedTaskCount;
		}

		public int getLargestPoolSize() {
			return this.largestPoolSize;
		}
	}
}
