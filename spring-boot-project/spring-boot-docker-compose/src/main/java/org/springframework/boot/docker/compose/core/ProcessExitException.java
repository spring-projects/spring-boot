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

package org.springframework.boot.docker.compose.core;

/**
 * Exception thrown by {@link ProcessRunner} when the process exits with a non-zero code.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ProcessExitException extends RuntimeException {

	private final int exitCode;

	private final String[] command;

	private final String stdOut;

	private final String stdErr;

	ProcessExitException(int exitCode, String[] command, String stdOut, String stdErr) {
		this(exitCode, command, stdOut, stdErr, null);
	}

	ProcessExitException(int exitCode, String[] command, String stdOut, String stdErr, Throwable cause) {
		super(buildMessage(exitCode, command, stdOut, stdErr), cause);
		this.exitCode = exitCode;
		this.command = command;
		this.stdOut = stdOut;
		this.stdErr = stdErr;
	}

	private static String buildMessage(int exitCode, String[] command, String stdOut, String strErr) {
		return "'%s' failed with exit code %d.\n\nStdout:\n%s\n\nStderr:\n%s".formatted(String.join(" ", command),
				exitCode, stdOut, strErr);
	}

	int getExitCode() {
		return this.exitCode;
	}

	String[] getCommand() {
		return this.command;
	}

	String getStdOut() {
		return this.stdOut;
	}

	String getStdErr() {
		return this.stdErr;
	}

}
