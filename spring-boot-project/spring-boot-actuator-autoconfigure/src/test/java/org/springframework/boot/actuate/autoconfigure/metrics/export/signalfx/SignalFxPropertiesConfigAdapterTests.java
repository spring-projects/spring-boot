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

package org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapterTests;
import org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx.SignalFxProperties.HistogramType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SignalFxPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class SignalFxPropertiesConfigAdapterTests
		extends StepRegistryPropertiesConfigAdapterTests<SignalFxProperties, SignalFxPropertiesConfigAdapter> {

	protected SignalFxPropertiesConfigAdapterTests() {
		super(SignalFxPropertiesConfigAdapter.class);
	}

	@Override
	protected SignalFxProperties createProperties() {
		SignalFxProperties signalFxProperties = new SignalFxProperties();
		signalFxProperties.setAccessToken("ABC");
		return signalFxProperties;
	}

	@Override
	protected SignalFxPropertiesConfigAdapter createConfigAdapter(SignalFxProperties properties) {
		return new SignalFxPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesAccessTokenIsSetAdapterAccessTokenReturnsIt() {
		SignalFxProperties properties = createProperties();
		assertThat(createConfigAdapter(properties).accessToken()).isEqualTo("ABC");
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		SignalFxProperties properties = createProperties();
		properties.setUri("https://example.signalfx.com");
		assertThat(createConfigAdapter(properties).uri()).isEqualTo("https://example.signalfx.com");
	}

	@Test
	void whenPropertiesSourceIsSetAdapterSourceReturnsIt() {
		SignalFxProperties properties = createProperties();
		properties.setSource("DESKTOP-GA5");
		assertThat(createConfigAdapter(properties).source()).isEqualTo("DESKTOP-GA5");
	}

	@Test
	void whenPropertiesPublishHistogramTypeIsCumulativePublishCumulativeHistogramReturnsIt() {
		SignalFxProperties properties = createProperties();
		properties.setPublishedHistogramType(HistogramType.CUMULATIVE);
		assertThat(createConfigAdapter(properties).publishCumulativeHistogram()).isTrue();
		assertThat(createConfigAdapter(properties).publishDeltaHistogram()).isFalse();
	}

	@Test
	void whenPropertiesPublishHistogramTypeIsDeltaPublishDeltaHistogramReturnsIt() {
		SignalFxProperties properties = createProperties();
		properties.setPublishedHistogramType(HistogramType.DELTA);
		assertThat(createConfigAdapter(properties).publishDeltaHistogram()).isTrue();
		assertThat(createConfigAdapter(properties).publishCumulativeHistogram()).isFalse();
	}

}
