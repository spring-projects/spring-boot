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
	void createWhenHostIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DockerConnectionException(null, null))
			.withMessage("Host must not be null");
	}

	@Test
	void createWhenCauseIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DockerConnectionException(HOST, null))
			.withMessage("Cause must not be null");
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
