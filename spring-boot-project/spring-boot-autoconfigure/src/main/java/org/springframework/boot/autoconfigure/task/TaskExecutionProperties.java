/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for task execution.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @since 2.1.0
 */
@ConfigurationProperties("spring.task.execution")
public class TaskExecutionProperties {

	private final Pool pool = new Pool();

	private final Shutdown shutdown = new Shutdown();

	/**
	 * Prefix to use for the names of newly created threads.
	 */
	private String threadNamePrefix = "task-";

	public Pool getPool() {
		return this.pool;
	}

	public Shutdown getShutdown() {
		return this.shutdown;
	}

	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public static class Pool {

		/**
		 * Queue capacity. An unbounded capacity does not increase the pool and therefore
		 * ignores the "max-size" property.
		 */
		private int queueCapacity = Integer.MAX_VALUE;

		/**
		 * Core number of threads.
		 */
		private int coreSize = 8;

		/**
		 * Maximum allowed number of threads. If tasks are filling up the queue, the pool
		 * can expand up to that size to accommodate the load. Ignored if the queue is
		 * unbounded.
		 */
		private int maxSize = Integer.MAX_VALUE;

		/**
		 * Whether core threads are allowed to time out. This enables dynamic growing and
		 * shrinking of the pool.
		 */
		private boolean allowCoreThreadTimeout = true;

		/**
		 * Time limit for which threads may remain idle before being terminated.
		 */
		private Duration keepAlive = Duration.ofSeconds(60);

		public int getQueueCapacity() {
			return this.queueCapacity;
		}

		public void setQueueCapacity(int queueCapacity) {
			this.queueCapacity = queueCapacity;
		}

		public int getCoreSize() {
			return this.coreSize;
		}

		public void setCoreSize(int coreSize) {
			this.coreSize = coreSize;
		}

		public int getMaxSize() {
			return this.maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

		public boolean isAllowCoreThreadTimeout() {
			return this.allowCoreThreadTimeout;
		}

		public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
			this.allowCoreThreadTimeout = allowCoreThreadTimeout;
		}

		public Duration getKeepAlive() {
			return this.keepAlive;
		}

		public void setKeepAlive(Duration keepAlive) {
			this.keepAlive = keepAlive;
		}

	}

	public static class Shutdown {

		/**
		 * Whether the executor should wait for scheduled tasks to complete on shutdown.
		 */
		private boolean awaitTermination;

		/**
		 * Maximum time the executor should wait for remaining tasks to complete.
		 */
		private Duration awaitTerminationPeriod;

		public boolean isAwaitTermination() {
			return this.awaitTermination;
		}

		public void setAwaitTermination(boolean awaitTermination) {
			this.awaitTermination = awaitTermination;
		}

		public Duration getAwaitTerminationPeriod() {
			return this.awaitTerminationPeriod;
		}

		public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
			this.awaitTerminationPeriod = awaitTerminationPeriod;
		}

	}

}
