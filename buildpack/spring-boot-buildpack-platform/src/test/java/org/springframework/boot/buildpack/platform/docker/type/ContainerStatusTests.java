/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContainerStatus}.
 *
 * @author Scott Frederick
 */
class ContainerStatusTests {

	@Test
	void ofCreatesFromJson() throws IOException {
		ContainerStatus status = ContainerStatus.of(getClass().getResourceAsStream("container-status-error.json"));
		assertThat(status.getStatusCode()).isOne();
		assertThat(status.getWaitingErrorMessage()).isEqualTo("error detail");
	}

	@Test
	void ofCreatesFromValues() {
		ContainerStatus status = ContainerStatus.of(1, "error detail");
		assertThat(status.getStatusCode()).isOne();
		assertThat(status.getWaitingErrorMessage()).isEqualTo("error detail");
	}

}
