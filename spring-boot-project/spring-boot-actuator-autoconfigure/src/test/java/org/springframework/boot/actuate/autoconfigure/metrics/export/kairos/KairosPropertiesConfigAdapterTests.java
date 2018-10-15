/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.kairos;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KairosPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 */
public class KairosPropertiesConfigAdapterTests extends
		StepRegistryPropertiesConfigAdapterTests<KairosProperties, KairosPropertiesConfigAdapter> {

	@Override
	protected KairosProperties createProperties() {
		return new KairosProperties();
	}

	@Override
	protected KairosPropertiesConfigAdapter createConfigAdapter(
			KairosProperties properties) {
		return new KairosPropertiesConfigAdapter(properties);
	}

	@Test
	public void whenPropertiesUrisIsSetAdapterUriReturnsIt() {
		KairosProperties properties = createProperties();
		properties.setUri("https://kairos.example.com:8080/api/v1/datapoints");
		assertThat(createConfigAdapter(properties).uri())
				.isEqualTo("https://kairos.example.com:8080/api/v1/datapoints");
	}

	@Test
	public void whenPropertiesUserNameIsSetAdapterUserNameReturnsIt() {
		KairosProperties properties = createProperties();
		properties.setUserName("alice");
		assertThat(createConfigAdapter(properties).userName()).isEqualTo("alice");
	}

	@Test
	public void whenPropertiesPasswordIsSetAdapterPasswordReturnsIt() {
		KairosProperties properties = createProperties();
		properties.setPassword("secret");
		assertThat(createConfigAdapter(properties).password()).isEqualTo("secret");
	}

}
