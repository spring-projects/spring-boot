/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.datadog;

import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatadogPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 * @author Mirko Sobeck
 */
class DatadogPropertiesConfigAdapterTests
		extends StepRegistryPropertiesConfigAdapterTests<DatadogProperties, DatadogPropertiesConfigAdapter> {

	DatadogPropertiesConfigAdapterTests() {
		super(DatadogPropertiesConfigAdapter.class);
	}

	@Override
	protected DatadogProperties createProperties() {
		return new DatadogProperties();
	}

	@Override
	protected DatadogPropertiesConfigAdapter createConfigAdapter(DatadogProperties properties) {
		return new DatadogPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesApiKeyIsSetAdapterApiKeyReturnsIt() {
		DatadogProperties properties = createProperties();
		properties.setApiKey("my-api-key");
		assertThat(createConfigAdapter(properties).apiKey()).isEqualTo("my-api-key");
	}

	@Test
	void whenPropertiesApplicationKeyIsSetAdapterApplicationKeyReturnsIt() {
		DatadogProperties properties = createProperties();
		properties.setApplicationKey("my-application-key");
		assertThat(createConfigAdapter(properties).applicationKey()).isEqualTo("my-application-key");
	}

	@Test
	void whenPropertiesCompressIsSetAdapterCompressReturnsIt() {
		DatadogProperties properties = createProperties();
		properties.setCompress(false);
		assertThat(createConfigAdapter(properties).compress()).isFalse();
	}

	@Test
	void whenPropertiesDescriptionsIsSetAdapterDescriptionsReturnsIt() {
		DatadogProperties properties = createProperties();
		properties.setDescriptions(false);
		assertThat(createConfigAdapter(properties).descriptions()).isEqualTo(false);
	}

	@Test
	void whenPropertiesHostTagIsSetAdapterHostTagReturnsIt() {
		DatadogProperties properties = createProperties();
		properties.setHostTag("waldo");
		assertThat(createConfigAdapter(properties).hostTag()).isEqualTo("waldo");
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		DatadogProperties properties = createProperties();
		properties.setUri("https://app.example.com/api/v1/series");
		assertThat(createConfigAdapter(properties).uri()).isEqualTo("https://app.example.com/api/v1/series");
	}

}
