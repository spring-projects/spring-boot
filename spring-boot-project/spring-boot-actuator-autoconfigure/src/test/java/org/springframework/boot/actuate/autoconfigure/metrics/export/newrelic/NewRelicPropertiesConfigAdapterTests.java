/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.newrelic.ClientProviderType;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapterTests;

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
