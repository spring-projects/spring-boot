/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.datadog;

import io.micrometer.datadog.DatadogConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatadogProperties}.
 *
 * @author Stephane Nicoll
 */
class DatadogPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		DatadogProperties properties = new DatadogProperties();
		DatadogConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.isCompress()).isEqualTo(config.compress());
		assertThat(properties.isDescriptions()).isEqualTo(config.descriptions());
		assertThat(properties.getHostTag()).isEqualTo(config.hostTag());
		assertThat(properties.getUri()).isEqualTo(config.uri());
	}

}
