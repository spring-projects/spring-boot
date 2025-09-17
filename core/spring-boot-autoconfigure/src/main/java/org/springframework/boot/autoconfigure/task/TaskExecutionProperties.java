/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for task execution.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author Yanming Zhou
 * @since 2.1.0
 */
@ConfigurationProperties("spring.task.execution")
public class TaskExecutionProperties {

	private final Pool pool = new Pool();

	private final Simple simple = new Simple();

	private final Shutdown shutdown = new Shutdown();

	/**
	 * Determine when the task executor is to be created.
	 */
	private Mode mode = Mode.AUTO;

	/**
	 * Prefix to use for the names of newly created threads.
	 */
	private String threadNamePrefix = "task-";

	public Simple getSimple() {
		return this.simple;
	}

	public Pool getPool() {
		return this.pool;
	}

	public Shutdown getShutdown() {
		return this.shutdown;
	}

	public Mode getMode() {
		return this.mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public static class Simple {

		/**
		 * Whether to cancel remaining tasks on close. Only recommended if threads are
		 * commonly expected to be stuck.
		 */
		private boolean cancelRemainingTasksOnClose;

		/**
		 * Whether to reject tasks when the concurrency limit has been reached.
		 */
		private boolean rejectTasksWhenLimitReached;

		/**
		 * Set the maximum number of parallel accesses allowed. -1 indicates no
		 * concurrency limit at all.
		 */
		private @Nullable Integer concurrencyLimit;

		public boolean isCancelRemainingTasksOnClose() {
			return this.cancelRemainingTasksOnClose;
		}

		public void setCancelRemainingTasksOnClose(boolean cancelRemainingTasksOnClose) {
			this.cancelRemainingTasksOnClose = cancelRemainingTasksOnClose;
		}

		public boolean isRejectTasksWhenLimitReached() {
			return this.rejectTasksWhenLimitReached;
		}

		public void setRejectTasksWhenLimitReached(boolean rejectTasksWhenLimitReached) {
			this.rejectTasksWhenLimitReached = rejectTasksWhenLimitReached;
		}

		public @Nullable Integer getConcurrencyLimit() {
			return this.concurrencyLimit;
		}

		public void setConcurrencyLimit(@Nullable Integer concurrencyLimit) {
			this.concurrencyLimit = concurrencyLimit;
		}

	}

	public static class Pool {

		/**
		 * Queue capacity. An unbounded capacity does not increase the pool and therefore
		 * ignores the "max-size" property. Doesn't have an effect if virtual threads are
		 * enabled.
		 */
		private int queueCapacity = Integer.MAX_VALUE;

		/**
		 * Core number of threads. Doesn't have an effect if virtual threads are enabled.
		 */
		private int coreSize = 8;

		/**
		 * Maximum allowed number of threads. If tasks are filling up the queue, the pool
		 * can expand up to that size to accommodate the load. Ignored if the queue is
		 * unbounded. Doesn't have an effect if virtual threads are enabled.
		 */
		private int maxSize = Integer.MAX_VALUE;

		/**
		 * Whether core threads are allowed to time out. This enables dynamic growing and
		 * shrinking of the pool. Doesn't have an effect if virtual threads are enabled.
		 */
		private boolean allowCoreThreadTimeout = true;

		/**
		 * Time limit for which threads may remain idle before being terminated. Doesn't
		 * have an effect if virtual threads are enabled.
		 */
		private Duration keepAlive = Duration.ofSeconds(60);

		private final Shutdown shutdown = new Shutdown();

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

		public Shutdown getShutdown() {
			return this.shutdown;
		}

		public static class Shutdown {

			/**
			 * Whether to accept further tasks after the application context close phase
			 * has begun.
			 */
			private boolean acceptTasksAfterContextClose;

			public boolean isAcceptTasksAfterContextClose() {
				return this.acceptTasksAfterContextClose;
			}

			public void setAcceptTasksAfterContextClose(boolean acceptTasksAfterContextClose) {
				this.acceptTasksAfterContextClose = acceptTasksAfterContextClose;
			}

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
		private @Nullable Duration awaitTerminationPeriod;

		public boolean isAwaitTermination() {
			return this.awaitTermination;
		}

		public void setAwaitTermination(boolean awaitTermination) {
			this.awaitTermination = awaitTermination;
		}

		public @Nullable Duration getAwaitTerminationPeriod() {
			return this.awaitTerminationPeriod;
		}

		public void setAwaitTerminationPeriod(@Nullable Duration awaitTerminationPeriod) {
			this.awaitTerminationPeriod = awaitTerminationPeriod;
		}

	}

	/**
	 * Determine when the task executor is to be created.
	 *
	 * @since 3.5.0
	 */
	public enum Mode {

		/**
		 * Create the task executor if no user-defined executor is present.
		 */
		AUTO,

		/**
		 * Create the task executor even if a user-defined executor is present.
		 */
		FORCE

	}

}
