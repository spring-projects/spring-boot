/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import io.opentelemetry.sdk.logs.LogLimits;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryLoggingProperties}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryLoggingPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		OpenTelemetryLoggingProperties.Limits limits = new OpenTelemetryLoggingProperties.Limits();
		LogLimits defaults = LogLimits.getDefault();
		assertThat(limits.getMaxAttributeValueLength()).isEqualTo(defaults.getMaxAttributeValueLength());
		assertThat(limits.getMaxAttributes()).isEqualTo(defaults.getMaxNumberOfAttributes());
	}

}
