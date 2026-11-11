/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.newrelic;

import io.micrometer.newrelic.ClientProviderType;
import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NewRelicPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class NewRelicPropertiesConfigAdapterTests
		extends StepRegistryPropertiesConfigAdapterTests<NewRelicProperties, NewRelicPropertiesConfigAdapter> {

	NewRelicPropertiesConfigAdapterTests() {
		super(NewRelicPropertiesConfigAdapter.class);
	}

	@Override
	protected NewRelicProperties createProperties() {
		return new NewRelicProperties();
	}

	@Override
	protected NewRelicPropertiesConfigAdapter createConfigAdapter(NewRelicProperties properties) {
		return new NewRelicPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesMeterNameEventTypeEnabledIsSetAdapterMeterNameEventTypeEnabledReturnsIt() {
		NewRelicProperties properties = createProperties();
		properties.setMeterNameEventTypeEnabled(true);
		assertThat(createConfigAdapter(properties).meterNameEventTypeEnabled()).isEqualTo(true);
	}

	@Test
	void whenPropertiesEventTypeIsSetAdapterEventTypeReturnsIt() {
		NewRelicProperties properties = createProperties();
		properties.setEventType("foo");
		assertThat(createConfigAdapter(properties).eventType()).isEqualTo("foo");
	}

	@Test
	void whenPropertiesClientProviderTypeIsSetAdapterClientProviderTypeReturnsIt() {
		NewRelicProperties properties = createProperties();
		properties.setClientProviderType(ClientProviderType.INSIGHTS_AGENT);
		assertThat(createConfigAdapter(properties).clientProviderType()).isEqualTo(ClientProviderType.INSIGHTS_AGENT);
	}

	@Test
	void whenPropertiesApiKeyIsSetAdapterApiKeyReturnsIt() {
		NewRelicProperties properties = createProperties();
		properties.setApiKey("my-key");
		assertThat(createConfigAdapter(properties).apiKey()).isEqualTo("my-key");
	}

	@Test
	void whenPropertiesAccountIdIsSetAdapterAccountIdReturnsIt() {
		NewRelicProperties properties = createProperties();
		properties.setAccountId("A38");
		assertThat(createConfigAdapter(properties).accountId()).isEqualTo("A38");
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		NewRelicProperties properties = createProperties();
		properties.setUri("https://example.newrelic.com");
		assertThat(createConfigAdapter(properties).uri()).isEqualTo("https://example.newrelic.com");
	}

}
