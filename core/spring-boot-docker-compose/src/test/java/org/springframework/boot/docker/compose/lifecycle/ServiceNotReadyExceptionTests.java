/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceNotReadyException}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceNotReadyExceptionTests {

	@Test
	void getServiceReturnsService() {
		RunningService service = mock(RunningService.class);
		ServiceNotReadyException exception = new ServiceNotReadyException(service, "fail");
		assertThat(exception.getService()).isEqualTo(service);
	}

}
