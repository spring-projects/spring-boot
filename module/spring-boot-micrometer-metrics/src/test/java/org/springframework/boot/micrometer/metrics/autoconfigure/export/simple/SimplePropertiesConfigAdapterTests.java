/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.simple;

import java.time.Duration;

import io.micrometer.core.instrument.simple.CountingMode;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimplePropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class SimplePropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<SimpleProperties, SimplePropertiesConfigAdapter> {

	SimplePropertiesConfigAdapterTests() {
		super(SimplePropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		SimpleProperties properties = new SimpleProperties();
		properties.setStep(Duration.ofSeconds(30));
		assertThat(new SimplePropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void whenPropertiesModeIsSetAdapterModeReturnsIt() {
		SimpleProperties properties = new SimpleProperties();
		properties.setMode(CountingMode.STEP);
		assertThat(new SimplePropertiesConfigAdapter(properties).mode()).isEqualTo(CountingMode.STEP);
	}

}
