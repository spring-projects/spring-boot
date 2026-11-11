/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.appoptics;

import io.micrometer.appoptics.AppOpticsConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppOpticsProperties}.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.2.0 for removal in 4.4.0 with no replacement as AppOptics has
 * retired
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class AppOpticsPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		AppOpticsProperties properties = new AppOpticsProperties();
		AppOpticsConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.getUri()).isEqualToIgnoringWhitespace(config.uri());
		assertThat(properties.getHostTag()).isEqualToIgnoringWhitespace(config.hostTag());
		assertThat(properties.isFloorTimes()).isEqualTo(config.floorTimes());
	}

}
