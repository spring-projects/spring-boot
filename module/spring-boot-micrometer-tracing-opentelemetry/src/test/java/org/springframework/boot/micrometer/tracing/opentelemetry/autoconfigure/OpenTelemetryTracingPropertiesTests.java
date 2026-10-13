/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import io.opentelemetry.sdk.trace.SpanLimits;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryTracingProperties}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryTracingPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		OpenTelemetryTracingProperties.Limits limits = new OpenTelemetryTracingProperties.Limits();
		SpanLimits defaults = SpanLimits.getDefault();
		assertThat(limits.getMaxAttributeValueLength()).isEqualTo(defaults.getMaxAttributeValueLength());
		assertThat(limits.getMaxAttributes()).isEqualTo(defaults.getMaxNumberOfAttributes());
		assertThat(limits.getMaxEvents()).isEqualTo(defaults.getMaxNumberOfEvents());
		assertThat(limits.getMaxLinks()).isEqualTo(defaults.getMaxNumberOfLinks());
		assertThat(limits.getMaxAttributesPerEvent()).isEqualTo(defaults.getMaxNumberOfAttributesPerEvent());
		assertThat(limits.getMaxAttributesPerLink()).isEqualTo(defaults.getMaxNumberOfAttributesPerLink());
	}

}
