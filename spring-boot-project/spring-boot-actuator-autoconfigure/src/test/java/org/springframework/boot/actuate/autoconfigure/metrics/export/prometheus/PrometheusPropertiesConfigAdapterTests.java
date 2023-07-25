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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import java.time.Duration;

import io.micrometer.prometheus.HistogramFlavor;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrometheusPropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class PrometheusPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<PrometheusProperties, PrometheusPropertiesConfigAdapter> {

	PrometheusPropertiesConfigAdapterTests() {
		super(PrometheusPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesDescriptionsIsSetAdapterDescriptionsReturnsIt() {
		PrometheusProperties properties = new PrometheusProperties();
		properties.setDescriptions(false);
		assertThat(new PrometheusPropertiesConfigAdapter(properties).descriptions()).isFalse();
	}

	@Test
	void whenPropertiesHistogramFlavorIsSetAdapterHistogramFlavorReturnsIt() {
		PrometheusProperties properties = new PrometheusProperties();
		properties.setHistogramFlavor(HistogramFlavor.VictoriaMetrics);
		assertThat(new PrometheusPropertiesConfigAdapter(properties).histogramFlavor())
			.isEqualTo(HistogramFlavor.VictoriaMetrics);
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		PrometheusProperties properties = new PrometheusProperties();
		properties.setStep(Duration.ofSeconds(30));
		assertThat(new PrometheusPropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofSeconds(30));
	}

}
