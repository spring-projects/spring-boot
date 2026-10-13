/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.dynatrace;

import io.micrometer.dynatrace.DynatraceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynatraceProperties}.
 *
 * @author Andy Wilkinson
 */
class DynatracePropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		DynatraceProperties properties = new DynatraceProperties();
		DynatraceConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.getV1().getTechnologyType()).isEqualTo(config.technologyType());
		assertThat(properties.getV2().isUseDynatraceSummaryInstruments())
			.isEqualTo(config.useDynatraceSummaryInstruments());
		assertThat(properties.getV2().isExportMeterMetadata()).isEqualTo(config.exportMeterMetadata());
	}

}
