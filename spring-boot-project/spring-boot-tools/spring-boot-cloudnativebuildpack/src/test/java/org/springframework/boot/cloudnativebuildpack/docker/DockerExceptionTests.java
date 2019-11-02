/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.cloudnativebuildpack.docker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerException}.
 *
 * @author Phillip Webb
 */
class DockerExceptionTests {

	private static final URI URI;
	static {
		try {
			URI = new URI("docker://localhost");
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static final Errors NO_ERRORS = new Errors(Collections.emptyList());

	private static final Errors ERRORS = new Errors(Collections.singletonList(new Errors.Error("code", "message")));

	@Test
	void createWhenUriIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DockerException(null, 404, null, NO_ERRORS))
				.withMessage("URI must not be null");
	}

	@Test
	void create() {
		DockerException exception = new DockerException(URI, 404, "missing", ERRORS);
		assertThat(exception.getMessage()).isEqualTo(
				"Docker API call to 'docker://localhost' failed with status code 404 \"missing\" [code: message]");
		assertThat(exception.getStatusCode()).isEqualTo(404);
		assertThat(exception.getReasonPhrase()).isEqualTo("missing");
		assertThat(exception.getErrors()).isSameAs(ERRORS);
	}

	@Test
	void createWhenReasonPhraseIsNull() {
		DockerException exception = new DockerException(URI, 404, null, ERRORS);
		assertThat(exception.getMessage())
				.isEqualTo("Docker API call to 'docker://localhost' failed with status code 404 [code: message]");
		assertThat(exception.getStatusCode()).isEqualTo(404);
		assertThat(exception.getReasonPhrase()).isNull();
		assertThat(exception.getErrors()).isSameAs(ERRORS);
	}

	@Test
	void createWhenErrorsIsNull() {
		DockerException exception = new DockerException(URI, 404, "missing", null);
		assertThat(exception.getErrors()).isNull();
	}

	@Test
	void createWhenErrorsIsEmpty() {
		DockerException exception = new DockerException(URI, 404, "missing", NO_ERRORS);
		assertThat(exception.getMessage())
				.isEqualTo("Docker API call to 'docker://localhost' failed with status code 404 \"missing\"");
		assertThat(exception.getStatusCode()).isEqualTo(404);
		assertThat(exception.getReasonPhrase()).isEqualTo("missing");
		assertThat(exception.getErrors()).isSameAs(NO_ERRORS);

	}

}
