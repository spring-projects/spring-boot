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

package org.springframework.boot.actuate.autoconfigure.metrics.export.otlp;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.registry.otlp.AggregationTemporality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link OtlpPropertiesConfigAdapter}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class OtlpPropertiesConfigAdapterTests {

	private OtlpProperties properties;

	private OpenTelemetryProperties openTelemetryProperties;

	@BeforeEach
	void setUp() {
		this.properties = new OtlpProperties();
		this.openTelemetryProperties = new OpenTelemetryProperties();
	}

	@Test
	void whenPropertiesUrlIsSetAdapterUrlReturnsIt() {
		this.properties.setUrl("http://another-url:4318/v1/metrics");
		assertThat(createAdapter().url()).isEqualTo("http://another-url:4318/v1/metrics");
	}

	@Test
	void whenPropertiesAggregationTemporalityIsNotSetAdapterAggregationTemporalityReturnsCumulative() {
		assertThat(createAdapter().aggregationTemporality()).isSameAs(AggregationTemporality.CUMULATIVE);
	}

	@Test
	void whenPropertiesAggregationTemporalityIsSetAdapterAggregationTemporalityReturnsIt() {
		this.properties.setAggregationTemporality(AggregationTemporality.DELTA);
		assertThat(createAdapter().aggregationTemporality()).isSameAs(AggregationTemporality.DELTA);
	}

	@Test
	@SuppressWarnings("removal")
	void whenPropertiesResourceAttributesIsSetAdapterResourceAttributesReturnsIt() {
		this.properties.setResourceAttributes(Map.of("service.name", "boot-service"));
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.name", "boot-service");
	}

	@Test
	void whenPropertiesHeadersIsSetAdapterHeadersReturnsIt() {
		this.properties.setHeaders(Map.of("header", "value"));
		assertThat(createAdapter().headers()).containsEntry("header", "value");
	}

	@Test
	void whenPropertiesBaseTimeUnitIsNotSetAdapterBaseTimeUnitReturnsMillis() {
		assertThat(createAdapter().baseTimeUnit()).isSameAs(TimeUnit.MILLISECONDS);
	}

	@Test
	void whenPropertiesBaseTimeUnitIsSetAdapterBaseTimeUnitReturnsIt() {
		this.properties.setBaseTimeUnit(TimeUnit.SECONDS);
		assertThat(createAdapter().baseTimeUnit()).isSameAs(TimeUnit.SECONDS);
	}

	@Test
	@SuppressWarnings("removal")
	void openTelemetryPropertiesShouldOverrideOtlpPropertiesIfNotEmpty() {
		this.properties.setResourceAttributes(Map.of("a", "alpha"));
		this.openTelemetryProperties.setResourceAttributes(Map.of("b", "beta"));
		assertThat(createAdapter().resourceAttributes()).containsExactly(entry("b", "beta"));
	}

	@Test
	@SuppressWarnings("removal")
	void openTelemetryPropertiesShouldNotOverrideOtlpPropertiesIfEmpty() {
		this.properties.setResourceAttributes(Map.of("a", "alpha"));
		this.openTelemetryProperties.setResourceAttributes(Collections.emptyMap());
		assertThat(createAdapter().resourceAttributes()).containsExactly(entry("a", "alpha"));
	}

	private OtlpPropertiesConfigAdapter createAdapter() {
		return new OtlpPropertiesConfigAdapter(this.properties, this.openTelemetryProperties);
	}

}
