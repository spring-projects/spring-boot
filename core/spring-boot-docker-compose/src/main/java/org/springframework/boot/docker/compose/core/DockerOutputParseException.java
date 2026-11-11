/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

/**
 * {@link DockerException} thrown if the docker JSON cannot be parsed.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class DockerOutputParseException extends DockerException {

	DockerOutputParseException(String json, Throwable cause) {
		super("Failed to parse docker JSON:\n\n" + json, cause);
	}

}
