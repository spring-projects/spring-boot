/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.test.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureTracing @AutoConfigureTracing} when used on a sliced
 * test.
 *
 * @author Moritz Halbritter
 */
// TODO Test AutoConfigureTracing in a sliced test
// @WebMvcTest
@Disabled
@AutoConfigureTracing
class AutoConfigureTracingSlicedIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void shouldHaveTracer() {
		assertThat(this.context.getBean(Tracer.class)).isEqualTo(Tracer.NOOP);
	}

	@Test
	void shouldHaveObservationRegistry() {
		assertThat(this.context.getBean(ObservationRegistry.class)).isNotNull();
	}

}
