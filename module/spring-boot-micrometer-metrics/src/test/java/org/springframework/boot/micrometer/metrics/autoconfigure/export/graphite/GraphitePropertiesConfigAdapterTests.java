/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.graphite;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.graphite.GraphiteProtocol;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphitePropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class GraphitePropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<GraphiteProperties, GraphitePropertiesConfigAdapter> {

	GraphitePropertiesConfigAdapterTests() {
		super(GraphitePropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesEnabledIsSetAdapterEnabledReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setEnabled(false);
		assertThat(new GraphitePropertiesConfigAdapter(properties).enabled()).isFalse();
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setStep(Duration.ofMinutes(15));
		assertThat(new GraphitePropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void whenPropertiesRateUnitsIsSetAdapterRateUnitsReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setRateUnits(TimeUnit.MINUTES);
		assertThat(new GraphitePropertiesConfigAdapter(properties).rateUnits()).isEqualTo(TimeUnit.MINUTES);
	}

	@Test
	void whenPropertiesDurationUnitsIsSetAdapterDurationUnitsReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setDurationUnits(TimeUnit.MINUTES);
		assertThat(new GraphitePropertiesConfigAdapter(properties).durationUnits()).isEqualTo(TimeUnit.MINUTES);
	}

	@Test
	void whenPropertiesHostIsSetAdapterHostReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setHost("node");
		assertThat(new GraphitePropertiesConfigAdapter(properties).host()).isEqualTo("node");
	}

	@Test
	void whenPropertiesPortIsSetAdapterPortReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setPort(4242);
		assertThat(new GraphitePropertiesConfigAdapter(properties).port()).isEqualTo(4242);
	}

	@Test
	void whenPropertiesProtocolIsSetAdapterProtocolReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setProtocol(GraphiteProtocol.UDP);
		assertThat(new GraphitePropertiesConfigAdapter(properties).protocol()).isEqualTo(GraphiteProtocol.UDP);
	}

	@Test
	void whenPropertiesGraphiteTagsEnabledIsSetAdapterGraphiteTagsEnabledReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setGraphiteTagsEnabled(true);
		assertThat(new GraphitePropertiesConfigAdapter(properties).graphiteTagsEnabled()).isTrue();
	}

	@Test
	void whenPropertiesTagsAsPrefixIsSetAdapterTagsAsPrefixReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setTagsAsPrefix(new String[] { "worker" });
		assertThat(new GraphitePropertiesConfigAdapter(properties).tagsAsPrefix()).isEqualTo(new String[] { "worker" });
	}

}
