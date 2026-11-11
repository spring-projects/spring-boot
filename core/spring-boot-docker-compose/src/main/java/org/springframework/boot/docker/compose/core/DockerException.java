/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

/**
 * Base class for docker exceptions.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public abstract class DockerException extends RuntimeException {

	public DockerException(String message) {
		super(message);
	}

	public DockerException(String message, Throwable cause) {
		super(message, cause);
	}

}
