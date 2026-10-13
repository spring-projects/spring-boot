/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReadinessTimeoutException}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ReadinessTimeoutExceptionTests {

	@Test
	void createCreatesException() {
		Duration timeout = Duration.ofSeconds(10);
		RunningService s1 = mock(RunningService.class);
		given(s1.name()).willReturn("s1");
		RunningService s2 = mock(RunningService.class);
		given(s2.name()).willReturn("s2");
		ServiceNotReadyException cause1 = new ServiceNotReadyException(s1, "1 not ready");
		ServiceNotReadyException cause2 = new ServiceNotReadyException(s2, "2 not ready");
		List<ServiceNotReadyException> exceptions = List.of(cause1, cause2);
		ReadinessTimeoutException exception = new ReadinessTimeoutException(timeout, exceptions);
		assertThat(exception).hasMessage("Readiness timeout of PT10S reached while waiting for services [s1, s2]");
		assertThat(exception).hasSuppressedException(cause1).hasSuppressedException(cause2);
		assertThat(exception.getTimeout()).isEqualTo(timeout);
	}

}
