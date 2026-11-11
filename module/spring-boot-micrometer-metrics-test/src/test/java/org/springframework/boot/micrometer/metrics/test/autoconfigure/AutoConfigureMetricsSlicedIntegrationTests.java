/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureMetrics} when used on a sliced test.
 *
 * @author Moritz Halbritter
 */
// TODO Test AutoConfigureMetrics in a sliced test
// @WebMvcTest
@Disabled
@AutoConfigureMetrics
class AutoConfigureMetricsSlicedIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void shouldHaveMeterRegistry() {
		assertThat(this.context.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class);
	}

	@Test
	void shouldHaveObservationRegistry() {
		assertThat(this.context.getBean(ObservationRegistry.class)).isNotNull();
	}

}
