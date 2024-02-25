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

	/**
	 * Constructs a new ProcessExitException with the specified exit code, command,
	 * standard output, standard error, and cause.
	 * @param exitCode the exit code of the process
	 * @param command the command that was executed
	 * @param stdOut the standard output of the process
	 * @param stdErr the standard error of the process
	 * @param cause the cause of the exception
	 */
	ProcessExitException(int exitCode, String[] command, String stdOut, String stdErr) {
		this(exitCode, command, stdOut, stdErr, null);
	}

	/**
	 * Constructs a new ProcessExitException with the specified exit code, command,
	 * standard output, standard error, and cause.
	 * @param exitCode the exit code of the process
	 * @param command the command that was executed
	 * @param stdOut the standard output of the process
	 * @param stdErr the standard error of the process
	 * @param cause the cause of the exception
	 */
	ProcessExitException(int exitCode, String[] command, String stdOut, String stdErr, Throwable cause) {
		super(buildMessage(exitCode, command, stdOut, stdErr), cause);
		this.exitCode = exitCode;
		this.command = command;
		this.stdOut = stdOut;
		this.stdErr = stdErr;
	}

	/**
	 * Builds a message for a ProcessExitException.
	 * @param exitCode the exit code of the failed process
	 * @param command the command that was executed
	 * @param stdOut the standard output of the process
	 * @param strErr the standard error of the process
	 * @return the formatted message
	 */
	private static String buildMessage(int exitCode, String[] command, String stdOut, String strErr) {
		return "'%s' failed with exit code %d.\n\nStdout:\n%s\n\nStderr:\n%s".formatted(String.join(" ", command),
				exitCode, stdOut, strErr);
	}

	/**
	 * Returns the exit code associated with this ProcessExitException.
	 * @return the exit code
	 */
	int getExitCode() {
		return this.exitCode;
	}

	/**
	 * Returns the command that caused the process exit.
	 * @return the command that caused the process exit
	 */
	String[] getCommand() {
		return this.command;
	}

	/**
	 * Returns the standard output of the process.
	 * @return the standard output of the process
	 */
	String getStdOut() {
		return this.stdOut;
	}

	/**
	 * Returns the standard error output of the process.
	 * @return the standard error output of the process
	 */
	String getStdErr() {
		return this.stdErr;
	}

}
