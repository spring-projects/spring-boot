/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.appoptics;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppOpticsPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 */
class AppOpticsPropertiesConfigAdapterTests
		extends StepRegistryPropertiesConfigAdapterTests<AppOpticsProperties, AppOpticsPropertiesConfigAdapter> {

	@Override
	protected AppOpticsProperties createProperties() {
		return new AppOpticsProperties();
	}

	@Override
	protected AppOpticsPropertiesConfigAdapter createConfigAdapter(AppOpticsProperties properties) {
		return new AppOpticsPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesUrisIsSetAdapterUriReturnsIt() {
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

}
