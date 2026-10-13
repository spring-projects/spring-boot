/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrometheusPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class PrometheusPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<PrometheusProperties, PrometheusPropertiesConfigAdapter> {

	PrometheusPropertiesConfigAdapterTests() {
		super(PrometheusPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesDescriptionsIsSetAdapterDescriptionsReturnsIt() {
		PrometheusProperties properties = new PrometheusProperties();
		properties.setDescriptions(false);
		assertThat(new PrometheusPropertiesConfigAdapter(properties).descriptions()).isFalse();
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		PrometheusProperties properties = new PrometheusProperties();
		properties.setStep(Duration.ofSeconds(30));
		assertThat(new PrometheusPropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofSeconds(30));
	}

}
