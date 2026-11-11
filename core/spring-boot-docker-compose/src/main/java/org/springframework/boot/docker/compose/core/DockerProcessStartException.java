/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

/**
 * {@link DockerException} thrown if the docker process cannot be started. Usually
 * indicates that docker is not installed.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class DockerProcessStartException extends DockerException {

	DockerProcessStartException(String message, Throwable cause) {
		super(message, cause);
	}

}
