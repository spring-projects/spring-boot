/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import java.io.PrintStream;

/**
 * Callback interface used to provide {@link DockerApi} output logging.
 *
 * @author Dmytro Nosan
 * @since 3.5.0
 * @see #toSystemOut()
 */
public interface DockerLog {

	/**
	 * Logs a given message.
	 * @param message the message to log
	 */
	void log(String message);

	/**
	 * Factory method that returns a {@link DockerLog} that outputs to {@link System#out}.
	 * @return {@link DockerLog} instance that logs to system out
	 */
	static DockerLog toSystemOut() {
		return to(System.out);
	}

	/**
	 * Factory method that returns a {@link DockerLog} that outputs to a given
	 * {@link PrintStream}.
	 * @param out the print stream used to output the log
	 * @return {@link DockerLog} instance that logs to the given print stream
	 */
	static DockerLog to(PrintStream out) {
		return out::println;
	}

}
