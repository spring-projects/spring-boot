/*
 * Copyright 2012-present the original author or authors.
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
