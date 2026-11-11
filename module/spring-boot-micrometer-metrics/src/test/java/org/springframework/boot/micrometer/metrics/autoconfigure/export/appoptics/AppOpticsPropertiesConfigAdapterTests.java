/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.appoptics;

import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppOpticsPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.2.0 for removal in 4.4.0 with no replacement as AppOptics has
 * retired
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class AppOpticsPropertiesConfigAdapterTests
		extends StepRegistryPropertiesConfigAdapterTests<AppOpticsProperties, AppOpticsPropertiesConfigAdapter> {

	AppOpticsPropertiesConfigAdapterTests() {
		super(AppOpticsPropertiesConfigAdapter.class);
	}

	@Override
	protected AppOpticsProperties createProperties() {
		return new AppOpticsProperties();
	}

	@Override
	protected AppOpticsPropertiesConfigAdapter createConfigAdapter(AppOpticsProperties properties) {
		return new AppOpticsPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		AppOpticsProperties properties = createProperties();
		properties.setUri("https://appoptics.example.com/v1/measurements");
		assertThat(createConfigAdapter(properties).uri()).isEqualTo("https://appoptics.example.com/v1/measurements");
	}

	@Test
	void whenPropertiesApiTokenIsSetAdapterApiTokenReturnsIt() {
		AppOpticsProperties properties = createProperties();
		properties.setApiToken("ABC123");
		assertThat(createConfigAdapter(properties).apiToken()).isEqualTo("ABC123");
	}

	@Test
	void whenPropertiesHostTagIsSetAdapterHostTagReturnsIt() {
		AppOpticsProperties properties = createProperties();
		properties.setHostTag("node");
		assertThat(createConfigAdapter(properties).hostTag()).isEqualTo("node");
	}

	@Test
	void whenPropertiesFloorTimesIsSetAdapterFloorTimesReturnsIt() {
		AppOpticsProperties properties = createProperties();
		properties.setFloorTimes(true);
		assertThat(createConfigAdapter(properties).floorTimes()).isTrue();
	}

}
