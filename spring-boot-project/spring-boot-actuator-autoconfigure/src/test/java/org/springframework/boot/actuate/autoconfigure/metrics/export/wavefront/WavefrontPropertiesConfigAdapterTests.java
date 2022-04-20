/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PushRegistryPropertiesConfigAdapterTests;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties.Metrics.Export;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WavefrontPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class WavefrontPropertiesConfigAdapterTests extends
		PushRegistryPropertiesConfigAdapterTests<WavefrontProperties.Metrics.Export, WavefrontPropertiesConfigAdapter> {

	@Override
	protected WavefrontProperties.Metrics.Export createProperties() {
		return new WavefrontProperties.Metrics.Export();
	}

	@Override
	protected WavefrontPropertiesConfigAdapter createConfigAdapter(WavefrontProperties.Metrics.Export export) {
		WavefrontProperties properties = new WavefrontProperties();
		properties.getMetrics().setExport(export);
		return new WavefrontPropertiesConfigAdapter(properties);
	}

	@Test
	void whenPropertiesGlobalPrefixIsSetAdapterGlobalPrefixReturnsIt() {
		Export properties = createProperties();
		properties.setGlobalPrefix("test");
		assertThat(createConfigAdapter(properties).globalPrefix()).isEqualTo("test");
	}

	@Override
	protected void whenPropertiesBatchSizeIsSetAdapterBatchSizeReturnsIt() {
		WavefrontProperties properties = new WavefrontProperties();
		properties.getSender().setBatchSize(10042);
		assertThat(createConfigAdapter(properties.getMetrics().getExport()).batchSize()).isEqualTo(10042);
	}

}
