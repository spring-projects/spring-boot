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

package org.springframework.boot.autoconfigure.task;

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

	/**
	 * Prefix to use for the names of newly created threads.
	 */
	private String threadNamePrefix = "scheduling-";

	public Pool getPool() {
		return this.pool;
	}

	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public static class Pool {

		/**
		 * Maximum allowed number of threads.
		 */
		private int size = 1;

		public int getSize() {
			return this.size;
		}

		public void setSize(int size) {
			this.size = size;
		}

	}

}
