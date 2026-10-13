/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerEngineException}.
 *
 * @author Scott Frederick
 */
class DockerConnectionExceptionTests {

	private static final String HOST = "docker://localhost/";

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenHostIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DockerConnectionException(null, null))
			.withMessage("'host' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenCauseIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DockerConnectionException(HOST, null))
			.withMessage("'cause' must not be null");
	}

	@Test
	void createWithIOException() {
		DockerConnectionException exception = new DockerConnectionException(HOST, new IOException("error"));
		assertThat(exception.getMessage())
			.contains("Connection to the Docker daemon at 'docker://localhost/' failed with error \"error\"");
	}

	@Test
	void createWithLastErrorException() {
		DockerConnectionException exception = new DockerConnectionException(HOST,
				new IOException(new com.sun.jna.LastErrorException("root cause")));
		assertThat(exception.getMessage())
			.contains("Connection to the Docker daemon at 'docker://localhost/' failed with error \"root cause\"");
	}

}
