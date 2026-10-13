/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LifecycleManagement}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class LifecycleManagementTests {

	@Test
	void shouldStartupWhenNone() {
		assertThat(LifecycleManagement.NONE.shouldStart()).isFalse();
	}

	@Test
	void shouldShutdownWhenNone() {
		assertThat(LifecycleManagement.NONE.shouldStop()).isFalse();
	}

	@Test
	void shouldStartupWhenStartOnly() {
		assertThat(LifecycleManagement.START_ONLY.shouldStart()).isTrue();
	}

	@Test
	void shouldShutdownWhenStartOnly() {
		assertThat(LifecycleManagement.START_ONLY.shouldStop()).isFalse();
	}

	@Test
	void shouldStartupWhenStartAndStop() {
		assertThat(LifecycleManagement.START_AND_STOP.shouldStart()).isTrue();
	}

	@Test
	void shouldShutdownWhenStartAndStop() {
		assertThat(LifecycleManagement.START_AND_STOP.shouldStop()).isTrue();
	}

}
