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

package org.springframework.boot.autoconfigure.task;

import java.time.Duration;

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
	 * Prefix to use for the names of newly created threads.
	 */
	private String threadNamePrefix = "task-";

	/**
	 * Returns the Simple object associated with this TaskExecutionProperties.
	 * @return the Simple object associated with this TaskExecutionProperties
	 */
	public Simple getSimple() {
		return this.simple;
	}

	/**
	 * Returns the pool associated with this TaskExecutionProperties object.
	 * @return the pool associated with this TaskExecutionProperties object
	 */
	public Pool getPool() {
		return this.pool;
	}

	/**
	 * Returns the shutdown object associated with this TaskExecutionProperties.
	 * @return the shutdown object
	 */
	public Shutdown getShutdown() {
		return this.shutdown;
	}

	/**
	 * Returns the thread name prefix used for task execution.
	 * @return the thread name prefix
	 */
	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	/**
	 * Sets the prefix for the thread name of the task execution.
	 * @param threadNamePrefix the prefix to be set for the thread name
	 */
	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	/**
	 * Simple class.
	 */
	public static class Simple {

		/**
		 * Set the maximum number of parallel accesses allowed. -1 indicates no
		 * concurrency limit at all.
		 */
		private Integer concurrencyLimit;

		/**
		 * Returns the concurrency limit of the Simple class.
		 * @return the concurrency limit as an Integer
		 */
		public Integer getConcurrencyLimit() {
			return this.concurrencyLimit;
		}

		/**
		 * Sets the concurrency limit for the Simple class.
		 * @param concurrencyLimit the maximum number of concurrent operations allowed
		 */
		public void setConcurrencyLimit(Integer concurrencyLimit) {
			this.concurrencyLimit = concurrencyLimit;
		}

	}

	/**
	 * Pool class.
	 */
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

		private final Shutdown shutdown = new Shutdown();

		/**
		 * Returns the capacity of the queue in the Pool.
		 * @return the capacity of the queue
		 */
		public int getQueueCapacity() {
			return this.queueCapacity;
		}

		/**
		 * Sets the capacity of the queue in the Pool.
		 * @param queueCapacity the new capacity of the queue
		 */
		public void setQueueCapacity(int queueCapacity) {
			this.queueCapacity = queueCapacity;
		}

		/**
		 * Returns the size of the core in the pool.
		 * @return the size of the core in the pool
		 */
		public int getCoreSize() {
			return this.coreSize;
		}

		/**
		 * Sets the core size of the pool.
		 * @param coreSize the new core size for the pool
		 */
		public void setCoreSize(int coreSize) {
			this.coreSize = coreSize;
		}

		/**
		 * Returns the maximum size of the pool.
		 * @return the maximum size of the pool
		 */
		public int getMaxSize() {
			return this.maxSize;
		}

		/**
		 * Sets the maximum size of the pool.
		 * @param maxSize the maximum size of the pool
		 */
		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

		/**
		 * Returns whether core threads are allowed to timeout.
		 * @return {@code true} if core threads are allowed to timeout, {@code false}
		 * otherwise.
		 */
		public boolean isAllowCoreThreadTimeout() {
			return this.allowCoreThreadTimeout;
		}

		/**
		 * Sets whether core threads in the pool are allowed to timeout.
		 * @param allowCoreThreadTimeout true if core threads are allowed to timeout,
		 * false otherwise
		 */
		public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
			this.allowCoreThreadTimeout = allowCoreThreadTimeout;
		}

		/**
		 * Returns the keep alive duration for the pool.
		 * @return the keep alive duration
		 */
		public Duration getKeepAlive() {
			return this.keepAlive;
		}

		/**
		 * Sets the keep alive duration for the pool.
		 * @param keepAlive the duration for which idle objects in the pool should be kept
		 * alive
		 */
		public void setKeepAlive(Duration keepAlive) {
			this.keepAlive = keepAlive;
		}

		/**
		 * Returns the shutdown object associated with this Pool.
		 * @return the shutdown object associated with this Pool
		 */
		public Shutdown getShutdown() {
			return this.shutdown;
		}

		/**
		 * Shutdown class.
		 */
		public static class Shutdown {

			/**
			 * Whether to accept further tasks after the application context close phase
			 * has begun.
			 */
			private boolean acceptTasksAfterContextClose;

			/**
			 * Returns a boolean value indicating whether tasks are accepted after the
			 * context is closed.
			 * @return true if tasks are accepted after the context is closed, false
			 * otherwise
			 */
			public boolean isAcceptTasksAfterContextClose() {
				return this.acceptTasksAfterContextClose;
			}

			/**
			 * Sets whether to accept tasks after the context is closed.
			 * @param acceptTasksAfterContextClose true to accept tasks after the context
			 * is closed, false otherwise
			 */
			public void setAcceptTasksAfterContextClose(boolean acceptTasksAfterContextClose) {
				this.acceptTasksAfterContextClose = acceptTasksAfterContextClose;
			}

		}

	}

	/**
	 * Shutdown class.
	 */
	public static class Shutdown {

		/**
		 * Whether the executor should wait for scheduled tasks to complete on shutdown.
		 */
		private boolean awaitTermination;

		/**
		 * Maximum time the executor should wait for remaining tasks to complete.
		 */
		private Duration awaitTerminationPeriod;

		/**
		 * Returns the value indicating whether the termination of the Shutdown instance
		 * is awaited.
		 * @return {@code true} if the termination is awaited, {@code false} otherwise.
		 */
		public boolean isAwaitTermination() {
			return this.awaitTermination;
		}

		/**
		 * Sets whether the program should await termination of all threads before
		 * exiting.
		 * @param awaitTermination true if the program should await termination, false
		 * otherwise
		 */
		public void setAwaitTermination(boolean awaitTermination) {
			this.awaitTermination = awaitTermination;
		}

		/**
		 * Returns the await termination period.
		 * @return the await termination period
		 */
		public Duration getAwaitTerminationPeriod() {
			return this.awaitTerminationPeriod;
		}

		/**
		 * Sets the await termination period for the Shutdown class.
		 * @param awaitTerminationPeriod the duration to wait for termination
		 */
		public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
			this.awaitTerminationPeriod = awaitTerminationPeriod;
		}

	}

}
