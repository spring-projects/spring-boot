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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapterTests;

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
