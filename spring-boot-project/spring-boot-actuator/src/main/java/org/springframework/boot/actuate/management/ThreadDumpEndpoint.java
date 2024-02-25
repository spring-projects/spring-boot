/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.function.Function;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
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

	private final PlainTextThreadDumpFormatter plainTextFormatter = new PlainTextThreadDumpFormatter();

	/**
     * Retrieves a thread dump and returns it as a ThreadDumpDescriptor object.
     *
     * @return The thread dump as a ThreadDumpDescriptor object.
     */
    @ReadOperation
	public ThreadDumpDescriptor threadDump() {
		return getFormattedThreadDump(ThreadDumpDescriptor::new);
	}

	/**
     * Retrieves a formatted thread dump in plain text format.
     *
     * @return the formatted thread dump as a string
     */
    @ReadOperation(produces = "text/plain;charset=UTF-8")
	public String textThreadDump() {
		return getFormattedThreadDump(this.plainTextFormatter::format);
	}

	/**
     * Retrieves a formatted thread dump using the provided formatter function.
     * 
     * @param formatter the function used to format the thread dump
     * @return the formatted thread dump
     * @param <T> the type of the formatted thread dump
     */
    private <T> T getFormattedThreadDump(Function<ThreadInfo[], T> formatter) {
		return formatter.apply(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true));
	}

	/**
	 * Description of a thread dump.
	 */
	public static final class ThreadDumpDescriptor implements OperationResponseBody {

		private final List<ThreadInfo> threads;

		/**
         * Constructs a new ThreadDumpDescriptor with the given array of ThreadInfo objects.
         * 
         * @param threads the array of ThreadInfo objects representing the thread dump
         */
        private ThreadDumpDescriptor(ThreadInfo[] threads) {
			this.threads = Arrays.asList(threads);
		}

		/**
         * Returns the list of ThreadInfo objects representing the threads in the ThreadDumpDescriptor.
         *
         * @return the list of ThreadInfo objects representing the threads in the ThreadDumpDescriptor
         */
        public List<ThreadInfo> getThreads() {
			return this.threads;
		}

	}

}
