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

package org.springframework.boot.actuate.management;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * {@link Endpoint @Endpoint} to expose thread info.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "threaddump")
public class ThreadDumpEndpoint {

	@ReadOperation
	public ThreadDumpDescriptor threadDump() {
		return new ThreadDumpDescriptor(Arrays.asList(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)));
	}

	/**
	 * A description of a thread dump. Primarily intended for serialization to JSON.
	 */
	public static final class ThreadDumpDescriptor {

		private final List<ThreadInfo> threads;

		private ThreadDumpDescriptor(List<ThreadInfo> threads) {
			this.threads = threads;
		}

		public List<ThreadInfo> getThreads() {
			return this.threads;
		}

	}

}
