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

package org.springframework.boot.autoconfigure.task;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for task scheduling.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@ConfigurationProperties("spring.task.scheduling")
public class TaskSchedulingProperties {

	private final Pool pool = new Pool();

	private final Simple simple = new Simple();

	private final Shutdown shutdown = new Shutdown();

	/**
	 * Prefix to use for the names of newly created threads.
	 */
	private String threadNamePrefix = "scheduling-";

	/**
     * Returns the pool associated with this TaskSchedulingProperties object.
     *
     * @return the pool associated with this TaskSchedulingProperties object
     */
    public Pool getPool() {
		return this.pool;
	}

	/**
     * Returns the Simple object associated with this TaskSchedulingProperties instance.
     *
     * @return the Simple object associated with this TaskSchedulingProperties instance
     */
    public Simple getSimple() {
		return this.simple;
	}

	/**
     * Returns the shutdown object.
     *
     * @return the shutdown object
     */
    public Shutdown getShutdown() {
		return this.shutdown;
	}

	/**
     * Returns the thread name prefix used for task scheduling.
     *
     * @return the thread name prefix
     */
    public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	/**
     * Sets the prefix for the names of the threads used for task scheduling.
     * 
     * @param threadNamePrefix the prefix to be set for thread names
     */
    public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	/**
     * Pool class.
     */
    public static class Pool {

		/**
		 * Maximum allowed number of threads.
		 */
		private int size = 1;

		/**
         * Returns the size of the pool.
         *
         * @return the size of the pool
         */
        public int getSize() {
			return this.size;
		}

		/**
         * Sets the size of the pool.
         * 
         * @param size the size of the pool to be set
         */
        public void setSize(int size) {
			this.size = size;
		}

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
         *
         * @return the concurrency limit as an Integer
         */
        public Integer getConcurrencyLimit() {
			return this.concurrencyLimit;
		}

		/**
         * Sets the concurrency limit for the Simple class.
         * 
         * @param concurrencyLimit the maximum number of concurrent operations allowed
         */
        public void setConcurrencyLimit(Integer concurrencyLimit) {
			this.concurrencyLimit = concurrencyLimit;
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
         * Returns the value indicating whether the termination of the Shutdown instance is awaited.
         *
         * @return {@code true} if the termination is awaited, {@code false} otherwise.
         */
        public boolean isAwaitTermination() {
			return this.awaitTermination;
		}

		/**
         * Sets whether the program should await termination of all threads before exiting.
         * 
         * @param awaitTermination true if the program should await termination, false otherwise
         */
        public void setAwaitTermination(boolean awaitTermination) {
			this.awaitTermination = awaitTermination;
		}

		/**
         * Returns the await termination period.
         * 
         * @return the await termination period
         */
        public Duration getAwaitTerminationPeriod() {
			return this.awaitTerminationPeriod;
		}

		/**
         * Sets the await termination period for the Shutdown class.
         * 
         * @param awaitTerminationPeriod the duration to wait for termination
         */
        public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
			this.awaitTerminationPeriod = awaitTerminationPeriod;
		}

	}

}
