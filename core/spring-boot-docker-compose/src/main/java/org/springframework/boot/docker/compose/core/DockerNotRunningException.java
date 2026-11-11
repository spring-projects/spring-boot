/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

/**
 * {@link DockerException} thrown if the docker daemon is not running.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class DockerNotRunningException extends DockerException {

	private final String errorOutput;

	DockerNotRunningException(String errorOutput, Throwable cause) {
		super("Docker is not running", cause);
		this.errorOutput = errorOutput;
	}

	/**
	 * Return the error output returned from docker.
	 * @return the error output
	 */
	public String getErrorOutput() {
		return this.errorOutput;
	}

}
