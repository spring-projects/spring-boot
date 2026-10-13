/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCompose;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StopCommand}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class StopCommandTests {

	private DockerCompose dockerCompose;

	private final Duration duration = Duration.ofSeconds(10);

	@BeforeEach
	void setUp() {
		this.dockerCompose = mock(DockerCompose.class);
	}

	@Test
	void applyToWhenDown() {
		StopCommand.DOWN.applyTo(this.dockerCompose, this.duration, Collections.emptyList());
		then(this.dockerCompose).should().down(this.duration, Collections.emptyList());
	}

	@Test
	void applyToWhenStart() {
		StopCommand.STOP.applyTo(this.dockerCompose, this.duration, Collections.emptyList());
		then(this.dockerCompose).should().stop(this.duration, Collections.emptyList());
	}

}
