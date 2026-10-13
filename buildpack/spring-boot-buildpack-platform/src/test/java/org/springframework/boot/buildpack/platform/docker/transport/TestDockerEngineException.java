/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.net.URI;

import org.jspecify.annotations.Nullable;

/**
 * Subclass of {@link DockerEngineException} for testing.
 *
 * @author Phillip Webb
 */
public class TestDockerEngineException extends DockerEngineException {

	public TestDockerEngineException(String host, URI uri, int statusCode, @Nullable String reasonPhrase,
			@Nullable Errors errors, @Nullable Message responseMessage, byte @Nullable [] content) {
		super(host, uri, statusCode, reasonPhrase, errors, responseMessage, content);
	}

}
