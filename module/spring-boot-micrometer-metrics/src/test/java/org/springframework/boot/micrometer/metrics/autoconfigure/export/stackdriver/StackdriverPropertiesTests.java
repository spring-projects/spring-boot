/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.stackdriver;

import io.micrometer.stackdriver.StackdriverConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StackdriverProperties}.
 *
 * @author Johannes Graf
 */
class StackdriverPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		StackdriverProperties properties = new StackdriverProperties();
		StackdriverConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.getResourceType()).isEqualTo(config.resourceType());
		assertThat(properties.isUseSemanticMetricTypes()).isEqualTo(config.useSemanticMetricTypes());
		assertThat(properties.getMetricTypePrefix()).isEqualTo(config.metricTypePrefix());
	}

}
