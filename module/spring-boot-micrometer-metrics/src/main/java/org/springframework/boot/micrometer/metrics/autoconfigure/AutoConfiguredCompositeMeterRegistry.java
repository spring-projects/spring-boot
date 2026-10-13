/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import java.util.List;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

/**
 * Specialization of {@link CompositeMeterRegistry} used to identify the auto-configured
 * composite.
 *
 * @author Andy Wilkinson
 */
class AutoConfiguredCompositeMeterRegistry extends CompositeMeterRegistry {

	AutoConfiguredCompositeMeterRegistry(Clock clock, List<MeterRegistry> registries) {
		super(clock, registries);
	}

}
