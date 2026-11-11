/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.jmx;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmxPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class JmxPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<JmxProperties, JmxPropertiesConfigAdapter> {

	JmxPropertiesConfigAdapterTests() {
		super(JmxPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		JmxProperties properties = new JmxProperties();
		properties.setStep(Duration.ofMinutes(15));
		assertThat(new JmxPropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void whenPropertiesDomainIsSetAdapterDomainReturnsIt() {
		JmxProperties properties = new JmxProperties();
		properties.setDomain("abc");
		assertThat(new JmxPropertiesConfigAdapter(properties).domain()).isEqualTo("abc");
	}

}
