/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.properties;

import io.micrometer.core.instrument.step.StepRegistryConfig;

/**
 * Base tests for {@link StepRegistryProperties} implementation.
 *
 * @author Stephane Nicoll
 */
public abstract class StepRegistryPropertiesTests extends PushRegistryPropertiesTests {

	protected void assertStepRegistryDefaultValues(StepRegistryProperties properties, StepRegistryConfig config) {
		super.assertStepRegistryDefaultValues(properties, config);
	}

}
