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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AtlasPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class AtlasPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<AtlasProperties, AtlasPropertiesConfigAdapter> {

	AtlasPropertiesConfigAdapterTests() {
		super(AtlasPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setStep(Duration.ofMinutes(15));
		assertThat(new AtlasPropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void whenPropertiesEnabledIsSetAdapterEnabledReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setEnabled(false);
		assertThat(new AtlasPropertiesConfigAdapter(properties).enabled()).isFalse();
	}

	@Test
	void whenPropertiesConnectTimeoutIsSetAdapterConnectTimeoutReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setConnectTimeout(Duration.ofSeconds(12));
		assertThat(new AtlasPropertiesConfigAdapter(properties).connectTimeout()).isEqualTo(Duration.ofSeconds(12));
	}

	@Test
	void whenPropertiesReadTimeoutIsSetAdapterReadTimeoutReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setReadTimeout(Duration.ofSeconds(42));
		assertThat(new AtlasPropertiesConfigAdapter(properties).readTimeout()).isEqualTo(Duration.ofSeconds(42));
	}

	@Test
	void whenPropertiesNumThreadsIsSetAdapterNumThreadsReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setNumThreads(8);
		assertThat(new AtlasPropertiesConfigAdapter(properties).numThreads()).isEqualTo(8);
	}

	@Test
	void whenPropertiesBatchSizeIsSetAdapterBatchSizeReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setBatchSize(10042);
		assertThat(new AtlasPropertiesConfigAdapter(properties).batchSize()).isEqualTo(10042);
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setUri("https://atlas.example.com");
		assertThat(new AtlasPropertiesConfigAdapter(properties).uri()).isEqualTo("https://atlas.example.com");
	}

	@Test
	void whenPropertiesLwcEnabledIsSetAdapterLwcEnabledReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setLwcEnabled(true);
		assertThat(new AtlasPropertiesConfigAdapter(properties).lwcEnabled()).isTrue();
	}

	@Test
	void whenPropertiesConfigRefreshFrequencyIsSetAdapterConfigRefreshFrequencyReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setConfigRefreshFrequency(Duration.ofMinutes(5));
		assertThat(new AtlasPropertiesConfigAdapter(properties).configRefreshFrequency())
			.isEqualTo(Duration.ofMinutes(5));
	}

	@Test
	void whenPropertiesConfigTimeToLiveIsSetAdapterConfigTTLReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setConfigTimeToLive(Duration.ofMinutes(6));
		assertThat(new AtlasPropertiesConfigAdapter(properties).configTTL()).isEqualTo(Duration.ofMinutes(6));
	}

	@Test
	void whenPropertiesConfigUriIsSetAdapterConfigUriReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setConfigUri("https://atlas.example.com/config");
		assertThat(new AtlasPropertiesConfigAdapter(properties).configUri())
			.isEqualTo("https://atlas.example.com/config");
	}

	@Test
	void whenPropertiesEvalUriIsSetAdapterEvalUriReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setEvalUri("https://atlas.example.com/evaluate");
		assertThat(new AtlasPropertiesConfigAdapter(properties).evalUri())
			.isEqualTo("https://atlas.example.com/evaluate");
	}

	@Test
	void whenPropertiesLwcStepIsSetAdapterLwcStepReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setLwcStep(Duration.ofSeconds(30));
		assertThat(new AtlasPropertiesConfigAdapter(properties).lwcStep()).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void whenPropertiesLwcIgnorePublishStepIsSetAdapterLwcIgnorePublishStepReturnsIt() {
		AtlasProperties properties = new AtlasProperties();
		properties.setLwcIgnorePublishStep(false);
		assertThat(new AtlasPropertiesConfigAdapter(properties).lwcIgnorePublishStep()).isFalse();
	}

	@Test
	@Override
	protected void adapterOverridesAllConfigMethods() {
		adapterOverridesAllConfigMethodsExcept("autoStart", "commonTags", "debugRegistry", "publisher", "rollupPolicy",
				"validTagCharacters");
	}

}
