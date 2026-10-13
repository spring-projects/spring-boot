/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DockerComposeServicesReadyEvent}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeServicesReadyEventTests {

	private ApplicationContext applicationContext = mock(ApplicationContext.class);

	private List<RunningService> runningServices = List.of(mock(RunningService.class));

	private DockerComposeServicesReadyEvent event = new DockerComposeServicesReadyEvent(this.applicationContext,
			this.runningServices);

	@Test
	void getSourceReturnsSource() {
		assertThat(this.event.getSource()).isSameAs(this.applicationContext);
	}

	@Test
	void getRunningServicesReturnsRunningServices() {
		assertThat(this.event.getRunningServices()).isSameAs(this.runningServices);
	}

}
