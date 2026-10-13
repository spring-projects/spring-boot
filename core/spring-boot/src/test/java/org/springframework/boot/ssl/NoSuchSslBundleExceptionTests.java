/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchSslBundleException}.
 *
 * @author Phillip Webb
 */
class NoSuchSslBundleExceptionTests {

	@Test
	void createCreatesException() {
		Throwable cause = new RuntimeException();
		NoSuchSslBundleException exception = new NoSuchSslBundleException("name", "badness", cause);
		assertThat(exception).hasMessage("badness").hasCause(cause);
		assertThat(exception.getBundleName()).isEqualTo("name");
	}

}
