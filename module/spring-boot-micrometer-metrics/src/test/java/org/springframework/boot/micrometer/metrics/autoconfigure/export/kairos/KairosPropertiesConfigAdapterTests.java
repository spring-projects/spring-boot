/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.kairos;

import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KairosPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 */
class KairosPropertiesConfigAdapterTests
		extends StepRegistryPropertiesConfigAdapterTests<KairosProperties, KairosPropertiesConfigAdapter> {

	KairosPropertiesConfigAdapterTests() {
		super(KairosPropertiesConfigAdapter.class);
	}

	@Override
	protected KairosProperties createProperties() {
		return new KairosProperties();
	}

	@Override
	protected KairosPropertiesConfigAdapter createConfigAdapter(KairosProperties properties) {
		return new KairosPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		KairosProperties properties = createProperties();
		properties.setUri("https://kairos.example.com:8080/api/v1/datapoints");
		assertThat(createConfigAdapter(properties).uri())
			.isEqualTo("https://kairos.example.com:8080/api/v1/datapoints");
	}

	@Test
	void whenPropertiesUserNameIsSetAdapterUserNameReturnsIt() {
		KairosProperties properties = createProperties();
		properties.setUserName("alice");
		assertThat(createConfigAdapter(properties).userName()).isEqualTo("alice");
	}

	@Test
	void whenPropertiesPasswordIsSetAdapterPasswordReturnsIt() {
		KairosProperties properties = createProperties();
		properties.setPassword("secret");
		assertThat(createConfigAdapter(properties).password()).isEqualTo("secret");
	}

}
