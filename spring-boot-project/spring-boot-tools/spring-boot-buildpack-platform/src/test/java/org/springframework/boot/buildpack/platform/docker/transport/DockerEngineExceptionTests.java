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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerEngineException}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class DockerEngineExceptionTests {

	private static final String HOST = "docker://localhost/";

	private static final URI URI;
	static {
		try {
			URI = new URI("example");
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static final Errors NO_ERRORS = new Errors(Collections.emptyList());

	private static final Errors ERRORS = new Errors(Collections.singletonList(new Errors.Error("code", "message")));

	private static final Message NO_MESSAGE = new Message(null);

	private static final Message MESSAGE = new Message("response message");

	@Test
	void createWhenHostIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new DockerEngineException(null, null, 404, null, NO_ERRORS, NO_MESSAGE))
			.withMessage("Host must not be null");
	}

	@Test
	void createWhenUriIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new DockerEngineException(HOST, null, 404, null, NO_ERRORS, NO_MESSAGE))
			.withMessage("URI must not be null");
	}

	@Test
	void create() {
		DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", ERRORS, MESSAGE);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" and message \"response message\" [code: message]");
		assertThat(exception.getStatusCode()).isEqualTo(404);
		assertThat(exception.getReasonPhrase()).isEqualTo("missing");
		assertThat(exception.getErrors()).isSameAs(ERRORS);
		assertThat(exception.getResponseMessage()).isSameAs(MESSAGE);
	}

	@Test
	void createWhenReasonPhraseIsNull() {
		DockerEngineException exception = new DockerEngineException(HOST, URI, 404, null, ERRORS, MESSAGE);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost/example' failed with status code 404 and message \"response message\" [code: message]");
		assertThat(exception.getStatusCode()).isEqualTo(404);
		assertThat(exception.getReasonPhrase()).isNull();
		assertThat(exception.getErrors()).isSameAs(ERRORS);
		assertThat(exception.getResponseMessage()).isSameAs(MESSAGE);
	}

	@Test
	void createWhenErrorsIsNull() {
		DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", null, MESSAGE);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" and message \"response message\"");
		assertThat(exception.getErrors()).isNull();
	}

	@Test
	void createWhenErrorsIsEmpty() {
		DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", NO_ERRORS, MESSAGE);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" and message \"response message\"");
		assertThat(exception.getStatusCode()).isEqualTo(404);
		assertThat(exception.getReasonPhrase()).isEqualTo("missing");
		assertThat(exception.getErrors()).isSameAs(NO_ERRORS);
	}

	@Test
	void createWhenMessageIsNull() {
		DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", ERRORS, null);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" [code: message]");
		assertThat(exception.getResponseMessage()).isNull();
	}

	@Test
	void createWhenMessageIsEmpty() {
		DockerEngineException exception = new DockerEngineException(HOST, URI, 404, "missing", ERRORS, NO_MESSAGE);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost/example' failed with status code 404 \"missing\" [code: message]");
		assertThat(exception.getResponseMessage()).isSameAs(NO_MESSAGE);
	}

}
