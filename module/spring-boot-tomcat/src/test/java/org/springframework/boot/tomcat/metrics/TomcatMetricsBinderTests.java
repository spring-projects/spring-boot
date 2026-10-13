/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatMetricsBinder}.
 *
 * @author Andy Wilkinson
 */
class TomcatMetricsBinderTests {

	private final MeterRegistry meterRegistry = mock(MeterRegistry.class);

	@Test
	void destroySucceedsWhenCalledBeforeApplicationHasStarted() {
		new TomcatMetricsBinder(this.meterRegistry).destroy();
	}

}
