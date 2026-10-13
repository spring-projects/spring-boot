/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCompose;
import org.springframework.boot.logging.LogLevel;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartCommand}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class StartCommandTests {

	private DockerCompose dockerCompose;

	@BeforeEach
	void setUp() {
		this.dockerCompose = mock(DockerCompose.class);
	}

	@Test
	void applyToWhenUp() {
		StartCommand.UP.applyTo(this.dockerCompose, LogLevel.INFO, Collections.emptyList());
		then(this.dockerCompose).should().up(LogLevel.INFO, Collections.emptyList());
	}

	@Test
	void applyToWhenStart() {
		StartCommand.START.applyTo(this.dockerCompose, LogLevel.INFO, Collections.emptyList());
		then(this.dockerCompose).should().start(LogLevel.INFO, Collections.emptyList());
	}

}
