/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.ganglia;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GangliaPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class GangliaPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<GangliaProperties, GangliaPropertiesConfigAdapter> {

	GangliaPropertiesConfigAdapterTests() {
		super(GangliaPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesEnabledIsSetAdapterEnabledReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setEnabled(false);
		assertThat(new GangliaPropertiesConfigAdapter(properties).enabled()).isFalse();
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setStep(Duration.ofMinutes(15));
		assertThat(new GangliaPropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void whenPropertiesDurationUnitsIsSetAdapterDurationUnitsReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setDurationUnits(TimeUnit.MINUTES);
		assertThat(new GangliaPropertiesConfigAdapter(properties).durationUnits()).isEqualTo(TimeUnit.MINUTES);
	}

	@Test
	void whenPropertiesAddressingModeIsSetAdapterAddressingModeReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setAddressingMode(UDPAddressingMode.UNICAST);
		assertThat(new GangliaPropertiesConfigAdapter(properties).addressingMode())
			.isEqualTo(UDPAddressingMode.UNICAST);
	}

	@Test
	void whenPropertiesTimeToLiveIsSetAdapterTtlReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setTimeToLive(2);
		assertThat(new GangliaPropertiesConfigAdapter(properties).ttl()).isEqualTo(2);
	}

	@Test
	void whenPropertiesHostIsSetAdapterHostReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setHost("node");
		assertThat(new GangliaPropertiesConfigAdapter(properties).host()).isEqualTo("node");
	}

	@Test
	void whenPropertiesPortIsSetAdapterPortReturnsIt() {
		GangliaProperties properties = new GangliaProperties();
		properties.setPort(4242);
		assertThat(new GangliaPropertiesConfigAdapter(properties).port()).isEqualTo(4242);
	}

}
