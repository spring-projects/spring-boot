/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import org.jspecify.annotations.Nullable;

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

	ProcessExitException(int exitCode, String[] command, String stdOut, String stdErr, @Nullable Throwable cause) {
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
